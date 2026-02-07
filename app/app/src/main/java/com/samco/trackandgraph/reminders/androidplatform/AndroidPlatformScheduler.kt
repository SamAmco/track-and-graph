package com.samco.trackandgraph.reminders.androidplatform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.samco.trackandgraph.reminders.PlatformScheduler
import com.samco.trackandgraph.reminders.ReminderNotificationParams
import com.samco.trackandgraph.reminders.StoredAlarmInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.Instant
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

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
        // Cancel any existing alarms and work for this reminder first
        cancel(reminderNotificationParams)

        // Schedule the AlarmManager alarm
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

        // Schedule the WorkManager fallback
        scheduleWorkManagerFallback(triggerAtMillis, reminderNotificationParams)
    }

    override suspend fun getNextScheduledMillis(reminderNotificationParams: ReminderNotificationParams): Long? {
        val workManager = WorkManager.getInstance(context)
        val tag = ReminderFallbackWorker.getWorkManagerTag(reminderNotificationParams.reminderId)

        return try {
            val workInfos = workManager.getWorkInfosByTag(tag).get()

            // Find the enqueued or running work for this reminder
            val workInfo = workInfos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
            if (workInfo == null) return null

            // Extract the trigger time from the work's UUID
            extractTriggerTime(workInfo.id)
        } catch (e: Exception) {
            // If there's any error querying WorkManager, return null
            Timber.e(e, "Error querying WorkManager for next scheduled reminder")
            null
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

    override fun cancel(reminderNotificationParams: ReminderNotificationParams) {
        // Cancel the AlarmManager alarm
        alarmManager.cancel(
            createPendingIntent(
                requestCode = reminderNotificationParams.alarmId,
                reminderId = reminderNotificationParams.reminderId,
                reminderName = reminderNotificationParams.reminderName,
            )
        )

        // Cancel the WorkManager fallback
        cancelWorkManagerFallback(reminderNotificationParams)
    }

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleWorkManagerFallback(
        triggerAtMillis: Long,
        reminderNotificationParams: ReminderNotificationParams
    ) {
        // Calculate the fallback delay - use the maximum possible delay for AlarmManager
        // On newer APIs with exact alarms, use a shorter delay (5 minutes)
        // On older APIs without exact alarms, use a longer delay (15 minutes)
        val fallbackDelayMinutes = if (canScheduleExactAlarms()) 5L else 15L
        val fallbackTriggerAtMillis = triggerAtMillis + (fallbackDelayMinutes * 60 * 1000)

        val inputData = ReminderFallbackWorker.createInputData(
            reminderNotificationParams.reminderId,
            triggerAtMillis
        )

        val workId = workIdForTriggerTime(triggerAtMillis)
        val workRequest = OneTimeWorkRequestBuilder<ReminderFallbackWorker>()
            .setId(workId)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(false)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .setRequiresStorageNotLow(false)
                    .build()
            )
            .setInputData(inputData)
            .setInitialDelay(
                fallbackTriggerAtMillis - Instant.now().toEpochMilli(),
                TimeUnit.MILLISECONDS
            )
            .addTag(ReminderFallbackWorker.getWorkManagerTag(reminderNotificationParams.reminderId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderFallbackWorker.getUniqueWorkName(reminderNotificationParams.reminderId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelWorkManagerFallback(reminderNotificationParams: ReminderNotificationParams) {
        val tag = ReminderFallbackWorker.getWorkManagerTag(reminderNotificationParams.reminderId)
        // Cancel by tag for backwards compatibility with old work scheduled before unique work names
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
        WorkManager.getInstance(context).cancelUniqueWork(ReminderFallbackWorker.getUniqueWorkName(reminderNotificationParams.reminderId))
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

    private fun workIdForTriggerTime(triggerAtMillis: Long): UUID {
        val bb = ByteBuffer.allocate(16)
        // First 8 bytes = the scheduled time
        bb.putLong(triggerAtMillis)
        // Remaining 8 bytes = entropy
        bb.putLong(Random.nextLong())
        return UUID(bb.getLong(0), bb.getLong(8))
    }

    private fun extractTriggerTime(id: UUID): Long {
        return ByteBuffer.allocate(8).putLong(0, id.mostSignificantBits).getLong(0)
    }
}