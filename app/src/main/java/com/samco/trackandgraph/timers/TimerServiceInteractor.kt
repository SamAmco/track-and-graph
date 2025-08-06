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

import android.content.Context
import android.content.Intent
import android.os.Build
import com.samco.trackandgraph.navigation.PendingIntentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface TimerServiceInteractor {
    fun startTimerNotificationService()
    fun requestWidgetUpdatesForFeatureId(featureId: Long)
    fun requestWidgetsDisabledForFeatureId(featureId: Long)
}

internal class TimerServiceInteractorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingIntentProvider: PendingIntentProvider,
) : TimerServiceInteractor {

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