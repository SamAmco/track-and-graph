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

import com.samco.trackandgraph.functionslib.AggregationPreferences
import com.samco.trackandgraph.functionslib.DataSample
import com.samco.trackandgraph.functionslib.TimeHelper
import com.samco.trackandgraph.database.entity.*
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

class TimeHistogramDataHelperTests {
    private val timeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay = Duration.ZERO
        }
    )

    @Test
    fun test_getHistogramBinsForSample_sumByVal_week_cont() {
        //GIVEN
        val month = OffsetDateTime.of(2020, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC)
        val sample = makeDataSample(
            IntProgression.fromClosedRange(6, 21, 1)
                .map { Pair(1.0, month.withDayOfMonth(it)) }
        )
        val window = TimeHistogramWindow.WEEK
        val feature = makeFeature(FeatureType.CONTINUOUS)
        val sumByCount = false

        //WHEN
        val answer = TimeHistogramDataHelper(timeHelper)
            .getHistogramBinsForSample(sample, window, feature, sumByCount)

        //THEN
        answer!!
        Assert.assertEquals(1, answer.keys.size)
        val vals = answer[0] ?: error("Key 0 not found")
        Assert.assertEquals(7, vals.size)
        val total = 3 + 3 + 2 + 2 + 2 + 2 + 2.0
        Assert.assertEquals(
            listOf(3 / total, 3 / total, 2 / total, 2 / total, 2 / total, 2 / total, 2 / total),
            vals
        )
    }

    @Test
    fun test_getHistogramBinsForSample_sumByVal_hour_disc() {
        //GIVEN
        val start = OffsetDateTime.of(2020, 7, 1, 9, 0, 1, 0, ZoneOffset.UTC)
        val sample = makeDataSample(
            IntProgression.fromClosedRange(0, 240 - 1, 1)
                .map { Pair((it % 3).toDouble(), start.plusMinutes(it.toLong())) }
        )
        val window = TimeHistogramWindow.HOUR
        val feature = makeFeature(FeatureType.DISCRETE, listOf(0, 1, 2))
        val sumByCount = false

        //WHEN
        val answer = TimeHistogramDataHelper(timeHelper)
            .getHistogramBinsForSample(sample, window, feature, sumByCount)

        //THEN
        answer!!
        Assert.assertEquals(3, answer.keys.size)

        val vals0 = answer[0] ?: error("Key 0 not found")
        Assert.assertEquals(60, vals0.size)
        val expected0 = List(60) { 0.0 }
        Assert.assertEquals(
            expected0,
            vals0
        )

        val total = 240.0
        val vals1 = answer[1] ?: error("Key 1 not found")
        Assert.assertEquals(60, vals1.size)
        val expected1 =
            mutableListOf<Double>().apply { repeat(20) { addAll(listOf(0.0, 4.0, 0.0)) } }
        Assert.assertEquals(
            expected1.map { it / total },
            vals1
        )

        val vals2 = answer[2] ?: error("Key 2 not found")
        Assert.assertEquals(60, vals2.size)
        val expected2 =
            mutableListOf<Double>().apply { repeat(20) { addAll(listOf(0.0, 0.0, 8.0)) } }
        Assert.assertEquals(
            expected2.map { it / total },
            vals2
        )
    }

    @Test
    fun test_getHistogramBinsForSample_sumByCount_year_disc() {
        //GIVEN
        val start = OffsetDateTime.of(2020, 1, 20, 9, 0, 1, 0, ZoneOffset.UTC)
        val sample = makeDataSample(
            IntProgression.fromClosedRange(0, 9, 1)
                .map { Pair((it % 2).toDouble(), start.plusMonths((it.toLong() * 5) % 12)) }
        )
        val window = TimeHistogramWindow.YEAR
        val feature = makeFeature(FeatureType.DISCRETE, listOf(0, 1))
        val sumByCount = true

        //WHEN
        val answer = TimeHistogramDataHelper(timeHelper)
            .getHistogramBinsForSample(sample, window, feature, sumByCount)

        //THEN
        answer!!
        Assert.assertEquals(2, answer.keys.size)
        val av = 1.0 / 10.0
        Assert.assertEquals(
            listOf(av, 0.0, 0.0, 0.0, av, 0.0, av, 0.0, av, 0.0, av, 0.0),
            answer[0] ?: error("Key 0 not found")
        )

        Assert.assertEquals(
            listOf(0.0, av, 0.0, av, 0.0, av, 0.0, 0.0, 0.0, av, 0.0, av),
            answer[1] ?: error("Key 1 not found")
        )
    }

    @Test
    fun test_getHistogramBinsForSample_no_data() {
        //GIVEN
        val sample = DataSample(emptyList())
        val window = TimeHistogramWindow.HOUR
        val feature = makeFeature(FeatureType.CONTINUOUS)
        val sumByCount = false

        //WHEN
        val answer = TimeHistogramDataHelper(timeHelper)
            .getHistogramBinsForSample(sample, window, feature, sumByCount)

        //THEN
        Assert.assertNull(answer)
    }

    @Test
    fun test_getHistogramBinsForSample_sumByVal_week_cont_StartTime() {
        //GIVEN
        val month = OffsetDateTime.of(2020, 7, 1, 3, 30, 0, 0, ZoneOffset.UTC)
        val sample = makeDataSample(
            IntProgression.fromClosedRange(7, 22, 1)
                .map { Pair(1.0, month.withDayOfMonth(it)) }
        )
        val window = TimeHistogramWindow.WEEK
        val feature = makeFeature(FeatureType.CONTINUOUS)
        val sumByCount = false

        val timeHelper = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek = DayOfWeek.MONDAY
                override val startTimeOfDay = Duration.ofHours(4)
            }
        )

        //WHEN
        val answer = TimeHistogramDataHelper(timeHelper)
            .getHistogramBinsForSample(sample, window, feature, sumByCount)

        //THEN
        answer!!
        Assert.assertEquals(1, answer.keys.size)
        val vals = answer[0] ?: error("Key 0 not found")
        Assert.assertEquals(7, vals.size)
        val expected = listOf(2, 3, 3, 2, 2, 2, 2)
        val total = expected.sum().toDouble()
        Assert.assertEquals(expected.map { it / total }, vals)
    }

    private fun makeFeature(
        featureType: FeatureType,
        discreteValues: List<Int> = listOf()
    ) =
        Feature(
            0L, "", 0L, featureType, discreteValues.map { DiscreteValue(it, "") },
            0, false, 0.0, ""
        )

    private fun makeDataSample(dataPoints: List<Pair<Double, OffsetDateTime>>) =
        DataSample(
            dataPoints.map { DataPoint(it.second, 0L, it.first, "", "") }
        )
}