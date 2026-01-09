/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders.scheduling

import com.samco.trackandgraph.FakeDataSampler
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.reminders.reminderFixture
import com.samco.trackandgraph.time.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime

internal class TimeSinceLastReminderSchedulerTest {

    private val timeProvider = FakeTimeProvider()
    private val dataSampler = FakeDataSampler()
    private val uut = ReminderSchedulerImpl(timeProvider, dataSampler)

    @Test
    fun `returns null when feature id is null`() = runTest {
        // PREPARE
        val reminder = reminderFixture.copy(
            featureId = null,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertNull("Should return null when feature id is null", result)
    }

    @Test
    fun `returns null when no data points exist for feature`() = runTest {
        // PREPARE
        val featureId = 123L
        dataSampler.setDataPointsForFeature(featureId, emptyList())

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertNull("Should return null when no data points exist", result)
    }

    @Test
    fun `returns null when feature does not exist`() = runTest {
        // PREPARE - don't set any data for the feature
        val reminder = reminderFixture.copy(
            featureId = 999L,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertNull("Should return null when feature does not exist", result)
    }

    @Test
    fun `schedules first reminder when within first interval`() = runTest {
        // PREPARE - Last tracked 1 day ago, first interval is 3 days
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))

        val lastTrackedTime = OffsetDateTime.of(2024, 1, 9, 10, 0, 0, 0, ZoneOffset.UTC)
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = lastTrackedTime,
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for 3 days after last tracked (Jan 12)
        val expected = ZonedDateTime.of(2024, 1, 12, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule first reminder at first interval", expected, result)
    }

    @Test
    fun `returns null when past first interval and no second interval`() = runTest {
        // PREPARE - Last tracked 5 days ago, first interval is 3 days, no second interval
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val lastTrackedTime = OffsetDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC)
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = lastTrackedTime,
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should return null since there's no second interval and first has passed
        assertNull("Should return null when past first interval with no second interval", result)
    }

    @Test
    fun `schedules using second interval when past first interval`() = runTest {
        // PREPARE - Last tracked 5 days ago, first interval is 3 days, second interval is 1 day
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val lastTrackedTime = OffsetDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC)
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = lastTrackedTime,
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = IntervalPeriodPair(interval = 1, period = Period.DAYS)
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - First interval is Jan 13, then iterate by 1 day until after Jan 15
        // Jan 13 + 1 = Jan 14, Jan 14 + 1 = Jan 15, Jan 15 + 1 = Jan 16
        val expected = ZonedDateTime.of(2024, 1, 16, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule using second interval iterations", expected, result)
    }

    @Test
    fun `handles hours interval correctly`() = runTest {
        // PREPARE - Last tracked 2 hours ago, first interval is 4 hours
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 12, 0, 0, 0, ZoneId.of("UTC"))

        val lastTrackedTime = OffsetDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC)
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = lastTrackedTime,
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 4, period = Period.HOURS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for 4 hours after last tracked (2:00 PM)
        val expected = ZonedDateTime.of(2024, 1, 10, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should handle hours interval correctly", expected, result)
    }

    @Test
    fun `handles weeks interval correctly`() = runTest {
        // PREPARE - Last tracked 1 week ago, first interval is 2 weeks
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 17, 10, 0, 0, 0, ZoneId.of("UTC"))

        val lastTrackedTime = OffsetDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC)
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = lastTrackedTime,
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 2, period = Period.WEEKS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for 2 weeks after last tracked (Jan 24)
        val expected = ZonedDateTime.of(2024, 1, 24, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should handle weeks interval correctly", expected, result)
    }

    @Test
    fun `uses latest data point when multiple exist`() = runTest {
        // PREPARE - Multiple data points, should use the most recent
        val featureId = 123L
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        // Data points are returned newest first
        dataSampler.setDataPointsForFeature(featureId, listOf(
            DataPoint(
                timestamp = OffsetDateTime.of(2024, 1, 14, 10, 0, 0, 0, ZoneOffset.UTC), // Most recent
                featureId = featureId,
                value = 3.0,
                label = "",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC),
                featureId = featureId,
                value = 2.0,
                label = "",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneOffset.UTC),
                featureId = featureId,
                value = 1.0,
                label = "",
                note = ""
            )
        ))

        val reminder = reminderFixture.copy(
            featureId = featureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule 3 days after Jan 14 (the most recent)
        val expected = ZonedDateTime.of(2024, 1, 17, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should use latest data point", expected, result)
    }
}

