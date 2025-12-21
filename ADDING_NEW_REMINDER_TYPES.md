# Adding New Reminder Types

This document outlines the steps required to add a new reminder type to the Track & Graph application.

## Data Model Requirements

### 1. Add New ReminderParams Subclass

**File**: `data/src/main/java/com/samco/trackandgraph/data/database/dto/Reminder.kt`

You must add a new data class that extends `ReminderParams` for each new reminder type. The data class should:

- Be annotated with `@Serializable`
- Have a unique `@SerialName` annotation for JSON serialization
- Extend `ReminderParams()`
- Include all necessary parameters for the reminder type

### Example: WeekDay Implementation

```kotlin
@Serializable
@SerialName("weekday")
data class WeekDayParams(
    @Serializable(with = LocalTimeSerializer::class)
    val time: LocalTime,
    val checkedDays: CheckedDays
) : ReminderParams()
```

### 2. Create Required Serializers

If your new reminder type uses data types that don't have existing serializers, create them in:
`data/src/main/java/com/samco/trackandgraph/data/serialization/`

Follow the pattern of existing serializers like `LocalTimeSerializer.kt`.

## Known Implementation Steps

Based on current understanding, you'll need to update:

### Data Layer
- [ ] Add new `ReminderParams` subclass in `Reminder.kt`
- [ ] Add any required serializers for new data types

### Scheduling Layer
- [ ] Add new case to `when` statement in `ReminderScheduler.scheduleNext()`
- [ ] Implement scheduling logic method for the new reminder type
- [ ] Add comprehensive tests for the new scheduling logic

### View Data Layer
- [ ] Add new `ReminderViewData` subclass in `ReminderViewData.kt`
- [ ] Update `fromReminder()` method to handle the new reminder type

### UI Layer  
- [ ] Update `ReminderTypeSelectionScreen` to include the new type
- [ ] Create new configuration ViewModel for the reminder type
- [ ] Create new configuration screen for the reminder type
- [ ] Add new navigation key to `AddReminderDialogContent`
- [ ] Update navigation logic to handle the new reminder type

## Key Requirements

- **Serialization**: All new data types must have proper kotlinx.serialization support
- **Time Handling**: Use `org.threeten.bp` classes for time-related fields
- **Backwards Compatibility**: Existing reminders must continue to work

*Note: This document will be updated as we implement new reminder types and discover additional required steps.*
