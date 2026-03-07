package com.samco.trackandgraph.reminders

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.threeten.bp.LocalTime

val reminderFixture = Reminder(
    id = 1L,
    reminderName = "Test Reminder",
    featureId = null,
    params = ReminderParams.WeekDayParams(
        time = LocalTime.of(0, 0),
        checkedDays = CheckedDays.none()
    ),
    unique = true,
)
