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
import com.samco.trackandgraph.reminders.scheduling.AlarmScheduler
import com.samco.trackandgraph.system.AlarmInfo
import com.samco.trackandgraph.system.AlarmManagerWrapper
import com.samco.trackandgraph.system.ReminderPrefWrapper
import com.samco.trackandgraph.system.StoredAlarmInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AlarmInteractor {

    /**
     * This should be called on app start up, device boot, and any time reminders are modified.
     * It ensures all reminders in the user database have their **next** alarm scheduled.
     * Once an alarm is triggered for a reminder, it is the responsibility of the
     * [AlarmReceiver] to schedule the next alarm for that reminder via [scheduleNext].
     */
    suspend fun syncAlarms()

    /**
     * This should be called when a reminder notification is triggered to schedule the next
     * alarm for that reminder.
     */
    suspend fun scheduleNext(reminder: Reminder)

    /**
     * This should be called when the given reminder is being deleted to cancel all alarms
     * for that reminder.
     */
    suspend fun deleteAlarms(reminder: Reminder)

    /**
     * This should be called for example when the user is about to restore a different database
     * to cancel all alarms for all reminders in the users current database.
     */
    suspend fun clearAlarms()
}

@OptIn(FlowPreview::class)
@Singleton
internal class AlarmInteractorImpl @Inject constructor(
    private val reminderPref: ReminderPrefWrapper,
    private val alarmManager: AlarmManagerWrapper,
    private val dataInteractor: DataInteractor,
    private val alarmScheduler: AlarmScheduler,
    private val json: Json,
    @IODispatcher private val io: CoroutineDispatcher,
) : AlarmInteractor {

    private val mutex = Mutex()

    override suspend fun syncAlarms() = mutex.withLock {
        withContext(io) {
            clearLegacyReminders()
            clearAlarmsInternal()
            val reminders = dataInteractor.getAllRemindersSync()
            for (reminder in reminders) createAlarms(reminder)
        }
    }

    override suspend fun scheduleNext(reminder: Reminder) = mutex.withLock {
        withContext(io) {
            createAlarms(reminder)
        }
    }

    override suspend fun deleteAlarms(reminder: Reminder) = mutex.withLock {
        withContext(io) {
            val alarmInfo = AlarmInfo(
                alarmId = reminder.id.toAlarmId(),
                reminderId = reminder.id,
                reminderName = reminder.alarmName
            )
            alarmManager.cancel(alarmInfo)
        }
    }

    override suspend fun clearAlarms() = mutex.withLock {
        withContext(io) {
            clearAlarmsInternal()
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

        storedIntents.forEach { alarmManager.cancel(it) }
        reminderPref.clear()
    }

    private suspend fun clearAlarmsInternal() {
        val alarmInfos = dataInteractor.getAllRemindersSync().map {
            AlarmInfo(
                alarmId = it.id.toAlarmId(),
                reminderId = it.id,
                reminderName = it.alarmName,
            )
        }
        for (alarmInfo in alarmInfos) alarmManager.cancel(alarmInfo)
    }

    private fun createAlarms(reminder: Reminder) {
        val nextAlarmTimeMillis = alarmScheduler.scheduleNext(reminder)
        if (nextAlarmTimeMillis != null) {
            val alarmInfo = AlarmInfo(
                alarmId = reminder.id.toAlarmId(),
                reminderId = reminder.id,
                reminderName = reminder.alarmName
            )
            alarmManager.set(
                triggerAtMillis = nextAlarmTimeMillis.toEpochMilli(),
                alarmInfo = alarmInfo
            )
        }
    }

    private fun Long.toAlarmId(): Int = murmurHash3()
}