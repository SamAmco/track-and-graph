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
    data class WeekDayParams(...)    // Specific days of the week
    data class PeriodicParams(...)   // Every N hours/days/weeks
    data class MonthDayParams(...)   // Specific day of month
    data class TimeSinceLastParams(...) // After duration since last entry
}
```

## Delete Behavior

Deletion uses `ReminderDeleteRequest(reminderId, groupId?)` and follows the symlink pattern:

- **`groupId` provided AND reminder in multiple locations** → remove only that group's symlink (GroupItem), reminder survives
- **Otherwise** → delete all GroupItems and the reminder itself

This means deleting from the Reminders screen (where `groupId` is always null) always deletes the reminder everywhere. A future dialog will warn the user if the reminder also exists in groups.

```kotlin
data class ReminderDeleteRequest(
    val reminderId: Long,
    val groupId: Long? = null,
)
```

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

## Key Files

- `ReminderHelperImpl.kt` - CRUD operations
- `ReminderDao.kt` - Database interface
- `ReminderSerializer.kt` - JSON serialization for params
- `PlatformScheduler.kt` - Platform abstraction interface
- `androidplatform/AndroidPlatformScheduler.kt` - Android implementation
