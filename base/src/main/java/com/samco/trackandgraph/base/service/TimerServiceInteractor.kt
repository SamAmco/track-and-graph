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

import android.content.Context
import android.content.Intent
import android.os.Build
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.navigation.PendingIntentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TimerServiceInteractor {
    fun startTimerNotificationService()
    fun requestWidgetUpdatesForFeatureId(featureId: Long)
    fun requestWidgetsDisabledForFeatureId(featureId: Long)
}

internal class TimerServiceInteractorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingIntentProvider: PendingIntentProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    private val dao: TrackAndGraphDatabaseDao
) : TimerServiceInteractor {

    private val ioJob = CoroutineScope(Job() + io)

    init {
        //Launch the timer notification service if there are any timers currently active.
        // Although the service is launched when timers are started it's possible that the database
        // contains active timers already that are unknown to the data interactor. For example
        // when restoring a database with active timers in it. Or if the service is killed in the
        // background for any reason.
        //
        //We must also only start the service if there are active timers because otherwise it will
        // not call startForeground when it is started which will crash the app.
        ioJob.launch {
            if (dao.getAllActiveTimerTrackers().isNotEmpty())
                startTimerNotificationService()
        }
    }

    override fun startTimerNotificationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, TimerNotificationService::class.java))
        } else {
            context.startService(Intent(context, TimerNotificationService::class.java))
        }
    }

    override fun requestWidgetUpdatesForFeatureId(featureId: Long) = context.sendBroadcast(
        pendingIntentProvider.getTrackWidgetUpdateForFeatureIdIntent(featureId)
    )

    override fun requestWidgetsDisabledForFeatureId(featureId: Long) = context.sendBroadcast(
        pendingIntentProvider.getTrackWidgetDisableForFeatureByIdIntent(featureId)
    )
}