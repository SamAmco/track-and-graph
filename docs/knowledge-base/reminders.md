---
title: Reminders — lifecycle, scheduling, and display ordering
description: Reminder data and lifecycle, including global and grouped placement, serialized enable/disable state, scheduling behavior, delete/duplicate operations, and reminder view-data construction.
topics:
  - Every reminder has one null-group Reminders-screen placement and may have one additional group placement
  - Reminders can belong to at most one group and cannot be symlinked
  - ReminderParams types and serialized enabled state; missing enabled values default to true
  - Disabled reminders remain stored and visible but cancel/skip notification scheduling
  - Delete: always deletes the reminder and every placement, regardless of deleteEverywhere
  - Duplicate: reproduces both placements and inserts after the original independently in each list
  - Scheduling: PlatformScheduler interface isolates Android AlarmManager (KMP pattern)
  - PITFALL: RemindersScreenViewModel dbDisplayIndices MUST react to DataUpdateType.Reminder or new reminders fall to bottom
keywords: [reminder, groupless, null, ReminderParams, enabled, disabled, serialization, backward-compatibility, PlatformScheduler, scheduling, cancel, delete, duplicate, display-index, RemindersScreenViewModel, DataUpdateType, KMP]
---

# Reminders

Reminders have special handling compared to other components.

## Unique Behavior

### Global and Grouped Reminders

Unlike trackers, functions, and graphs, reminders can exist **outside of any group**:

```kotlin
GroupItem(
    groupId = null,  // No parent group
    childId = reminderId,
    type = GroupItemType.REMINDER,
    displayIndex = 0
)
```

Every reminder has exactly one null-group `GroupItem`, which represents its position in the
dedicated **Reminders screen**. A grouped reminder has one additional non-null `GroupItem`, which
independently represents its position in that group's component grid. It may not have a second
non-null placement.

Unlike other component types, reminders cannot be symlinked. The Add Symlink picker excludes them,
and `GroupHelperImpl.createSymlink` rejects `GroupChildType.REMINDER` as a data-layer invariant.
The generic GroupItem schema does not enforce the cardinality itself; reminder creation and
duplication maintain it, while the symlink entry point provides the defensive rejection.

No migration is required for reminders created before grouped reminders were introduced: they
already have the required null-group placement and simply remain global-only reminders.

### Why This Exists

Reminders serve a different purpose than other components:
- They're about notifications, not data visualization
- Users may want reminders for trackers throughout the app
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
    val params: ReminderParams,      // Deserialized schedule
    val unique: Boolean
)
```

Note: `groupId` and `displayIndex` are NOT on the DTO. They're managed via GroupItem. Reminder DTOs
always report `unique = true`: the required global and optional group rows are not symlinks.

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

Deletion accepts `ComponentDeleteRequest` for API consistency, but reminders do not use its
symlink-oriented `deleteEverywhere` distinction. The helper derives the reminder ID from the
selected placement, then always deletes the reminder entity and all of its GroupItems. This is true
whether deletion starts from the Reminders screen, its group, or recursive group deletion.
When recursive group deletion reports deleted reminder IDs, `DataInteractorImpl.deleteGroup` also
emits a reminder update so notification reconciliation and the global Reminders screen observe the
removal. It does not emit that event when the deleted group contained no reminders.

## Operations

### Create Reminder in Group

```kotlin
// Insert independent placements at the top of both lists.
groupItemDao.shiftDisplayIndexesDownForNullGroup()
groupItemDao.shiftDisplayIndexesDown(groupId)
val reminderId = reminderDao.insertReminder(entity)
groupItemDao.insertGroupItem(GroupItem(groupId = null, ...))
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

`duplicateReminder(groupItemId: Long)` looks up every placement for that reminder and reproduces
them for the copy. The copy is inserted immediately after the original independently in the global
list and, when present, the group list. The returned `CreatedComponent.groupItemId` corresponds to
the same context as the placement passed by the caller.

```kotlin
for (placement in existingPlacements) {
    shiftItemsAfter(placement.groupId, placement.displayIndex)
    insertPlacement(
        groupId = placement.groupId,
        displayIndex = placement.displayIndex + 1,
    )
}
```

### Query Reminders-screen Placements

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

## UI projection and display ordering

`RemindersScreenViewModel` queries all reminder entities and pairs each with its required null-group
placement from `getDisplayIndicesForRemindersScreen()`. That placement supplies both the
`groupItemId` used by delete/duplicate and the global display index. A grouped reminder's non-null
placement is consumed separately by `GroupViewModel`, so dragging in either screen changes only
that screen's order.

`GroupViewModel` combines `getDisplayIndicesForGroup(groupId)` with reminder view data and emits
`GroupChild.ChildReminder`, just like its other component branches. It obtains the reminder DTOs
through `getRemindersForGroupSync(groupId)`, which reads the typed reminder placements and fetches
only those reminder entities; do not load every global reminder and filter them in the ViewModel.
Reminder cards span two grid columns in group and search results, participate in the group's shared
drag order, and support edit, duplicate, and delete. They do not offer move or symlink actions.

`ReminderViewDataFactory` owns the shared conversion from the stored DTO to `ReminderViewData`,
including next-scheduled calculation and the time-since-last data sample. Both screen ViewModels use
it so scheduling presentation stays identical.

## Key Files

- `ReminderHelperImpl.kt` - CRUD operations
- `ReminderDao.kt` - Database interface
- `ReminderSerializer.kt` - JSON serialization for params
- `PlatformScheduler.kt` - Platform abstraction interface
- `androidplatform/AndroidPlatformScheduler.kt` - Android implementation
