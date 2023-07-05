package com.samco.trackandgraph.settings

import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration

interface TngSettings {
    val firstDayOfWeek: DayOfWeek
    val startTimeOfDay: Duration
}