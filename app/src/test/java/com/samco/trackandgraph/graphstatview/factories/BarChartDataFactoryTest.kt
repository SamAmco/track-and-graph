package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.base.database.dto.BarChartBarPeriod
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.functions.aggregation.AggregationPreferences
import com.samco.trackandgraph.functions.helpers.TimeHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class BarChartDataFactoryTest {

    private val defaultTimeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay: Duration = Duration.ZERO
        },
        ZoneId.systemDefault()
    )

    @Test
    fun `test bar chart, no label, no end time, no duration`() {
        //PREPARE
        val end = ZonedDateTime.now().withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(end),
                dp(end.minusHours(1)),
                dp(end.minusDays(1)),
                dp(end.minusDays(2).minusHours(1)),
                dp(end.minusDays(2).minusHours(2)),
                dp(end.minusDays(2).minusHours(3))
            ).asSequence()
        )

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            duration = null
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(1, barData.bars.size)
        assertEquals(listOf(3.0, 1.0, 2.0), barData.bars[0].getyVals())
        assertEquals(listOf(endOfDay.minusDays(2), endOfDay.minusDays(1), endOfDay), barData.dates)
        assertEquals(0, barData.bounds.minX.toInt())
        assertEquals(2, barData.bounds.maxX.toInt())
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(3, barData.bounds.maxY.toInt())
    }

    private fun dp(
        time: ZonedDateTime,
        value: Double = 1.0,
        label: String = ""
    ) = object : IDataPoint() {
        override val timestamp: OffsetDateTime = time.toOffsetDateTime()
        override val value: Double = value
        override val label: String = label
    }
}