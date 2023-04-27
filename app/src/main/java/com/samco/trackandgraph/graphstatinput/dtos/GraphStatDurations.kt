package com.samco.trackandgraph.graphstatinput.dtos

import org.threeten.bp.Duration

enum class GraphStatDurations(val duration: Duration?) {
    ALL_DATA(null),
    A_DAY(Duration.ofDays(1)),
    A_WEEK(Duration.ofDays(7)),
    A_MONTH(Duration.ofDays(31)),
    THREE_MONTHS(Duration.ofDays(93)),
    SIX_MONTHS(Duration.ofDays(183)),
    A_YEAR(Duration.ofDays(365));

    companion object {
        fun fromDuration(duration: Duration?): GraphStatDurations {
            return values().firstOrNull { it.duration == duration } ?: ALL_DATA
        }
    }
}