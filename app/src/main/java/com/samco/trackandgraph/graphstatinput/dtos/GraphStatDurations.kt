package com.samco.trackandgraph.graphstatinput.dtos

import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount

class GraphStatDurations private constructor(val temporalAmount: TemporalAmount?){

    companion object {
        val allData = GraphStatDurations(null)
        val aDay = GraphStatDurations(Period.ofDays(1))
        val aWeek = GraphStatDurations(Period.ofWeeks(1))
        val aMonth = GraphStatDurations(Period.ofMonths(1))
        val threeMonths = GraphStatDurations(Period.ofMonths(3))
        val sixMonths = GraphStatDurations(Period.ofMonths(6))
        val aYear = GraphStatDurations(Period.ofYears(1))
        //TODO add custom duration

        private val valueMap = mapOf(
            null to allData,

            //These are here for legacy reasons. Some people might have graphs from the old
            // version which did not use periods and only used durations. As such their selected
            // duration will not be recognised unless we map the old versions too.
            Duration.ofDays(1) to aDay,
            Duration.ofDays(7) to aWeek,
            Duration.ofDays(31) to aMonth,
            Duration.ofDays(93) to threeMonths,
            Duration.ofDays(183) to sixMonths,
            Duration.ofDays(365) to aYear,

            Period.ofDays(1) to aDay,
            Period.ofWeeks(1) to aWeek,
            Period.ofMonths(1) to aMonth,
            Period.ofMonths(3) to threeMonths,
            Period.ofMonths(6) to sixMonths,
            Period.ofYears(1) to aYear
        )

        fun fromTemporalAmount(temporalAmount: TemporalAmount?): GraphStatDurations {
            return temporalAmount?.let {
                valueMap[temporalAmount] ?: GraphStatDurations(temporalAmount)
            } ?: allData
        }
    }
}