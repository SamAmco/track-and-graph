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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.samco.trackandgraph.database.Reminder
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.threeten.bp.LocalTime
import java.util.*

class RemindersHelper {
    companion object {
        fun syncAlarms(context: Context) = CoroutineScope(Dispatchers.IO + Job()).launch {
            val dataSource = TrackAndGraphDatabase
                .getInstance(context).trackAndGraphDatabaseDao
            for (reminder in dataSource.getAllRemindersSync()) {
                deleteAlarms(reminder, context)
                createAlarms(reminder, context)
            }
        }

        fun createAlarms(reminder: Reminder, context: Context) {
            val allIntents = getAllAlarmIntents(reminder, context, true)
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            allIntents.forEach { kvp ->
                val calendar = getNextReminderTime(reminder.time, kvp.key)
                alarmMgr.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7L,
                    kvp.value
                )
            }
        }

        fun deleteAlarms(reminder: Reminder, context: Context) {
            val allIntents = getAllAlarmIntents(reminder, context, false)
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            allIntents.forEach { kvp -> alarmMgr.cancel(kvp.value) }
        }

        private fun getAllAlarmIntents(reminder: Reminder, context: Context, filterUnchecked: Boolean): Map<Int, PendingIntent> {
            val days = reminder.checkedDays.toList()
                .mapIndexed { i, checked -> i + 1 to checked}.toMap()
            return days
                .filter { kvp -> !filterUnchecked || kvp.value }
                .map { day -> day.key to
                        Intent(context, AlarmReceiver::class.java)
                            .putExtra("Message", reminder.alarmName)
                            .let { intent ->
                                val id = ((reminder.id * 10) + day.key).toInt()
                                PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                            }
                }.toMap()
        }

        private fun getNextReminderTime(time: LocalTime, dayOfWeek: Int) = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val orderedDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
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
        }
    }
}