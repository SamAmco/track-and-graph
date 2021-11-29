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

package com.samco.trackandgraph.functionslib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.*

class TimeHelperTests {
    //TODO account for offsets of daylight savings

    private val basicAggregationPreferences = object : AggregationPreferences {
        override val firstDayOfWeek = DayOfWeek.MONDAY
        override val startTimeOfDay = Duration.ZERO
    }

    @Test
    fun testDurationHourWithOffset() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2021, 11, 29,
            4, 45, 0, 0, ZoneOffset.ofHours(1)
        )
        val temporal = Duration.ofHours(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 11, 29,
            4, 0, 0, 0, ZoneOffset.ofHours(1)
        )
        assertEquals(expected, answer)
    }

    @Test
    fun test() {
        assertTrue(OffsetDateTime.of(
            2021, 11, 29,
            0, 1, 0, 0, ZoneOffset.ofHours(0)
        ) > OffsetDateTime.of(
            2021, 11, 29,
            0, 1, 0, 0, ZoneOffset.ofHours(1)
        ))
    }

    @Test
    fun testPeriodWeekWithOffset() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2021, 11, 29,
            0, 1, 0, 0, ZoneOffset.ofHours(1)
        )
        val temporal = Period.ofWeeks(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 11, 29,
            0, 0, 0, 0, ZoneOffset.ofHours(1)
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testAggregationStartTimeOfDayAfterDateTime() {
        //GIVEN
        val uut = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek = DayOfWeek.MONDAY
                override val startTimeOfDay = Duration.ofHours(5)
            }
        )
        val dateTime = OffsetDateTime.of(
            2021, 11, 29,
            4, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 11, 28,
            5, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testAggregationStartTimeOfDayBeforeDateTime() {
        //GIVEN
        val uut = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek = DayOfWeek.MONDAY
                override val startTimeOfDay = Duration.ofHours(5)
            }
        )
        val dateTime = OffsetDateTime.of(
            2021, 11, 29,
            6, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 11, 29,
            5, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testAggregationStartDayOfWeekAfterDateTime() {
        //GIVEN
        val uut = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek = DayOfWeek.WEDNESDAY
                override val startTimeOfDay = Duration.ofHours(4)
            }
        )
        val dateTime = OffsetDateTime.of(
            2021, 11, 30,
            4, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofWeeks(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 11, 24,
            4, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testAggregationStartDayOfWeekBeforeDateTime() {
        //GIVEN
        val uut = TimeHelper(
            object : AggregationPreferences {
                override val firstDayOfWeek = DayOfWeek.WEDNESDAY
                override val startTimeOfDay = Duration.ofHours(5)
            }
        )
        val dateTime = OffsetDateTime.of(
            2021, 12, 2,
            6, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofWeeks(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2021, 12, 1,
            5, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationHour() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofHours(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            15, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverHour() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofHours(2)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationDay() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverDay() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(1).plus(Duration.ofNanos(1))

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationWeek() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(7)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverWeek() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Duration.ofDays(7).plus(Duration.ofNanos(1))

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        //Should fallback to a week anyway
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodWeek() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofWeeks(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverWeek() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofWeeks(1).plusDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodMonth() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverMonth() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(1).plusDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodQuater() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(3)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 4, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverQuater() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(3).plusDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodBiYear() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(6)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverBiYear() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofMonths(6).plusDays(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodYear() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofYears(1)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverYear() {
        //GIVEN
        val uut = TimeHelper(basicAggregationPreferences)
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC
        )
        val temporal = Period.ofYears(4)

        //WHEN
        val answer = uut.findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC
        )
        assertEquals(expected, answer)
    }

    @Test
    fun testGetQuaterForMonthValue() {
        val uut = TimeHelper(basicAggregationPreferences)
        assertEquals(
            listOf(1, 1, 1, 4, 4, 4, 7, 7, 7, 10, 10, 10),
            IntProgression.fromClosedRange(1, 12, 1)
                .map { uut.getQuaterForMonthValue(it) }
        )
    }

    @Test
    fun testGetBiYearForMonthValue() {
        val uut = TimeHelper(basicAggregationPreferences)
        assertEquals(
            listOf(1, 1, 1, 1, 1, 1, 7, 7, 7, 7, 7, 7),
            IntProgression.fromClosedRange(1, 12, 1)
                .map { uut.getBiYearForMonthValue(it) }
        )
    }
}