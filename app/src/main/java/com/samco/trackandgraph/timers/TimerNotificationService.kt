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

package com.samco.trackandgraph.timers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_STOPWATCH
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.helpers.formatTimeDuration
import com.samco.trackandgraph.navigation.PendingIntentProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class TimerNotificationService : Service() {

    @Inject
    lateinit var dataInteractor: DataInteractor

    @Inject
    lateinit var pendingIntentProvider: PendingIntentProvider

    private val serviceDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val serviceScope by lazy { CoroutineScope(SupervisorJob() + serviceDispatcher) }
    private var serviceJob: Job? = null
    private var updateJobIsRunning = false
    private val notificationUpdater = NotificationUpdater()

    // Flag to track if startForeground has been called at least once
    // This is managed by ensureForegroundState and the NotificationUpdater loop
    private var calledStartForeGround = false

    private val notificationManager: NotificationManager by lazy {
        ContextCompat.getSystemService(
            this@TimerNotificationService, NotificationManager::class.java
        ) as NotificationManager
    }

    private inner class NotificationUpdater {
        private var activeTrackers: List<DisplayTracker> = emptyList()
        private var updateJob: Job? = null

        private fun clearOldNotificationsAndJob() {
            // Cancel all notifications except the persistent one
            activeTrackers.forEach { notificationManager.cancel(it.id.toInt()) }
            activeTrackers = emptyList()
        }

        fun setNotifications(newTrackers: List<DisplayTracker>) {
            updateJob?.cancel()
            clearOldNotificationsAndJob()

            activeTrackers = newTrackers
                .filter { it.timerStartInstant != null }
                .sortedBy { it.timerStartInstant }

            if (activeTrackers.isEmpty()) return

            // Start a single job to update all notifications
            updateJob = serviceScope.launch {
                while (isActive) {
                    syncNotifications()
                    delay(1000)
                }
            }
        }

        private fun syncNotifications() {
            try {
                activeTrackers.forEachIndexed { index, tracker ->
                    val instant = tracker.timerStartInstant ?: return@forEachIndexed
                    val durationSecs = Duration.between(instant, Instant.now()).seconds
                    val notification = buildNotification(tracker.featureId, tracker.name, instant)
                        .setContentText(formatTimeDuration(durationSecs))
                        .build()

                    // First tracker should always use the persistent notification ID
                    val notificationId =
                        if (index == 0) PERSISTENT_NOTIFICATION_ID
                        else tracker.id.toInt()

                    try {
                        if (!calledStartForeGround && index == 0) {
                            // First time showing the first notification - use startForeground
                            startForegroundService(notificationId, notification)
                            calledStartForeGround = true
                        } else {
                            // Use notify for all subsequent notifications and updates
                            notificationManager.notify(notificationId, notification)
                        }
                    } catch (e: Exception) {
                        // Log error but continue with other notifications
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (updateJobIsRunning) return super.onStartCommand(intent, flags, startId)

        createChannel()

        updateJobIsRunning = true
        serviceJob?.cancel() // Cancel any existing job
        serviceJob = serviceScope.launch {
            try {
                dataInteractor.getDataUpdateEvents()
                    .filter { it is DataUpdateType.TrackerUpdated || it is DataUpdateType.TrackerDeleted }
                    .map { } // Map to Unit to trigger debounce/collection
                    .onStart { emit(Unit) } // Emit immediately on start to load initial state
                    .debounce(200) // Debounce rapid updates
                    .catch { e ->
                        // Log error
                        Timber.e(e)
                        updateJobIsRunning = false
                        stopService()
                    }
                    .collect {
                        try {
                            val newTrackers = dataInteractor
                                .getAllActiveTimerTrackers()
                                .filter { it.timerStartInstant != null }

                            // If after the update there are no active trackers, stop the service
                            if (newTrackers.isEmpty()) {
                                // Ensure startForeground was called if needed
                                ensureStartForegroundCalled()
                                stopService()
                            } else {
                                notificationUpdater.setNotifications(newTrackers)
                            }
                        } catch (t: Throwable) {
                            // Log error
                            Timber.e(t)
                            stopService()
                        }
                    }
            } finally {
                updateJobIsRunning = false
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
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

    private fun stopService() {
        stopForeground()
        stopSelf()
    }

    override fun onDestroy() {
        try {
            serviceJob?.cancel()
            serviceScope.cancel()
            serviceDispatcher.close()
        } catch (t: Throwable) {
            Timber.e(t)
        }
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(
        featureId: Long,
        name: String,
        startTimeInstant: Instant
    ): NotificationCompat.Builder {
        val durationSecs = Duration.between(startTimeInstant, Instant.now()).seconds
        val pendingIntent = pendingIntentProvider.getDurationInputActivityPendingIntent(featureId)

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

    companion object {
        private const val CHANNEL_ID = "TIMER_NOTIFICATION_SERVICE_CHANNEL"
        private const val PERSISTENT_NOTIFICATION_ID = Int.MAX_VALUE - 123
    }

}
