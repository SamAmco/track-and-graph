package com.samco.trackandgraph.reminders.androidplatform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.samco.trackandgraph.reminders.ReminderNotificationParams
import com.samco.trackandgraph.reminders.PlatformScheduler
import com.samco.trackandgraph.reminders.StoredAlarmInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidPlatformScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : PlatformScheduler {
    private val alarmManager: AlarmManager
        get() {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

    override fun set(
        triggerAtMillis: Long,
        reminderNotificationParams: ReminderNotificationParams
    ) {
        val operation = createPendingIntent(
            requestCode = reminderNotificationParams.alarmId,
            reminderId = reminderNotificationParams.reminderId,
            reminderName = reminderNotificationParams.reminderName,
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
            reminderId = storedAlarmInfo.reminderId,
            reminderName = storedAlarmInfo.reminderName,
        )
    )

    override fun cancel(reminderNotificationParams: ReminderNotificationParams) = alarmManager.cancel(
        createPendingIntent(
            requestCode = reminderNotificationParams.alarmId,
            reminderId = reminderNotificationParams.reminderId,
            reminderName = reminderNotificationParams.reminderName,
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
            Intent(context, ReminderBroadcastReceiver::class.java)
                .putExtra(ReminderBroadcastReceiver.ALARM_MESSAGE_KEY, reminderName)
                .putExtra(ReminderBroadcastReceiver.ALARM_REMINDER_ID_KEY, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}