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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.algorithms.murmurHash3
import com.samco.trackandgraph.navigation.PendingIntentProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingIntentProvider: PendingIntentProvider


    companion object Companion {
        private const val REMINDERS_CHANNEL_ID = "reminder_notifications_channel"
        const val ALARM_MESSAGE_KEY = "Message"
        const val ALARM_REMINDER_ID_KEY = "ReminderId"
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the support library
        if (Build.VERSION.SDK_INT >= 26) {
            val name = context.getString(R.string.reminders_notifications_channel_name)
            val descriptionText =
                context.getString(R.string.reminders_notifications_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(REMINDERS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        createNotificationChannel(context)

        val message = intent?.extras?.getString(ALARM_MESSAGE_KEY) ?: return

        val pendingIntent = pendingIntentProvider.getMainActivityPendingIntent()

        val notification = NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
            .setContentTitle(message)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentText("")
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(System.currentTimeMillis().murmurHash3(), notification)
        performVibrate(context)


        //Schedule the next notification for this specific reminder using WorkManager
        val reminderId = intent.extras?.getLong(ALARM_REMINDER_ID_KEY)
        if (reminderId != null) {
            // Cancel the fallback WorkManager task since the alarm fired successfully
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(ReminderFallbackWorker.getWorkManagerTag(reminderId))
            
            val work = OneTimeWorkRequestBuilder<ScheduleNextReminderWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(ScheduleNextReminderWorker.REMINDER_ID_KEY to reminderId))
                .build()

            WorkManager.getInstance(context).enqueue(work)
        }
    }

    @Suppress("DEPRECATION")
    private fun performVibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(250, 250, 250, 250), -1))
        } else if (Build.VERSION.SDK_INT >= 26) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(250, 250, 250, 250), -1))
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(longArrayOf(250, 250, 250, 250), -1)
        }
    }
}