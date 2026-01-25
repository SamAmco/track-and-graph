/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.data.database.dto.BarChart
import com.samco.trackandgraph.data.database.dto.BarChartBarPeriod
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSample
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.graphstatview.factories.helpers.DataDisplayIntervalHelper
import com.samco.trackandgraph.graphstatview.functions.aggregation.AggregationPreferences
import com.samco.trackandgraph.graphstatview.functions.aggregation.GlobalAggregationPreferences
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BarChartDataFactoryTest {

    private val defaultTimeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay: Duration = Duration.ZERO
        },
        ZoneId.systemDefault()
    )

    private val dataInteractor = mock<DataInteractor>()
    private val dataSampler = mock<DataSampler>()
    private val testCoroutineDispatcher = UnconfinedTestDispatcher()

    private fun uut() = BarChartDataFactory(
        dataInteractor = dataInteractor,
        dataSampler = dataSampler,
        dataDisplayIntervalHelper = DataDisplayIntervalHelper(),
        ioDispatcher = testCoroutineDispatcher,
        timeHelper = TimeHelper(GlobalAggregationPreferences),
    )

    @Test
    fun `test bar chart respects data interval helper upper Y bound, if range is not fixed`() = runTest {
        //PREPARE
        val end = ZonedDateTime.now().withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(end),
                dp(end.minusDays(1), value = 1.435),
            ).asSequence()
        ) {}

        var dataSampledCalled = false
        val onDataSampled: (List<DataPoint>) -> Unit = {
            dataSampledCalled = true
        }

        whenever(dataSampler.getDataSampleForFeatureId(1L, null))
            .thenReturn(dataSample)

        //EXECUTE
        val uut = uut()
        val viewData = uut.getViewData(
            graphOrStat = dummyGraphOrStat(),
            config = BarChart(
                id = 1,
                graphStatId = 1,
                featureId = 1,
                endDate = GraphEndDate.Latest,
                sampleSize = null,
                yRangeType = YRangeType.DYNAMIC,
                yTo = 0.0,
                scale = 1.0,
                barPeriod = BarChartBarPeriod.DAY,
                sumByCount = false
            ),
            onDataSampled = onDataSampled
        )

        //VERIFY
        assertEquals(1, viewData.bars.size)
        assertEquals(1.5, viewData.bounds.maxY.toDouble(), 0.0001)
        assertEquals(true, dataSampledCalled)
    }

    @Test
    fun `test should not return a range of 0 to 0 if theres no data`() {
        //PREPARE
        val end = ZonedDateTime.now().withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(dp(end, value = 0.0)).asSequence()
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = null,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(1, barData.segmentSeries.size)
        assertEquals(listOf(endOfDay), barData.dates)
        assertEquals(listOf(0.0), barData.segmentSeries[0].segmentSeries.getyVals())

        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(0.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(1, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart, no label, end time, no duration`() {
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
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(1, barData.segmentSeries.size)
        assertEquals(listOf(3.0, 1.0, 2.0), barData.segmentSeries[0].segmentSeries.getyVals())
        assertEquals(listOf(endOfDay.minusDays(2), endOfDay.minusDays(1), endOfDay), barData.dates)
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(2.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(3, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart, with multiple labels, end time, no duration`() {
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
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(2, barData.segmentSeries.size)

        //b comes first because the sum of all b values is larger than the sum of all a values
        assertEquals(listOf(0.0, 1.0, 2.0), barData.segmentSeries[1].segmentSeries.getyVals())
        assertEquals(listOf(5.0, 1.0, 4.0), barData.segmentSeries[0].segmentSeries.getyVals())

        assertEquals(listOf(endOfDay.minusDays(2), endOfDay.minusDays(1), endOfDay), barData.dates)
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(2.5, barData.bounds.maxX.toDouble(), 0.1)
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
                dp(time = end.minusDays(9), value = 4.0, label = "b")
            ).asSequence()
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = Duration.ofDays(7),
            sumByCount = true,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(2, barData.segmentSeries.size)
        assertEquals(
            listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
            barData.segmentSeries.first { it.segmentSeries.title == "a" }.segmentSeries.getyVals()
        )
        assertEquals(
            listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
            barData.segmentSeries.first { it.segmentSeries.title == "b" }.segmentSeries.getyVals()
        )
        assertEquals(
            listOf(
                endOfDay.minusDays(6),
                endOfDay.minusDays(5),
                endOfDay.minusDays(4),
                endOfDay.minusDays(3),
                endOfDay.minusDays(2),
                endOfDay.minusDays(1),
                endOfDay
            ), barData.dates
        )
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(6.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(2, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart over daylight savings with different start of day`() {
        //PREPARE
        val timeHelper = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
                override val startTimeOfDay: Duration = Duration.ofHours(15)
            },
            ZoneId.systemDefault()
        )
        //Sun March 26 2023 at 4pm BST (which is the day after the clocks go forward)
        //actually the offset is +1 but atZoneSimilarLocal will make it +1 for us
        val end = OffsetDateTime.parse("2023-03-25T16:00:00Z")
            .atZoneSimilarLocal(ZoneId.of("Europe/London"))

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(time = end, label = "a"),
                dp(time = end.minusDays(1), label = "a"),
            ).asSequence()
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = timeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        assertEquals(1, barData.segmentSeries.size)
        assertEquals(listOf(1.0, 1.0), barData.segmentSeries[0].segmentSeries.getyVals())
        assertEquals(
            listOf(
                ZonedDateTime.parse("2023-03-25T14:59:59.999999999Z[Europe/London]"),
                ZonedDateTime.parse("2023-03-26T14:59:59.999999999+01:00[Europe/London]")
            ),
            barData.dates
        )
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(1.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(1, barData.bounds.maxY.toInt())

    }

    @Test
    fun `test bar chart fixed y to`() {
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
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.FIXED,
            yTo = 80.0,
            scale = 1.0
        )

        //VERIFY
        assertEquals(1, barData.segmentSeries.size)
        assertEquals(listOf(3.0, 1.0, 2.0), barData.segmentSeries[0].segmentSeries.getyVals())
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(2.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(80, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart with scale`() {
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
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = end,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 3.0
        )

        //VERIFY
        assertEquals(2, barData.segmentSeries.size)

        //b comes first because the sum of all b values is larger than the sum of all a values
        assertEquals(listOf(0.0, 3.0, 6.0), barData.segmentSeries[1].segmentSeries.getyVals())
        assertEquals(listOf(15.0, 3.0, 12.0), barData.segmentSeries[0].segmentSeries.getyVals())
        assertEquals(-0.5, barData.bounds.minX.toDouble(), 0.1)
        assertEquals(2.5, barData.bounds.maxX.toDouble(), 0.1)
        assertEquals(0, barData.bounds.minY.toInt())
        assertEquals(18, barData.bounds.maxY.toInt())
    }

    @Test
    fun `test bar chart no end time`() {
        //PREPARE
        val end = ZonedDateTime.now().withHour(22)

        val dataSample = DataSample.fromSequence(
            listOf(
                dp(end.minusHours(1)),
                dp(end.minusDays(1)),
                dp(end.minusDays(2).minusHours(1)),
                dp(end.minusDays(2).minusHours(2)),
                dp(end.minusDays(2).minusHours(3))
            ).asSequence()
        ) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = defaultTimeHelper,
            dataSample = dataSample,
            endTime = null,
            barSize = BarChartBarPeriod.DAY,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        val endOfDay = end
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusNanos(1)
        assertEquals(1, barData.segmentSeries.size)
        assertEquals(listOf(3.0, 1.0, 1.0), barData.segmentSeries[0].segmentSeries.getyVals())
        assertEquals(listOf(endOfDay.minusDays(2), endOfDay.minusDays(1), endOfDay), barData.dates)
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

    private fun dummyGraphOrStat() = GraphOrStat(
        id = 1,
        groupId = 1,
        name = "name",
        type = GraphStatType.BAR_CHART,
        displayIndex = 1
    )

    @Test
    fun `test monthly bar chart with data points at end of each month`() {
        //PREPARE
        // Create data points at the last second of each month for a year (2024 is a leap year)
        // Going from December 2023 to November 2024 (12 months)
        val dataPoints = listOf(
            dp(ZonedDateTime.of(2024, 11, 30, 23, 59, 59, 0, ZoneId.of("UTC"))), // Nov 2024
            dp(ZonedDateTime.of(2024, 10, 31, 23, 59, 59, 0, ZoneId.of("UTC"))), // Oct 2024
            dp(ZonedDateTime.of(2024, 9, 30, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Sep 2024
            dp(ZonedDateTime.of(2024, 8, 31, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Aug 2024
            dp(ZonedDateTime.of(2024, 7, 31, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Jul 2024
            dp(ZonedDateTime.of(2024, 6, 30, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Jun 2024
            dp(ZonedDateTime.of(2024, 5, 31, 23, 59, 59, 0, ZoneId.of("UTC"))),  // May 2024
            dp(ZonedDateTime.of(2024, 4, 30, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Apr 2024
            dp(ZonedDateTime.of(2024, 3, 31, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Mar 2024
            dp(ZonedDateTime.of(2024, 2, 29, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Feb 2024 (leap year)
            dp(ZonedDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneId.of("UTC"))),  // Jan 2024
            dp(ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"))), // Dec 2023
        )

        val timeHelper = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
                override val startTimeOfDay: Duration = Duration.ZERO
            },
            ZoneId.of("UTC")
        )

        val dataSample = DataSample.fromSequence(dataPoints.asSequence()) {}

        //EXECUTE
        val barData = BarChartDataFactory.getBarData(
            timeHelper = timeHelper,
            dataSample = dataSample,
            endTime = null,
            barSize = BarChartBarPeriod.MONTH,
            sampleSize = null,
            sumByCount = false,
            yRangeType = YRangeType.DYNAMIC,
            yTo = 0.0,
            scale = 1.0
        )

        //VERIFY
        // We should have 12 bars, one for each month
        // If the bug exists, some months will be merged because bar boundaries
        // are calculated incorrectly using end-period instead of beginning-of-period
        val values = barData.segmentSeries[0].segmentSeries.getyVals()

        // Build a description of what we got for the error message
        val barSummary = barData.dates.mapIndexed { i, date ->
            "${date.month} ${date.year}: ${values[i]}"
        }.joinToString(", ")

        assertEquals(
            "Expected 12 bars (one per month), but got ${barData.dates.size}. " +
                "Bars: [$barSummary]. " +
                "This bug occurs because bar boundaries are calculated using " +
                "currentBarEndTime.minus(period) instead of finding the actual " +
                "beginning of the period.",
            12,
            barData.dates.size
        )

        assertEquals(12, values.size)

        // All values should be 1.0 - if the bug exists, some months will have 0.0
        // and others will have 2.0 because data points jumped to wrong months
        values.forEachIndexed { index, value ->
            assertEquals(
                "Bar at index $index (date: ${barData.dates[index]}) should have value 1.0",
                1.0,
                value.toDouble(),
                0.0001
            )
        }
    }
}