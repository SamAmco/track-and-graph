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
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_STOPWATCH
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.DisplayFeature
import com.samco.trackandgraph.base.helpers.formatTimeDuration
import com.samco.trackandgraph.base.model.DataInteractor
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

    private val notificationManager by lazy {
        ContextCompat.getSystemService(
            this@TimerNotificationService, NotificationManager::class.java
        ) as NotificationManager
    }

    private inner class NotificationUpdater {
        private var notifications: List<Int>? = null
        private var updateJobs: List<Job>? = null

        private fun clearOldNotifications() {
            this.notifications?.forEach { notificationManager.cancel(it) }
            this.updateJobs?.forEach { it.cancel() }
        }

        private fun createUpdateJob(feature: DisplayFeature, isPrimary: Boolean): Job? {
            val instant = feature.timerStartInstant ?: return null
            val builder = buildNotification(feature.id, feature.name, instant)
            return jobScope.launch {
                while (true) {
                    val durationSecs = Duration.between(instant, Instant.now()).seconds
                    builder.setContentText(formatTimeDuration(durationSecs))
                    val id = feature.id.toInt()
                    val notification = builder.build()
                    if (isPrimary) startForeground(id, notification)
                    else notificationManager.notify(id, notification)
                    delay(1000)
                }
            }
        }

        fun setNotifications(features: List<DisplayFeature>) =
            synchronized(this@TimerNotificationService) {
                clearOldNotifications()

                notifications = features
                    .filter { it.timerStartInstant != null }
                    .map { it.id.toInt() }

                updateJobs = features
                    .sortedBy { it.timerStartInstant }
                    .mapIndexed { index, feature -> createUpdateJob(feature, index == 0) }
                    .filterNotNull()
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
        createChannel()
        jobScope.launch {
            dataInteractor.getDataUpdateEvents()
                .onStart { emit(Unit) }
                .debounce(200)
                .collect {
                    val newFeatures = withContext(io) {
                        dataInteractor.getAllActiveTimerFeatures()
                            .filter { it.timerStartInstant != null }
                    }
                    if (newFeatures.isEmpty()) {
                        notificationUpdater.setNotifications(emptyList())
                        stopForeground(true)
                        stopSelf()
                    } else notificationUpdater.setNotifications(newFeatures)
                }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun buildNotification(
        id: Long,
        name: String,
        startTimeInstant: Instant
    ): NotificationCompat.Builder {
        val durationSecs = Duration.between(startTimeInstant, Instant.now()).seconds
        val pendingIntent = pendingIntentProvider
            .getDurationInputActivityPendingIntent(id, startTimeInstant.toString())

        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop_timer,
            getString(R.string.stop),
            pendingIntent
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(name)
            .setOngoing(true)
            //TODO use a different small icon ?
            .setSmallIcon(R.drawable.notification_icon)
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