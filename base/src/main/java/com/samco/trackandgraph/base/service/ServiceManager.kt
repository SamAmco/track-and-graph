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
import com.samco.trackandgraph.base.navigation.PendingIntentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ServiceManager {
    fun startTimerNotificationService()
    fun requestWidgetUpdatesForFeatureId(featureId: Long)
    fun requestWidgetsDisabledForFeatureId(featureId: Long)
}

internal class ServiceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingIntentProvider: PendingIntentProvider
) : ServiceManager {

    init {
        //Launch the service, if there are no timers it will stop its self immediately,
        // if there are timers it will display notifications and if it's already running
        // the call will be ignored. This works around the issue of the service being killed
        // and it not being possible to re-start it even if there are technically timers running
        // e.g. if you restore from a database that has running timers
        startTimerNotificationService()
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