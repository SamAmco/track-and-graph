package com.samco.trackandgraph.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.samco.trackandgraph.reminders.AlarmReceiver
import com.samco.trackandgraph.reminders.AlarmReceiver.Companion.ALARM_MESSAGE_KEY
import kotlinx.serialization.Serializable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Serializable
internal data class StoredAlarmInfo(
    val reminderId: Long,
    val reminderName: String,
    val pendingIntentId: Int
)

internal interface AlarmManagerWrapper {
    fun cancelLegacyAlarm(id: Int, alarmName: String)

    fun cancel(storedAlarmInfo: StoredAlarmInfo)

    @RequiresApi(Build.VERSION_CODES.S)
    fun canScheduleExactAlarms(): Boolean

    fun setExact(type: Int, triggerAtMillis: Long, storedAlarmInfo: StoredAlarmInfo)

    fun set(type: Int, triggerAtMillis: Long, storedAlarmInfo: StoredAlarmInfo)
}

internal class AlarmManagerWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmManagerWrapper {
    private val alarmManager: AlarmManager
        get() {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

    override fun cancelLegacyAlarm(id: Int, alarmName: String) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, AlarmReceiver::class.java).putExtra(ALARM_MESSAGE_KEY, alarmName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun cancel(storedAlarmInfo: StoredAlarmInfo) =
        alarmManager.cancel(createPendingIntent(storedAlarmInfo))

    @RequiresApi(Build.VERSION_CODES.S)
    override fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()

    override fun setExact(type: Int, triggerAtMillis: Long, storedAlarmInfo: StoredAlarmInfo) =
        alarmManager.setExact(type, triggerAtMillis, createPendingIntent(storedAlarmInfo))

    override fun set(type: Int, triggerAtMillis: Long, storedAlarmInfo: StoredAlarmInfo) =
        alarmManager.set(type, triggerAtMillis, createPendingIntent(storedAlarmInfo))

    private fun createPendingIntent(storedIndent: StoredAlarmInfo): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            storedIndent.pendingIntentId,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(ALARM_MESSAGE_KEY, storedIndent.reminderName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}