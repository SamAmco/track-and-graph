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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.GraphEndDate
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphFeature
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.functions.aggregation.AggregationPreferences
import com.samco.trackandgraph.functions.helpers.TimeHelper
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period

@OptIn(ExperimentalCoroutinesApi::class)
class LineGraphDataFactoryTest {

    private val dataInteractor: DataInteractor = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val timeHelper: TimeHelper = TimeHelper(
        aggregationPreferences = object :
            AggregationPreferences {
            override val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay: Duration = Duration.ofSeconds(0)
        },
    )

    private val daggerComponent = DaggerLineGraphDataFactoryTestComponent
        .builder()
        .dataInteractor(dataInteractor)
        .ioDispatcher(ioDispatcher)
        .defaultDispatcher(defaultDispatcher)
        .timeHelper(timeHelper)
        .build()

    private fun uut() = daggerComponent.provideLineGraphDataFactory()

    @Test
    fun `plot total line ending at last data point ends at last data point`() = runTest {
        val now = OffsetDateTime.now()

        val graphOrStat = GraphOrStat(
            id = 1L,
            groupId = 1L,
            name = "Graph",
            type = GraphStatType.LINE_GRAPH,
            displayIndex = 0
        )

        val lineGraphFeature = LineGraphFeature(
            id = 1L,
            lineGraphId = 1L,
            featureId = 1L,
            name = "Feature",
            colorIndex = 0,
            averagingMode = LineGraphAveraginModes.NO_AVERAGING,
            plottingMode = LineGraphPlottingModes.GENERATE_DAILY_TOTALS,
            pointStyle = LineGraphPointStyle.NONE,
            offset = 0.0,
            scale = 1.0,
            durationPlottingMode = DurationPlottingMode.NONE
        )

        val lineGraphWithFeatures = LineGraphWithFeatures(
            id = 1L,
            graphStatId = 1L,
            features = listOf(lineGraphFeature),
            sampleSize = null,
            yRangeType = YRangeType.DYNAMIC,
            yFrom = 0.0,
            yTo = 100.0,
            endDate = GraphEndDate.Latest,
        )

        whenever(dataInteractor.getLineGraphByGraphStatId(1L))
            .thenReturn(lineGraphWithFeatures)

        whenever(dataInteractor.getDataSampleForFeatureId(1L))
            .thenReturn(DataSample.fromSequence(
                onDispose = {},
                data = sequenceOf(
                    100, 101, 102, 103, 104, 105, 106, 107, 108, 109
                ).map { dpDaysAgo(it, now) }
            ))

        val lineGraphDataFactory = uut()
        val lineGraphViewData = lineGraphDataFactory.getViewData(graphOrStat)

        assertEquals(1, lineGraphViewData.lines.size)
        val line = lineGraphViewData.lines[0]
        assertEquals(10, line.line?.size())

        val startEpochMilli = now.minusDays(109L).toInstant().toEpochMilli()
        val end = now.minusDays(100L)
        val endEpochMilli = end.toInstant().toEpochMilli()
        val expected = endEpochMilli - startEpochMilli
        val actual = line.line!!.getX(9).toLong() - line.line!!.getX(0).toLong()

        assertEquals(expected, actual)

        val a = timeHelper
            .findEndOfTemporal(end, Period.ofDays(1))
        val expectedEnd = a.toOffsetDateTime()
        assertEquals(expectedEnd, lineGraphViewData.endTime)
    }

    private fun dpDaysAgo(
        days: Int,
        from: OffsetDateTime = OffsetDateTime.now(),
    ) = object : IDataPoint() {
        override val timestamp: OffsetDateTime
            get() {
                val result = from.minusDays(days.toLong())
                return result
            }
        override val value: Double = 1.0
        override val label: String = ""
    }
}