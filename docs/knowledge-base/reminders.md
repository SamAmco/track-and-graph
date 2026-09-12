---
title: Reminders — lifecycle, scheduling, and display ordering
description: Reminder data and lifecycle, including global and grouped placement, path display, serialized enable/disable state, scheduling behavior, delete/duplicate operations, and reminder view-data construction.
topics:
  - Every reminder has one null-group Reminders-screen placement and may have one additional group placement
  - Reminders can belong to at most one group and cannot be symlinked
  - ReminderParams types, serialized enabled state, and optional time-of-day state for Time Since Last
  - Disabled reminders remain stored and visible but cancel/skip notification scheduling
  - Delete: always deletes the reminder and every placement, regardless of deleteEverywhere
  - Duplicate: reproduces both placements and inserts after the original independently in each list
  - Move from global: creates or relocates the optional grouped placement without moving the global row
  - Reminders-screen cards show the first resolved component path when grouped
  - Scheduling: PlatformScheduler interface isolates Android AlarmManager (KMP pattern)
  - PITFALL: RemindersScreenViewModel dbDisplayIndices MUST react to DataUpdateType.Reminder or new reminders fall to bottom
keywords: [reminder, groupless, null, component-path, ReminderParams, enabled, disabled, serialization, backward-compatibility, PlatformScheduler, scheduling, cancel, delete, duplicate, move, display-index, RemindersScreenViewModel, DataUpdateType, KMP]
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

