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

package com.samco.trackandgraph.reminders.androidplatform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.algorithms.murmurHash3
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.navigation.PendingIntentProvider
import com.samco.trackandgraph.reminders.ReminderInteractor
import com.samco.trackandgraph.reminders.scheduling.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.Locale

/**
 * A WorkManager worker that serves as a fallback mechanism for reminder
 * notifications.
 *
 * This worker is part of a dual-scheduling system where both AlarmManager
 * and WorkManager are scheduled simultaneously for each reminder. The
 * WorkManager task is scheduled to trigger 5-15 minutes after the expected
 * AlarmManager time (depending on API level and exact alarm capabilities).
 *
 * ## Purpose
 * - **Primary role**: Acts as a safety net when AlarmManager fails to
 *   trigger reminders
 * - **Missed reminder detection**: Identifies and notifies users about
 *   reminders they missed e.g. if the device was off
 *
 * ## How it works
 * 1. **Normal flow**: AlarmManager triggers successfully and cancels this
 *    WorkManager task
 * 2. **Fallback flow**: If AlarmManager fails, this worker:
 *    - Calculates all missed reminders from the expected trigger time
 *      until now
 *    - Shows appropriate notifications (single vs multiple missed
 *      reminders)
 *    - Schedules the next reminder as normal (scheduling next always
 *      cancels any existing alarm/work-manager task)
 *
 * @see AndroidPlatformScheduler for the dual scheduling implementation
 * @see ReminderBroadcastReceiver for the AlarmManager success path
 */
@HiltWorker
class ReminderFallbackWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
    private val reminderScheduler: ReminderScheduler,
    private val timeProvider: TimeProvider,
    private val pendingIntentProvider: PendingIntentProvider
) : CoroutineWorker(context, params) {

    private data class NotificationContent(
        val title: String,
        val message: String
    )

    companion object {
        const val REMINDER_ID_KEY = "REMINDER_ID_KEY"
        const val EXPECTED_TRIGGER_TIME_KEY = "EXPECTED_TRIGGER_TIME_KEY"
        private const val MISSED_REMINDERS_CHANNEL_ID = "missed_reminder_notifications_channel"

        fun getWorkManagerTag(reminderId: Long): String {
            return "reminder_fallback_$reminderId"
        }

        fun createInputData(reminderId: Long, expectedTriggerTimeEpochMilli: Long): Data {
            return Data.Builder()
                .putLong(REMINDER_ID_KEY, reminderId)
                .putLong(EXPECTED_TRIGGER_TIME_KEY, expectedTriggerTimeEpochMilli)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        try {
            val reminderId = inputData.getLong(REMINDER_ID_KEY, -1)
            val expectedTriggerTime = inputData.getLong(EXPECTED_TRIGGER_TIME_KEY, -1)

            if (reminderId == -1L || expectedTriggerTime == -1L) {
                return Result.failure()
            }

            val reminder = dataInteractor.getReminderById(reminderId)
                ?: return Result.failure()

            // Check if the alarm actually missed its time
            val now = timeProvider.now().toInstant()
            val expectedInstant = Instant.ofEpochMilli(expectedTriggerTime)

            // If the expected trigger time is in the future, this shouldn't have triggered
            // This should never happen, but if anything ever goes out of alignment
            // it's best to just call schedule next because it will cancel the alarm and start
            // again
            if (expectedInstant.isAfter(now)) {
                reminderInteractor.scheduleNext(reminder)
                return Result.success()
            }

            // Calculate all missed reminders
            val missedReminders = calculateMissedReminders(reminder, expectedInstant, now)

            // Show notification for missed reminders
            if (missedReminders.isNotEmpty()) {
                showMissedReminderNotification(reminder.reminderName, missedReminders)
            }

            // Schedule the next reminder as normal
            reminderInteractor.scheduleNext(reminder)

            return Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "Error in ReminderFallbackWorker")
            return Result.failure()
        }
    }

    private fun calculateMissedReminders(
        reminder: Reminder,
        expectedInstant: Instant,
        now: Instant
    ): List<Instant> {
        val missedReminders = mutableListOf<Instant>()
        var currentTime = expectedInstant
        val maxAttempts = 100 // Safety limit to prevent infinite loops
        var attempts = 0

        while (attempts < maxAttempts && !currentTime.isAfter(now)) {
            missedReminders.add(currentTime)

            // Get the next reminder time after this one
            val nextInstant = reminderScheduler.scheduleNext(reminder, currentTime)
            if (nextInstant == null || nextInstant.isBefore(currentTime) || nextInstant == currentTime) {
                // No more reminders or invalid time, break to avoid infinite loop
                break
            }
            currentTime = nextInstant
            attempts++
        }

        return missedReminders
    }

    private fun showMissedReminderNotification(reminderName: String, missedTimes: List<Instant>) {
        createMissedReminderNotificationChannel()

        val context = applicationContext
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent = pendingIntentProvider.getMainActivityPendingIntent()

        val notificationContent = createNotificationContent(reminderName, missedTimes)

        val notification = NotificationCompat.Builder(context, MISSED_REMINDERS_CHANNEL_ID)
            .setContentTitle(notificationContent.title)
            .setContentText(notificationContent.message)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().murmurHash3(), notification)
    }

    private fun createNotificationContent(
        reminderName: String,
        missedTimes: List<Instant>
    ): NotificationContent {
        val context = applicationContext

        return if (missedTimes.size == 1) {
            val missedInstant = missedTimes.first()
            val missedDateTime = LocalDateTime.ofInstant(missedInstant, timeProvider.defaultZone())
            val nowDateTime = timeProvider.now()

            val formattedTime = if (missedDateTime.toLocalDate() == nowDateTime.toLocalDate()) {
                // Same day - show only time
                missedDateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
            } else {
                // Different day - show date and time
                missedDateTime.format(
                    DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
                )
            }

            NotificationContent(
                title = reminderName,
                message = context.getString(R.string.missed_reminder_single_content, formattedTime)
            )
        } else {
            NotificationContent(
                title = reminderName,
                message = context.getString(
                    R.string.missed_reminders_multiple_content,
                    missedTimes.size,
                )
            )
        }
    }

    private fun createMissedReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val context = applicationContext
            val name = context.getString(R.string.missed_reminders_notifications_channel_name)
            val descriptionText =
                context.getString(R.string.missed_reminders_notifications_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MISSED_REMINDERS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
