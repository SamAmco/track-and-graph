/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.base.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_STOPWATCH
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.DisplayTracker
import com.samco.trackandgraph.base.helpers.formatTimeDuration
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.DataUpdateType
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.navigation.PendingIntentProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class TimerNotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "TIMER_NOTIFICATION_SERVICE_CHANNEL"
    }

    @Inject
    lateinit var dataInteractor: DataInteractor

    @Inject
    lateinit var pendingIntentProvider: PendingIntentProvider

    @Inject
    @IODispatcher
    lateinit var io: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    private val job = Job()
    private val jobScope by lazy { CoroutineScope(job + defaultDispatcher) }
    private var updateJobIsRunning = false

    private val notificationManager by lazy {
        ContextCompat.getSystemService(
            this@TimerNotificationService, NotificationManager::class.java
        ) as NotificationManager
    }

    private inner class NotificationUpdater {
        private var activeTrackers: List<DisplayTracker> = emptyList()
        private var updateJob: Job? = null

        private fun clearOldNotificationsAndJob() {
            // Cancel the previous update job if it's running
            updateJob?.cancel()
            updateJob = null

            // Cancel notifications shown by the previous job
            activeTrackers.forEach { notificationManager.cancel(it.id.toInt()) }
            activeTrackers = emptyList() // Clear the list for the next update
        }

        @Synchronized
        fun setNotifications(newTrackers: List<DisplayTracker>) {
            clearOldNotificationsAndJob()

            activeTrackers = newTrackers
                .filter { it.timerStartInstant != null }
                .sortedBy { it.timerStartInstant }

            if (activeTrackers.isEmpty()) return

            // Start a single job to update all notifications
            updateJob = jobScope.launch {
                while (isActive) {
                    activeTrackers.forEachIndexed { index, tracker ->
                        val instant = tracker.timerStartInstant ?: return@forEachIndexed // Should not happen due to filter
                        val builder = buildNotification(tracker.id, tracker.name, instant)
                        val durationSecs = Duration.between(instant, Instant.now()).seconds
                        builder.setContentText(formatTimeDuration(durationSecs))
                        val notification = builder.build()
                        val notificationId = tracker.id.toInt()

                        // Always use startForegroundService for the first notification (index 0)
                        // to correctly update the foreground state notification.
                        if (index == 0) {
                            startForegroundService(notificationId, notification)
                            // Ensure the service-level flag is set
                            this@TimerNotificationService.calledStartForeGround = true
                        } else {
                            // Use notify for all subsequent notifications.
                            notificationManager.notify(notificationId, notification)
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun startForegroundService(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }

    private val notificationUpdater = NotificationUpdater()

    // Flag to track if startForeground has been called at least once
    // This is managed by ensureForegroundState and the NotificationUpdater loop
    private var calledStartForeGround = false

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationChannel.setShowBadge(true)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (updateJobIsRunning) return super.onStartCommand(intent, flags, startId)

        createChannel()

        updateJobIsRunning = true
        jobScope.launch {
            dataInteractor.getDataUpdateEvents()
                .filter { it is DataUpdateType.TrackerUpdated || it is DataUpdateType.TrackerDeleted }
                .map { } // Map to Unit to trigger debounce/collection
                .onStart { emit(Unit) } // Emit immediately on start to load initial state
                .debounce(200) // Debounce rapid updates
                .collect {
                    val newTrackers = withContext(io) {
                        dataInteractor.getAllActiveTimerTrackers()
                            .filter { it.timerStartInstant != null }
                    }


                    // If after the update there are no active trackers, stop the service
                    if (newTrackers.isEmpty()) {
                        ensureStartForegroundCalled() // Ensure startForeground was called if needed
                        stopForeground()
                        stopSelf()
                        // Exiting the collect block will allow the service jobScope to complete if needed
                    } else {
                        notificationUpdater.setNotifications(newTrackers)
                    }
                }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @Suppress("DEPRECATION")
    private fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    /**
     * Ensures startForeground is called at least once, even if no timers are active,
     * to comply with foreground service requirements. Creates and immediately cancels
     * a temporary notification if needed.
     */
    private fun ensureStartForegroundCalled() {
        if (!calledStartForeGround) {
            // Use a temporary ID
            val tempId = 123
            val tempNotification = NotificationCompat
                .Builder(this@TimerNotificationService, CHANNEL_ID)
                .setSmallIcon(R.drawable.timer_notification_icon)
                .setSilent(true)
                .setAutoCancel(true)
                .build()
            startForegroundService(tempId, tempNotification)
            calledStartForeGround = true
            // Immediately cancel the temporary notification as it's not needed
            notificationManager.cancel(tempId)
        }
    }

    override fun onDestroy() {
        job.cancel()
    }

    private fun buildNotification(
        trackerId: Long,
        name: String,
        startTimeInstant: Instant
    ): NotificationCompat.Builder {
        val durationSecs = Duration.between(startTimeInstant, Instant.now()).seconds
        val pendingIntent = pendingIntentProvider
            .getDurationInputActivityPendingIntent(trackerId, startTimeInstant.toString())

        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop_timer,
            getString(R.string.stop),
            pendingIntent
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(name)
            .setOngoing(true)
            .setSmallIcon(R.drawable.timer_notification_icon)
            .setContentText(formatTimeDuration(durationSecs))
            .setWhen(startTimeInstant.epochSecond * 1000L)
            .setUsesChronometer(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(CATEGORY_STOPWATCH)
            .setContentIntent(pendingIntentProvider.getMainActivityPendingIntent(false))
            .addAction(stopAction)
            .setAutoCancel(true)
    }
}
