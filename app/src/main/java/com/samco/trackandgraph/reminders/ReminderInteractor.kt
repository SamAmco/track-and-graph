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

package com.samco.trackandgraph.reminders

import com.samco.trackandgraph.data.algorithms.murmurHash3
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderInteractor {

    /**
     * This should be called when the device is restarted, the app is started,
     * the app is re-installed etc. This ensures that reminder notifications
     * are scheduled for all reminders in the user database, scheduling them
     * if they are not already scheduled.
     *
     * This avoids cancelling existing notifications for 2 reasons:
     *
     * 1. Calculating when a reminder should be next scheduled can be expensive
     * in the case of a time since last based on a complex function for example.
     * This will avoid calling [ReminderScheduler.scheduleNext] for any
     * reminder in the database that is already scheduled.
     *
     * 2. This avoids cancelling missed reminder notifications. For example if the
     * user turns their device off for several days and misses 3 notifications, when
     * they restart their device this function will see that there is a missed reminder
     * notification scheduled and not cancel it (the next reminder will be scheduled after
     * that missed notification reminder anyway.)
     */
    suspend fun ensureReminderNotifications()

    /**
     * This should be called when a reminder notification is triggered to
     * schedule the next notification for that reminder. It will schedule only
     * one notification (the next one for the given reminder). It should also
     * be called any time a reminder is modified or anything that might change
     * its next notification time. Any previously scheduled notifications for
     * the reminder will be cancelled/replaced.
     */
    suspend fun scheduleNext(reminder: Reminder)

    /**
     * Returns the next scheduled notification for the given reminder, or null if
     * there is no next scheduled notification.
     */
    suspend fun getNextScheduled(reminder: Reminder): NextScheduled

    /**
     * This should be called when the given reminder is being deleted to cancel
     * any upcoming notifications for that reminder.
     */
    suspend fun cancelReminderNotifications(reminder: Reminder)

    /**
     * This should be called for example when the user is about to restore a
     * different database to cancel all notifications for all reminders in the
     * users current database.
     */
    suspend fun clearNotifications()
}

sealed interface NextScheduled {
    data class AtInstant(val instant: Instant) : NextScheduled
    data object Never : NextScheduled
}

@OptIn(FlowPreview::class)
@Singleton
internal class ReminderInteractorImpl @Inject constructor(
    private val reminderPref: ReminderPrefWrapper,
    private val platformScheduler: PlatformScheduler,
    private val dataInteractor: DataInteractor,
    private val reminderScheduler: ReminderScheduler,
    private val json: Json,
    @IODispatcher private val io: CoroutineDispatcher,
) : ReminderInteractor {

    private val mutex = Mutex()

    override suspend fun ensureReminderNotifications() = mutex.withLock {
        withContext(io) {
            clearLegacyReminders()
            val reminders = dataInteractor.getAllRemindersSync()
            for (reminder in reminders) {
                val params = reminder.toReminderNotificationParams()
                if (platformScheduler.getNextScheduledMillis(params) == null) {
                    createNextNotification(reminder)
                }
            }
        }
    }

    override suspend fun scheduleNext(reminder: Reminder) = mutex.withLock {
        withContext(io) {
            createNextNotification(reminder)
        }
    }

    override suspend fun getNextScheduled(reminder: Reminder): NextScheduled {
        val nextEpochMilli = platformScheduler.getNextScheduledMillis(reminder.toReminderNotificationParams())
        return when (nextEpochMilli) {
            null -> NextScheduled.Never
            else -> NextScheduled.AtInstant(Instant.ofEpochMilli(nextEpochMilli))
        }
    }

    override suspend fun cancelReminderNotifications(reminder: Reminder) = mutex.withLock {
        withContext(io) {
            val reminderNotificationParams = ReminderNotificationParams(
                alarmId = reminder.id.toAlarmId(),
                reminderId = reminder.id,
                reminderName = reminder.reminderName
            )
            platformScheduler.cancel(reminderNotificationParams)
        }
    }

    override suspend fun clearNotifications() = mutex.withLock {
        withContext(io) {
            clearNotificationsInternal()
        }
    }

    private fun clearLegacyReminders() {
        val storedIntentsEncoded = reminderPref.getStoredIntents()
        val storedIntents = try {
            storedIntentsEncoded?.let {
                json.decodeFromString<List<StoredAlarmInfo>>(it)
            } ?: emptyList()
        } catch (t: Throwable) {
            Timber.e(t, "Could not parse stored intents string: $storedIntentsEncoded")
            emptyList()
        }

        storedIntents.forEach { platformScheduler.cancel(it) }
        reminderPref.clear()
    }

    private suspend fun clearNotificationsInternal() {
        val reminderNotificationParams = dataInteractor.getAllRemindersSync()
            .map { it.toReminderNotificationParams() }
        for (params in reminderNotificationParams) platformScheduler.cancel(params)
    }

    private fun createNextNotification(reminder: Reminder) {
        val nextInstant = reminderScheduler.scheduleNext(reminder)
        if (nextInstant != null) {
            platformScheduler.set(
                triggerAtMillis = nextInstant.toEpochMilli(),
                reminderNotificationParams = reminder.toReminderNotificationParams()
            )
        }
    }

    private fun Reminder.toReminderNotificationParams() = ReminderNotificationParams(
        alarmId = id.toAlarmId(),
        reminderId = id,
        reminderName = reminderName
    )

    private fun Long.toAlarmId(): Int = murmurHash3()
}