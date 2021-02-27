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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.R
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val message = (intent?.extras?.get("Message") as String?) ?: return

        val pendingIntent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .let { PendingIntent.getActivity(context, 0, it, 0) }

        val notification = NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
            .setContentTitle(message)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentText("")
            .setContentIntent(pendingIntent)
            .setDefaults(DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(System.currentTimeMillis().toInt() % 10, notification)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(250, 250, 250, 250), -1))
        } else vibrator.vibrate(longArrayOf(250, 250, 250, 250), -1)
    }
}

class RecreateAlarms : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val validActions = listOf(
            "action.REMINDERS_CHANGED",
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.MY_PACKAGE_REPLACED"
        )
        if (!validActions.contains(intent?.action)) return
        if (context == null) return
        runBlocking { RemindersHelper.syncAlarms(context) }
    }
}
