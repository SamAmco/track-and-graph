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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.base.navigation.PendingIntentProvider
import com.samco.trackandgraph.base.service.TrackWidgetProvider.Companion.WIDGET_PREFS_NAME
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


    companion object {
        const val UPDATE_FEATURE_TIMER_EXTRA = "UPDATE_FEATURE_TIMER_EXTRA"
        const val APP_WIDGET_ID_EXTRA = "appWidgetIdExtra"
        const val DISABLE_WIDGET_EXTRA = "disableWidgetExtra"

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, TrackWidgetJobIntentService::class.java, 0, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        println("samsam: doWork called: $intent")
        val extras = intent.extras ?: return
        val appWidgetId = extras.getInt(APP_WIDGET_ID_EXTRA, -1) ?: return
        val featureId = applicationContext
            .getSharedPreferences(WIDGET_PREFS_NAME, MODE_PRIVATE)
            .getLong(TrackWidgetProvider.getFeatureIdPref(appWidgetId), -1)
        val disableWidget = extras.getBoolean(DISABLE_WIDGET_EXTRA, false)
        if (extras.containsKey(UPDATE_FEATURE_TIMER_EXTRA) ?: false) {
            updateTimer(featureId, extras.getBoolean(UPDATE_FEATURE_TIMER_EXTRA, false))
        }
        return updateWidgetView(appWidgetId, featureId, disableWidget).also {
            println("samsam: returning $it")
        }
    }

    private fun updateTimer(featureId: Long, startTimer: Boolean) = runBlocking {
        if (startTimer) dataInteractor.playTimerForFeature(featureId)
        else {
            dataInteractor.tryGetDisplayFeatureByIdSync(featureId)?.timerStartInstant?.let {
                dataInteractor.stopTimerForFeature(featureId)
                val intent = pendingIntentProvider
                    .getDurationInputActivityIntent(featureId, it.toString())
                applicationContext.startActivity(intent)
            }
        }
    }

    private fun updateWidgetView(appWidgetId: Int, featureId: Long, disableWidget: Boolean) =
        runBlocking {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            val feature = withContext(io) {
                dataInteractor.tryGetDisplayFeatureByIdSync(featureId)
            } ?: return@runBlocking

            val title = feature.name
            val requireInput = feature.hasDefaultValue
            val remoteViews = createRemoteViews(
                applicationContext,
                appWidgetId,
                featureId,
                title,
                requireInput,
                disableWidget,
                feature.featureType == DataType.DURATION,
                feature.timerStartInstant != null
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
        disable: Boolean = false,
        isDuration: Boolean = false,
        timerRunning: Boolean = false
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)

        if (disable) {
            remoteViews.setTextViewText(R.id.track_widget_title, "")
            setTrackButtonPendingIntent(
                remoteViews,
                PendingIntent.getActivity(context, appWidgetId, Intent(), 0)
            )
            setUpPlayStopButtons(appWidgetId, featureId, remoteViews, false, false)
        } else {
            setTrackButtonPendingIntent(
                remoteViews,
                pendingIntentProvider.getTrackWidgetInputDataPointActivityPendingIntent(appWidgetId)
            )
            setUpPlayStopButtons(appWidgetId, featureId, remoteViews, isDuration, timerRunning)

            title?.let { remoteViews.setTextViewText(R.id.track_widget_title, it) }
        }

        setWidgetTrackButtonDrawable(disable, requireInput, remoteViews)

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
        disable: Boolean,
        requireInput: Boolean?,
        remoteViews: RemoteViews
    ) {
        when {
            disable -> {
                remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.INVISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon, View.INVISIBLE)
            }
            requireInput == false -> {
                remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.INVISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon, View.INVISIBLE)
            }
            else -> {
                remoteViews.setViewVisibility(R.id.track_widget_icon_warning, View.INVISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon_default, View.INVISIBLE)
                remoteViews.setViewVisibility(R.id.track_widget_icon, View.VISIBLE)
            }
        }
    }

}