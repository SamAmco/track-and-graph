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

package com.samco.trackandgraph.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.samco.trackandgraph.reminders.AlarmReceiver
import com.samco.trackandgraph.reminders.AlarmReceiver.Companion.ALARM_MESSAGE_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
@Deprecated("Alarm information should no longer be persisted, use [AlarmInfo]")
internal data class StoredAlarmInfo(
    val reminderId: Long,
    val reminderName: String,
    val pendingIntentId: Int
)

// TODO add support for group ID
internal data class AlarmInfo(
    val alarmId: Int,
    val reminderId: Long,
    val reminderName: String,
)

internal interface AlarmManagerWrapper {
    fun set(triggerAtMillis: Long, alarmInfo: AlarmInfo)

    @Deprecated("This remains only for users of 9.x who still have persisted alarms to cancel them on first sync")
    fun cancel(storedAlarmInfo: StoredAlarmInfo)

    fun cancel(alarmInfo: AlarmInfo)
}

internal class AlarmManagerWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmManagerWrapper {
    private val alarmManager: AlarmManager
        get() {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

    override fun set(
        triggerAtMillis: Long,
        alarmInfo: AlarmInfo
    ) {
        val operation = createPendingIntent(
            requestCode = alarmInfo.alarmId,
            reminderId = alarmInfo.reminderId,
            reminderName = alarmInfo.reminderName,
        )
        if (canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    @Deprecated("See AlarmManagerWrapper interface")
    override fun cancel(storedAlarmInfo: StoredAlarmInfo) = alarmManager.cancel(
        createPendingIntent(
            requestCode = storedAlarmInfo.pendingIntentId,
            reminderId = 0L, // Legacy - reminderId not needed for cancellation
            reminderName = storedAlarmInfo.reminderName,
        )
    )

    override fun cancel(alarmInfo: AlarmInfo) = alarmManager.cancel(
        createPendingIntent(
            requestCode = alarmInfo.alarmId,
            reminderId = alarmInfo.reminderId,
            reminderName = alarmInfo.reminderName,
        )
    )

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()
    }

    private fun createPendingIntent(
        requestCode: Int,
        reminderId: Long,
        reminderName: String,
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(ALARM_MESSAGE_KEY, reminderName)
                .putExtra(AlarmReceiver.ALARM_REMINDER_ID_KEY, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}