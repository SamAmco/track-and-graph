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

package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.navigation.PendingIntentProvider
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.widgets.TrackWidgetProvider.Companion.WIDGET_PREFS_NAME
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

//I did try converting this to a Worker but i was blocked by this issue:
// https://stackoverflow.com/questions/70654474/starting-workmanager-task-from-appwidgetprovider-results-in-endless-onupdate-cal
// So I am keeping this as a JobIntentService for now at least until this issue is fixed
@AndroidEntryPoint
class TrackWidgetJobIntentService : JobIntentService() {

    @Inject
    @IODispatcher
    lateinit var io: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var ui: CoroutineDispatcher

    @Inject
    lateinit var dataInteractor: DataInteractor

    @Inject
    lateinit var pendingIntentProvider: PendingIntentProvider

    @Inject
    lateinit var timerServiceInteractor: TimerServiceInteractor

    companion object {
        const val UPDATE_FEATURE_TIMER_EXTRA = "UPDATE_FEATURE_TIMER_EXTRA"
        const val APP_WIDGET_ID_EXTRA = "appWidgetIdExtra"
        const val DISABLE_WIDGET_EXTRA = "disableWidgetExtra"

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, TrackWidgetJobIntentService::class.java, 0, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val extras = intent.extras ?: return
        val appWidgetId = extras.getInt(APP_WIDGET_ID_EXTRA, -1)
        val featureId = applicationContext
            .getSharedPreferences(WIDGET_PREFS_NAME, MODE_PRIVATE)
            .getLong(TrackWidgetProvider.getFeatureIdPref(appWidgetId), -1)
        if (extras.getBoolean(DISABLE_WIDGET_EXTRA, false)) {
            forceDisableWidget(appWidgetId)
        } else if (extras.containsKey(UPDATE_FEATURE_TIMER_EXTRA)) {
            updateTimer(featureId, extras.getBoolean(UPDATE_FEATURE_TIMER_EXTRA, false))
        } else return updateWidgetView(appWidgetId, featureId)
    }

    private fun updateTimer(featureId: Long, startTimer: Boolean) = runBlocking {
        val tracker = dataInteractor.tryGetDisplayTrackerByFeatureIdSync(featureId) ?: return@runBlocking
        if (startTimer) {
            dataInteractor.playTimerForTracker(tracker.id)
            timerServiceInteractor.startTimerNotificationService()
            timerServiceInteractor.requestWidgetUpdatesForFeatureId(tracker.featureId)
        }
        else {
            //TODO might be worth writing a simpler function for this as tryGetDisplayTrackerByIdSync
            // counts all data points in the tracker. Seems a bit excessive when all we need is timerStartInstant
            tracker.timerStartInstant?.let {
                dataInteractor.stopTimerForTracker(tracker.id)
                timerServiceInteractor.requestWidgetUpdatesForFeatureId(tracker.featureId)
                val intent = pendingIntentProvider
                    .getDurationInputActivityIntent(tracker.id, it.toString())
                applicationContext.startActivity(intent)
            }
        }
    }

    private fun forceDisableWidget(appWidgetId: Int) = runBlocking {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val remoteViews = RemoteViews(applicationContext.packageName, R.layout.track_widget)
        remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.play_button, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.stop_button, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.track_widget_icon, View.INVISIBLE)
        withContext(ui) { appWidgetManager.updateAppWidget(appWidgetId, remoteViews) }
    }

    private fun updateWidgetView(appWidgetId: Int, featureId: Long) =
        runBlocking {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            val tracker = withContext(io) {
                dataInteractor.tryGetDisplayTrackerByFeatureIdSync(featureId)
            }

            if (tracker == null) {
                forceDisableWidget(appWidgetId)
                return@runBlocking
            }

            val title = tracker.name
            val requireInput = !tracker.hasDefaultValue
            val remoteViews = createRemoteViews(
                context = applicationContext,
                appWidgetId = appWidgetId,
                featureId = featureId,
                title = title,
                requireInput = requireInput,
                isDuration = tracker.dataType == DataType.DURATION,
                timerRunning = tracker.timerStartInstant != null
            )
            withContext(ui) { appWidgetManager.updateAppWidget(appWidgetId, remoteViews) }
        }

    /**
     * Construct the RemoteViews for a widget.
     */
    private fun createRemoteViews(
        context: Context,
        appWidgetId: Int,
        featureId: Long,
        title: String?,
        requireInput: Boolean? = true,
        isDuration: Boolean = false,
        timerRunning: Boolean = false
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)

        setTrackButtonPendingIntent(
            remoteViews,
            pendingIntentProvider.getTrackWidgetInputDataPointActivityPendingIntent(appWidgetId)
        )
        setUpPlayStopButtons(appWidgetId, featureId, remoteViews, isDuration, timerRunning)

        title?.let { remoteViews.setTextViewText(R.id.track_widget_title, it) }

        setWidgetTrackButtonDrawable(requireInput, remoteViews)

        return remoteViews
    }

    private fun setUpPlayStopButtons(
        appWidgetId: Int,
        featureId: Long,
        remoteViews: RemoteViews,
        isDuration: Boolean,
        timerRunning: Boolean
    ) {
        if (!isDuration) {
            remoteViews.setViewVisibility(R.id.play_button, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.stop_button, View.INVISIBLE)
        } else if (timerRunning) {
            remoteViews.setViewVisibility(R.id.play_button, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.stop_button, View.VISIBLE)
            remoteViews.setOnClickPendingIntent(
                R.id.stop_button,
                pendingIntentProvider.getTrackWidgetStartStopTimerIntent(
                    appWidgetId,
                    featureId,
                    false
                )
            )
        } else {
            remoteViews.setViewVisibility(R.id.play_button, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.stop_button, View.INVISIBLE)
            remoteViews.setOnClickPendingIntent(
                R.id.play_button,
                pendingIntentProvider.getTrackWidgetStartStopTimerIntent(
                    appWidgetId,
                    featureId,
                    true
                )
            )
        }
    }

    private fun setTrackButtonPendingIntent(
        remoteViews: RemoteViews,
        pendingIntent: PendingIntent
    ) {
        remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        //remoteViews.setOnClickPendingIntent(R.id.track_widget_icon, pendingIntent)
        //remoteViews.setOnClickPendingIntent(R.id.track_widget_icon_default, pendingIntent)
    }


    /**
     * Set the appropriate drawable for the widget according to the status of the widget.
     */
    private fun setWidgetTrackButtonDrawable(
        requireInput: Boolean?,
        remoteViews: RemoteViews
    ) {
        if (requireInput == true) {
            remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.track_widget_icon, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.track_widget_icon, View.INVISIBLE)
        }
    }

}