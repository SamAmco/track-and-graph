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

package com.samco.trackandgraph.base.navigation

import android.app.PendingIntent
import android.content.Intent

interface PendingIntentProvider {
    /**
     * Get a pending intent that will start the main activity of the app
     */
    fun getMainActivityPendingIntent(clearTask: Boolean = true): PendingIntent

    /**
     * Get an intent to start the duration input activity directly
     */
    fun getDurationInputActivityIntent(trackerId: Long, startInstant: String): Intent

    /**
     * Get a pending intent that will start the duration input activity
     * (used when a timer is stopped via a notification action)
     */
    fun getDurationInputActivityPendingIntent(trackerId: Long, startInstant: String): PendingIntent

    /**
     * Get a pending intent that will start the track widget input data point activity for a given
     * widget id.
     */
    fun getTrackWidgetInputDataPointActivityPendingIntent(appWidgetId: Int): PendingIntent

    /**
     * Get an intent to broadcast to the TrackWidgetProvider that a given feature has been delete
     */
    fun getTrackWidgetDisableForFeatureByIdIntent(featureId: Long): Intent

    /**
     * Get an intent to broadcast to the TrackWidgetProvider that a given feature has been updated
     * and therefore any widgets referencing that feature need to be updated also
     */
    fun getTrackWidgetUpdateForFeatureIdIntent(featureId: Long): Intent

    /**
     * Get a pending intent that will broadcast to the TrackWidgetProvider (a broadcast receiver)
     * that it should start/stop the timer for a given duration based feature and then update
     * all widgets referencing that feature.
     */
    fun getTrackWidgetStartStopTimerIntent(
        appWidgetId: Int,
        featureId: Long,
        startTimer: Boolean
    ): PendingIntent
}
