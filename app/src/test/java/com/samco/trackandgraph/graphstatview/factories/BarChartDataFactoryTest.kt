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
            duration = null,
            sumByCount = false
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

    @Test
    fun `test bar chart, with multiple labels, no end time, no duration`() {
        //PREPARE
        val end = ZonedDateTime.now().withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(time = end, label = "a"),
                dp(time = end.minusHours(1), label = "a"),
                dp(time = end.minusHours(5), value = 4.0, label = "b"),
                dp(time = end.minusDays(1), label = "a"),
                dp(time = end.minusDays(1), label = "b"),
                dp(time = end.minusDays(2), label = "b"),
                dp(time = end.minusDays(2).minusHours(1), value = 2.0, label = "b"),
                dp(time = end.minusDays(2).minusHours(2), label = "b"),
                dp(time = end.minusDays(2).minusHours(3), label = "b")
            ).asSequence()
        )

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            duration = null,
            sumByCount = false
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(2, barData.bars.size)
        assertEquals(listOf(0.0, 1.0, 2.0), barData.bars.first { it.title == "a" }.getyVals())
        assertEquals(listOf(5.0, 1.0, 4.0), barData.bars.first { it.title == "b" }.getyVals())
        assertEquals(listOf(endOfDay.minusDays(2), endOfDay.minusDays(1), endOfDay), barData.dates)
        assertEquals(0, barData.bounds.minX.toInt())
        assertEquals(2, barData.bounds.maxX.toInt())
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(6, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart, labels, end time, duration, sum by count`() {
        //PREPARE
        val end = ZonedDateTime.now().minusDays(180).withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(time = end.minusDays(1), value = 2.0, label = "a"),
                dp(time = end.minusDays(1), value = 2.0, label = "b"),
                dp(time = end.minusDays(2), value = 3.0, label = "a"),
                dp(time = end.minusDays(2), value = 1.0, label = "b"),
                dp(time = end.minusDays(3), value = 3.0, label = "a"),
                dp(time = end.minusDays(3), value = 1.0, label = "b"),
                dp(time = end.minusDays(4), value = 7.0, label = "a"),
                dp(time = end.minusDays(4), value = 5.0, label = "b"),
                dp(time = end.minusDays(5), value = 1.0, label = "a"),
                dp(time = end.minusDays(5), value = 1.0, label = "b"),
                dp(time = end.minusDays(6), value = 8.0, label = "a"),
                dp(time = end.minusDays(6), value = 9.0, label = "b"),
                dp(time = end.minusDays(7), value = 1.0, label = "a"),
                dp(time = end.minusDays(7), value = 1.0, label = "b"),
                dp(time = end.minusDays(8), value = 3.0, label = "a"),
                dp(time = end.minusDays(8), value = 2.0, label = "b"),
                dp(time = end.minusDays(9), value = 3.0, label = "a"),
                dp(time = end.minusDays(9), value = 4.0, label = "b"),

            ).asSequence()
        )

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            duration = Duration.ofDays(7),
            sumByCount = true
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(2, barData.bars.size)
        assertEquals(listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0), barData.bars.first { it.title == "a" }.getyVals())
        assertEquals(listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0), barData.bars.first { it.title == "b" }.getyVals())
        assertEquals(listOf(
            endOfDay.minusDays(6),
            endOfDay.minusDays(5),
            endOfDay.minusDays(4),
            endOfDay.minusDays(3),
            endOfDay.minusDays(2),
            endOfDay.minusDays(1),
            endOfDay
        ), barData.dates)
        assertEquals(0, barData.bounds.minX.toInt())
        assertEquals(6, barData.bounds.maxX.toInt())
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(2, barData.bounds.maxY.toInt())
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