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
        private var notifications: List<Int>? = null
        private var updateJobs: List<Job>? = null
        private var calledStartForeGround = false

        private fun clearOldNotifications() {
            this.notifications?.forEach { notificationManager.cancel(it) }
            this.updateJobs?.forEach { it.cancel() }
        }

        private fun createUpdateJob(tracker: DisplayTracker, isPrimary: Boolean): Job? {
            val instant = tracker.timerStartInstant ?: return null
            val builder = buildNotification(tracker.id, tracker.name, instant)
            val id = tracker.id.toInt()
            return jobScope.launch {
                while (true) {
                    val durationSecs = Duration.between(instant, Instant.now()).seconds
                    builder.setContentText(formatTimeDuration(durationSecs))
                    val notification = builder.build()
                    if (isPrimary) {
                        startForegroundService(id, notification)
                        calledStartForeGround = true
                    } else notificationManager.notify(id, notification)
                    delay(1000)
                }
            }
        }

        fun setNotifications(features: List<DisplayTracker>) =
            synchronized(this@TimerNotificationService) {
                clearOldNotifications()

                notifications = features
                    .filter { it.timerStartInstant != null }
                    .map { it.id.toInt() }

                //We have to call start foreground before stop foreground so we always
                // call startForeground even if there are no notifications somehow like if
                // the timer was deleted from the db between start timer being called and the
                // service starting.
                if (!calledStartForeGround && notifications?.isEmpty() != false) {
                    notifications = listOf(123)
                    startForegroundService(
                        123,
                        NotificationCompat
                            .Builder(this@TimerNotificationService, CHANNEL_ID)
                            .setSmallIcon(R.drawable.timer_notification_icon)
                            .setOnlyAlertOnce(true)
                            .setSilent(true)
                            .setAutoCancel(true)
                            .build()
                    )
                    calledStartForeGround = true
                }

                updateJobs = features
                    .sortedBy { it.timerStartInstant }
                    .mapIndexed { index, feature -> createUpdateJob(feature, index == 0) }
                    .filterNotNull()
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
        notificationUpdater.setNotifications(emptyList())

        updateJobIsRunning = true
        jobScope.launch {
            dataInteractor.getDataUpdateEvents()
                .filter { it is DataUpdateType.TrackerUpdated || it is DataUpdateType.TrackerDeleted }
                .map { }
                .onStart { emit(Unit) }
                .debounce(200)
                .collect {
                    val newFeatures = withContext(io) {
                        dataInteractor.getAllActiveTimerTrackers()
                            .filter { it.timerStartInstant != null }
                    }
                    if (newFeatures.isEmpty()) {
                        notificationUpdater.setNotifications(emptyList())
                        stopForeground()
                        stopSelf()
                    } else notificationUpdater.setNotifications(newFeatures)
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

    override fun onDestroy() {
        job.cancel()
        updateJobIsRunning = false
        super.onDestroy()
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
            .addAction(stopAction)
            .setAutoCancel(true)
    }
}