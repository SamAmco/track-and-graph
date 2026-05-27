---
title: Reminders — lifecycle, scheduling, and display ordering
description: Reminder data and lifecycle, including groupless placement, serialized enable/disable state, scheduling behavior, delete/duplicate operations, and RemindersScreenViewModel display ordering.
topics:
  - Groupless reminders: group_id = null, appear only in Reminders screen (not group views)
  - ReminderParams types and serialized enabled state; missing enabled values default to true
  - Disabled reminders remain stored and visible but cancel/skip notification scheduling
  - Delete: uses unified ComponentDeleteRequest (see helper-classes.md); groupless reminders with single placement always delete everywhere
  - Duplicate: takes groupItemId (consistent with all other ops); inserts AFTER original; shifts only items below; app module alignment pending (Stage C)
  - Scheduling: PlatformScheduler interface isolates Android AlarmManager (KMP pattern)
  - PITFALL: RemindersScreenViewModel dbDisplayIndices MUST react to DataUpdateType.Reminder or new reminders fall to bottom
keywords: [reminder, groupless, null, ReminderParams, enabled, disabled, serialization, backward-compatibility, PlatformScheduler, scheduling, cancel, delete, duplicate, display-index, RemindersScreenViewModel, DataUpdateType, KMP]
---

# Reminders

Reminders have special handling compared to other components.

## Unique Behavior

### Groupless Reminders

Unlike trackers, functions, and graphs, reminders can exist **outside of any group**:

```kotlin
GroupItem(
    groupId = null,  // No parent group
    childId = reminderId,
    type = GroupItemType.REMINDER,
    displayIndex = 0
)
```

These "groupless" reminders appear only in the dedicated **Reminders screen**, not in any group view.

### Why This Exists

Reminders serve a different purpose than other components:
- They're about notifications, not data visualization
- Users may want reminders for trackers across multiple groups
- A central reminders view makes sense for managing notification schedules

## Reminder Structure

### Entity (Database)

```kotlin
@Entity(tableName = "reminders_table")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmName: String,
    val featureId: Long?,           // Optional - can prompt for a specific tracker
    val encodedReminderParams: String  // JSON-serialized schedule
)
```

### DTO (API Layer)

```kotlin
data class Reminder(
    val id: Long,
    val reminderName: String,
    val featureId: Long?,
    val params: ReminderParams       // Deserialized schedule
)
```

Note: `groupId` and `displayIndex` are NOT on the DTO. They're managed via GroupItem.

## Reminder Types (ReminderParams)

```kotlin
sealed class ReminderParams {
    abstract val enabled: Boolean

    data class WeekDayParams(...)    // Specific days of the week
    data class PeriodicParams(...)   // Every N hours/days/weeks
    data class MonthDayParams(...)   // Specific day of month
    data class TimeSinceLastParams(...) // After duration since last entry
}
```

## Enable and Disable Behavior

Enablement belongs to each serialized `ReminderParams` subtype rather than the reminder database entity. Every subtype declares `enabled: Boolean = true`; the default is required for backward-compatible deserialization of reminders saved before this field existed.

A disabled reminder is retained as normal data and remains visible and editable. It differs only in presentation and scheduling:

- `ReminderScheduler` returns no next instant without delegating to a type-specific scheduler.
- The notification reconciliation path cancels any existing platform alarm for the reminder.
- The reminders screen presents it as disabled instead of showing a next scheduled time.

Configuration screens thread the same state through each reminder-type ViewModel. New/reset configurations default to enabled, while editing restores the serialized value. The enable checkbox is colocated with the reminder-name field; the animated disabled label is presentation-only and must not become the source of truth.

## Delete Behavior

Deletion uses `ComponentDeleteRequest(groupItemId, deleteEverywhere)` — the same unified DTO used by all component types. See [helper-classes.md](helper-classes.md#delete-pattern-symlink-logic) for the full pattern.

The helper derives the `reminderId` from the GroupItem lookup. Deleting from the Reminders screen (where `groupId` is null on the GroupItem) with `deleteEverywhere = false` will still delete the reminder if it has no other placements.

## Operations

### Create Reminder in Group

```kotlin
// Standard flow - shift and insert GroupItem
groupItemDao.shiftDisplayIndexesDown(groupId)
val reminderId = reminderDao.insertReminder(entity)
groupItemDao.insertGroupItem(GroupItem(groupId = groupId, ...))
```

### Create Groupless Reminder

```kotlin
// Shift items with null group
groupItemDao.shiftDisplayIndexesDownForNullGroup()
val reminderId = reminderDao.insertReminder(entity)
groupItemDao.insertGroupItem(GroupItem(groupId = null, ...))
```

### Duplicate Reminder

`duplicateReminder(groupItemId: Long)` takes a `groupItemId` (placement identity), consistent with all other duplicate operations. The implementation looks up the GroupItem first to derive the `reminderId` and placement details.

Inserts the copy immediately **after** the original (not at the top). Only items below the original are shifted:

```kotlin
val originalIndex = existingGroupItem.displayIndex
val insertAtIndex = originalIndex + 1
// Shift only items after the original
if (groupId != null) groupItemDao.shiftDisplayIndexesDownAfter(groupId, originalIndex)
else groupItemDao.shiftDisplayIndexesDownAfterForNullGroup(originalIndex)
groupItemDao.insertGroupItem(GroupItem(displayIndex = insertAtIndex, ...))
```

**App module alignment pending (parameter only)**: `RemindersScreenViewModel` still calls `duplicateReminder` with `reminder.id` (a component ID) instead of a `groupItemId`. The return type is now `CreatedComponent` (aligned). Fixing the parameter requires threading `groupItemId` through `ReminderViewData`, planned for Stage C.

### Query Groupless Reminders

```kotlin
groupItemDao.getGroupItemsWithNoGroup()
    .filter { it.type == GroupItemType.REMINDER }
```

## Scheduling Architecture

The reminder scheduler deliberately isolates Android platform code behind an interface — this is an intentional KMP-compatibility pattern (see [architecture.md](architecture.md)):

- **`PlatformScheduler`** — pure Kotlin interface: `set(triggerAtMillis, params)`, `cancel(params)`, `getNextScheduledMillis(params)`
- **`AndroidPlatformScheduler`** — Android implementation using `AlarmManager`; lives in `androidplatform/` subpackage
- **`ReminderScheduler` / `*ReminderScheduler`** — pure Kotlin scheduling logic, depend only on `PlatformScheduler`
- **`FakePlatformScheduler`** — used in tests instead of mocking

## RemindersScreenViewModel — Display Ordering

The screen combines two independent flows:

- `allReminders` — reacts to `DataUpdateType.Reminder`; holds the list of `ReminderViewData`
- `dbDisplayIndices` — reacts to `DataUpdateType.ReminderScreenDisplayOrder` **and** `DataUpdateType.Reminder`; holds a `Map<reminderId, displayIndex>`

Both flows **must** react to `DataUpdateType.Reminder`. If `dbDisplayIndices` only listens for `ReminderScreenDisplayOrder`, a newly created reminder won't appear in the index map and will fall to the bottom of the list (sorted with `Int.MAX_VALUE` as the fallback).

## Key Files

- `ReminderHelperImpl.kt` - CRUD operations
- `ReminderDao.kt` - Database interface
- `ReminderSerializer.kt` - JSON serialization for params
- `PlatformScheduler.kt` - Platform abstraction interface
- `androidplatform/AndroidPlatformScheduler.kt` - Android implementation