This single non-null placement identifies one owning group, but it does not guarantee that the
reminder has only one path through the overall hierarchy. The owning group itself may appear below
multiple parents, so path-based navigation can discover multiple descents to the same reminder.
See [group-hierarchy.md](group-hierarchy.md#groupgraph-and-path-resolution).

Notification taps carry the stable reminder ID and resolve its current grouped placement only when
the app opens; they do not persist the movable group-item ID. See
[deep-link-navigation.md](deep-link-navigation.md#reminder-notification-entry-point).

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

`TimeSinceLastParams.timeOfDay` is nullable and applies only when the first interval is measured in
days, weeks, months, or years. A missing or null value preserves the original behavior of adding the
interval while retaining the last data point's local time. When set, the scheduler adds the calendar
interval and then replaces the resulting local time with the configured time of day. This field is
stored inside the existing params JSON, so it requires no Room migration. Production JSON parsing
ignores unknown keys, allowing older production clients to read reminders written by newer clients;
debug parsing intentionally rejects unknown keys. New clients default a missing field to null.

## Enable and Disable Behavior

Enablement belongs to each serialized `ReminderParams` subtype rather than the reminder database entity. Every subtype declares `enabled: Boolean = true`; the default is required for backward-compatible deserialization of reminders saved before this field existed.

A disabled reminder is retained as normal data and remains visible and editable. It differs only in presentation and scheduling:

- `ReminderScheduler` returns no next instant without delegating to a type-specific scheduler.
- The notification reconciliation path cancels any existing platform alarm for the reminder.
- The reminders screen presents it as disabled instead of showing a next scheduled time.

Configuration screens thread the same state through each reminder-type ViewModel. New/reset configurations default to enabled, while editing restores the serialized value. The enable checkbox is colocated with the reminder-name field; the animated disabled label is presentation-only and must not become the source of truth.

The create-flow type selector initially omits Time Since Last while `hasAnyFeatures()` runs in the
background, then adds the tile when the result becomes true. Functions and trackers are both valid
features for this reminder type, so the eligibility check is intentionally broader than
`hasAtLeastOneTracker()`. Because the selector is a retained Navigation 3 entry, its async boolean
must cross the entry-provider boundary as observable Compose state; see
[dialogs.md](dialogs.md#dialog-navigation).

## Delete Behavior

Deletion accepts `ComponentDeleteRequest` for API consistency, but reminders do not use its
symlink-oriented `deleteEverywhere` distinction. The helper derives the reminder ID from the
selected placement, then always deletes the reminder entity and all of its GroupItems. This is true
whether deletion starts from the Reminders screen, its group, or recursive group deletion.
When recursive group deletion reports deleted reminder IDs, `DataInteractorImpl.deleteGroup` also
emits a reminder update so notification reconciliation and the global Reminders screen observe the
removal. It does not emit that event when the deleted group contained no reminders.

The group screen cancels scheduled notifications for the exact `deletedReminderIds` returned after
recursive deletion commits. `ReminderInteractor.cancelReminderNotifications(reminderId)` derives
the AlarmManager and WorkManager identity from the ID without reading the reminder row, so it is
safe after deletion. Do not pre-query all nested reminders: groups form a DAG, and a nested group
may survive through a parent outside the deletion set. The helper's returned IDs reflect its actual
DAG-aware deletion decision.

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

### Move Reminder to Group

The global Reminders-screen card offers **Move to** even though its null-group placement is never
moved. `moveReminderToGroup(reminderId, toGroupId)` instead creates the reminder's optional grouped
placement when it has none, or relocates the existing grouped placement. This lets reminders that
predate grouped reminders be assigned to a group while preserving their independent global order.
The operation does not require the unrelated null placement to exist. If malformed data contains
multiple grouped placements, it replaces them with one destination placement, restoring the
single-group invariant rather than throwing because the invariant was already broken.
Group-screen cards continue to move their concrete non-null placement through the generic
`moveComponent` operation.

### Query Reminders-screen Placements

```kotlin
groupItemDao.getGroupItemsWithNoGroup()
    .filter { it.type == GroupItemType.REMINDER }
```

## Scheduling Architecture

The reminder scheduler deliberately isolates Android platform code behind an interface — this is an intentional KMP-compatibility pattern (see [architecture.md](architecture.md)):

- **`PlatformScheduler`** — pure Kotlin interface: scheduling/query operations take notification
  params, while cancellation takes only `ReminderNotificationIdentity` (`alarmId` plus
  `reminderId`); reminder display content is not part of cancellation identity
- **`AndroidPlatformScheduler`** — Android implementation using `AlarmManager`; lives in `androidplatform/` subpackage
- **`ReminderScheduler` / `*ReminderScheduler`** — pure Kotlin scheduling logic, depend only on `PlatformScheduler`
- **`FakePlatformScheduler`** — used in tests instead of mocking

## UI projection and display ordering

`RemindersScreenViewModel` queries all reminder entities and pairs each with its null-group
placement from `getDisplayIndicesForRemindersScreen()`. The placement list is authoritative for
the screen: it supplies both the `groupItemId` used by delete/duplicate and the global display
index. If the two separate reads observe inconsistent or malformed data, unmatched reminder
entities and dangling placements are omitted instead of crashing the UI; a subsequent data update
reloads the projection. Normal create and duplicate writes must still maintain the one-null-row
invariant. Do not add a database migration solely to repair data produced by an unreleased
development build.

A grouped reminder's non-null placement is consumed separately by `GroupViewModel`, so dragging
in either screen changes only that screen's order.

The Reminders screen also resolves the current root `GroupGraph` and puts the first full component
path on `ReminderViewData` for grouped reminders (for example,
`/Health/Medication/Take tablet`). It uses the same `ComponentPathProvider` reminder lookup as
other path consumers; there is no separate reminder-only definition of a group path. A group can
have multiple parent placements, so this is deliberately a representative path rather than a
claim of uniqueness. The shared reminder card renders this optional value under its title on one
line with leading ellipsis; group and search callers leave it null, so the path appears only in the
global list. Group hierarchy updates must therefore refresh the Reminders-screen projection as
well as reminder updates.

`GroupViewModel` combines `getDisplayIndicesForGroup(groupId)` with reminder view data and emits
`GroupChild.ChildReminder`, just like its other component branches. It obtains the reminder DTOs
through `getRemindersForGroupSync(groupId)`, which reads the typed reminder placements and fetches
only those reminder entities; do not load every global reminder and filter them in the ViewModel.
Reminder cards span two grid columns in group and search results, participate in the group's shared
drag order, and support edit, duplicate, delete, and moving their grouped placement to another
group. The Reminders-screen card also offers moving, but uses the reminder-specific operation that
creates or relocates the optional grouped placement without touching the required null placement.
Reminder cards never offer symlink actions.

`ReminderViewDataFactory` owns the shared conversion from the stored DTO to `ReminderViewData`,
including next-scheduled calculation and the time-since-last data sample. Both screen ViewModels use
it so scheduling presentation stays identical.

Grouped reminders are included as `GroupGraphItem.ReminderNode` values, so group search indexes
their names and resolves every descent to their grouped placement. Tapping a reminder navigates to
it in the group hierarchy just like other search results. A reminder cannot itself be symlinked,
but symlinked ancestor groups can still produce multiple paths and the normal disambiguation dialog
handles that case. Search keeps the node structural until it has ranked a query, then progressively
builds and caches `ReminderViewData` through the same factory. Both the loading placeholder and the
loaded reminder card can navigate because their paths are available before enrichment completes.

`DataUpdateType.Reminder(reminderId)` identifies the affected entity. All reminder mutation paths
must emit the real ID, including one event per reminder removed by recursive group deletion. This
lets targeted consumers such as search refresh only the matching card; broad list consumers may
still reload their full projection when any reminder event arrives.

## Key Files

- `ReminderHelperImpl.kt` - CRUD operations
- `ReminderDao.kt` - Database interface
- `ReminderSerializer.kt` - JSON serialization for params
- `PlatformScheduler.kt` - Platform abstraction interface
- `androidplatform/AndroidPlatformScheduler.kt` - Android implementation
