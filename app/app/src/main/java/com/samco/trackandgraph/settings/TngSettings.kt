package com.samco.trackandgraph.settings

import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration

interface TngSettings {
    val firstDayOfWeek: DayOfWeek
    val startTimeOfDay: Duration
}

val mockSettings = object : TngSettings {
    override val firstDayOfWeek = DayOfWeek.MONDAY
    override val startTimeOfDay = Duration.ofSeconds(0)
}
