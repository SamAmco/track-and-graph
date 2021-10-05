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

package com.samco.trackandgraph.statistics

import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.*

class Statistics_findBeginningOfTemporal_KtTest {
    private val aggPreferences = AggregationWindowPreferences(DayOfWeek.MONDAY)
    private val aggPreferences4AM = AggregationWindowPreferences(DayOfWeek.MONDAY, Duration.ofHours(4))

    @Test
    fun testDurationHour() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofHours(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            15, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverHour() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofHours(2)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationDay() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 6, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 6, 8,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverDay() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(1).plus(Duration.ofNanos(1))

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal, aggPreferences)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationWeek() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(7)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal, aggPreferences)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverWeek() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(7).plus(Duration.ofNanos(1))

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationMonth() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(31)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverMonth() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(31).plus(Duration.ofNanos(1))

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationQuater() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365/4)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 4, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverQuater() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365/4).plusDays(2)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationBiYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365/2)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverBiYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365/2).plusDays(2)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testDurationOverYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Duration.ofDays(365*4)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodWeek() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofWeeks(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal, aggPreferences)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 6,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverWeek() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofWeeks(1).plusDays(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodMonth() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverMonth() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(1).plusDays(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 7, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodQuater() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(3)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 4, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverQuater() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(3).plusDays(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodBiYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 5, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(6)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverBiYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofMonths(6).plusDays(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofYears(1)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testPeriodOverYear() {
        //GIVEN
        val dateTime = OffsetDateTime.of(
            2020, 7, 8,
            15, 45, 32, 432, ZoneOffset.UTC)
        val temporal = Period.ofYears(4)

        //WHEN
        val answer = findBeginningOfTemporal(dateTime, temporal)

        //THEN
        val expected = OffsetDateTime.of(
            2020, 1, 1,
            0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(expected, answer)
    }

    @Test
    fun testGetQuaterForMonthValue() {
        assertEquals(
            listOf(1,1,1,4,4,4,7,7,7,10,10,10),
            IntProgression.fromClosedRange(1, 12, 1)
                .map { getQuaterForMonthValue(it) }
        )
    }

    @Test
    fun testGetBiYearForMonthValue() {
        assertEquals(
            listOf(1,1,1,1,1,1,7,7,7,7,7,7),
            IntProgression.fromClosedRange(1, 12, 1)
                .map { getBiYearForMonthValue(it) }
        )
    }
}