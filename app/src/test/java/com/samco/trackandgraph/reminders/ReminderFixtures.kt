package com.samco.trackandgraph.reminders

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import org.threeten.bp.LocalTime

val reminderFixture = Reminder(
    id = 1L,
    displayIndex = 0,
    reminderName = "Test Reminder",
    time = LocalTime.of(0, 0),
    checkedDays = CheckedDays.none()
)
