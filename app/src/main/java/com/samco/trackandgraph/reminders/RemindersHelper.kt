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

import android.annotation.SuppressLint
import android.app.AlarmManager
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.system.AlarmManagerWrapper
import com.samco.trackandgraph.system.ReminderPrefWrapper
import com.samco.trackandgraph.system.StoredAlarmInfo
import com.samco.trackandgraph.system.SystemInfoProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

interface AlarmInteractor {
    suspend fun syncAlarms()

    suspend fun clearAlarms()
}

internal interface RemindersHelper : AlarmInteractor {
    fun createAlarms(reminder: Reminder)

    fun deleteAlarms(reminder: Reminder)
}

@OptIn(FlowPreview::class)
@Singleton
internal class RemindersHelperImpl @Inject constructor(
    private val reminderPref: ReminderPrefWrapper,
    private val alarmManager: AlarmManagerWrapper,
    private val systemInfoProvider: SystemInfoProvider,
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    private val moshi: Moshi,
) : RemindersHelper, CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + io

    private val onSyncRequest = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    private val syncMutex = Mutex()
    private val clearMutex = Mutex()

    private val moshiStoredAlarmInfoListType = Types
        .newParameterizedType(List::class.java, StoredAlarmInfo::class.java)

    init {
        launch {
            onSyncRequest
                .debounce(200)
                .collect { performSync() }
        }
    }

    override suspend fun syncAlarms() {
        onSyncRequest.emit(Unit)
    }

    private suspend fun clearLegacyReminders() {
        if (reminderPref.hasMigratedLegacyReminders) return

        val reminders = dataInteractor.getAllRemindersSync()
        for (reminder in reminders) {
            for (day in 1..7) {
                val id = ((reminder.id * 10) + day).toInt()
                alarmManager.cancelLegacyAlarm(id, reminder.alarmName)
            }
        }

        reminderPref.hasMigratedLegacyReminders = true
    }

    private suspend fun performSync() = syncMutex.withLock {
        withContext(io) {
            clearLegacyReminders()
            clearAlarms()
            val reminders = dataInteractor.getAllRemindersSync()
            for (reminder in reminders) createAlarms(reminder)
        }
    }

    override suspend fun clearAlarms() = clearMutex.withLock {
        withContext(io) {
            getPendingIntentsFromPrefs().forEach { alarmManager.cancel(it) }
            updateStoredIntentsInPrefs(emptyList())
        }
    }

    @SuppressLint("NewApi")
    @Synchronized
    override fun createAlarms(reminder: Reminder) {
        val allIntents = createAlarmPendingIntents(reminder)
        allIntents.forEach { kvp ->
            val calendar = getNextReminderTime(reminder.time, kvp.key)
            if (systemInfoProvider.buildVersionSdkInt < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    kvp.value
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    kvp.value
                )
            }
        }
    }

    @Synchronized
    override fun deleteAlarms(reminder: Reminder) {
        val allIntents = getPendingIntentsFromPrefs()
        val intentsToRemove = allIntents
            .filter { it.reminderId == reminder.id }
        intentsToRemove.forEach {
            alarmManager.cancel(it)
        }
        val intentsToKeep = allIntents
            .filter { it.reminderId != reminder.id }
        updateStoredIntentsInPrefs(intentsToKeep)
    }

    private fun createAlarmPendingIntents(reminder: Reminder): Map<Int, StoredAlarmInfo> {
        val intentMap = reminder.checkedDays.toList()
            .mapIndexed { i, checked -> i + 1 to checked }
            .filter { pair -> pair.second }
            .map { it.first }
            .associateWith {
                StoredAlarmInfo(
                    reminder.id,
                    reminder.alarmName,
                    //A large prime to minimise the possibility of key clashes
                    ((System.nanoTime() - it) % 2_147_483_647L).toInt()
                )
            }
        addStoredIntentsToPrefs(intentMap.values)
        return intentMap.mapValues { it.value }
    }

    private fun getPendingIntentsFromPrefs(): List<StoredAlarmInfo> {
        val storedIntents = reminderPref.getStoredIntents()
        return try {
            storedIntents?.let {
                moshi.adapter<List<StoredAlarmInfo>>(moshiStoredAlarmInfoListType)
                    .fromJson(it)
            } ?: emptyList()
        } catch (t: Throwable) {
            Timber.e("Could not parse stored intents string: $storedIntents")
            emptyList()
        }
    }

    private fun updateStoredIntentsInPrefs(intentsToStore: Collection<StoredAlarmInfo>) {
        reminderPref.putStoredIntents(
            moshi.adapter<List<StoredAlarmInfo>>(moshiStoredAlarmInfoListType)
                .toJson(intentsToStore.toList())
        )
    }

    @Synchronized
    private fun addStoredIntentsToPrefs(intentsToStore: Collection<StoredAlarmInfo>) =
        updateStoredIntentsInPrefs(getPendingIntentsFromPrefs().plus(intentsToStore))

    private fun getNextReminderTime(time: LocalTime, dayOfWeek: Int) =
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val orderedDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
            val currentDay = (orderedDays.indexOf(get(Calendar.DAY_OF_WEEK)) + 1)
            var dayDiff = dayOfWeek - currentDay
            if (dayDiff < 0) dayDiff += 7
            else if (dayDiff == 0) {
                val currentHour = get(Calendar.HOUR_OF_DAY)
                val reminderHour = time.hour
                val currentMin = get(Calendar.MINUTE)
                val reminderMin = time.minute

                if (currentHour > reminderHour) dayDiff += 7
                else if (currentHour == reminderHour && currentMin >= reminderMin) dayDiff += 7
            }
            add(Calendar.DAY_OF_MONTH, dayDiff)
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}