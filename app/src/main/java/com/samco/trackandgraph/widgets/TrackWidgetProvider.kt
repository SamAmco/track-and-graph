/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

// This package has to be base service because that's where the original
// TrackWidgetProvider was located. Any user widgets will be deleted/broken
// after updating the app if FQCN (Full Qualified Class Name) of TrackWidgetProvider
// changes.
@file:Suppress("PackageDirectoryMismatch")
package com.samco.trackandgraph.base.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.widgets.TrackWidgetGlance
import com.samco.trackandgraph.widgets.TrackWidgetState.DELETE_FEATURE_ID
import com.samco.trackandgraph.widgets.TrackWidgetState.UPDATE_FEATURE_ID
import com.samco.trackandgraph.widgets.TrackWidgetState.WIDGET_PREFS_NAME
import com.samco.trackandgraph.widgets.TrackWidgetState.getFeatureIdPref
import com.samco.trackandgraph.widgets.TrackWidgetState.updateFromTracker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TrackWidgetProvider : GlanceAppWidgetReceiver() {

    @Inject
    lateinit var dataInteractor: DataInteractor

    @Inject
    lateinit var timerServiceInteractor: TimerServiceInteractor

    override val glanceAppWidget: GlanceAppWidget = TrackWidgetGlance()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        intent.extras?.let { extras ->
            when {
                extras.containsKey(UPDATE_FEATURE_ID) -> {
                    updateWidgetsForFeature(context, extras.getLong(UPDATE_FEATURE_ID))
                }

                extras.containsKey(DELETE_FEATURE_ID) -> {
                    onDeleted(context, intArrayOf(extras.getInt(DELETE_FEATURE_ID)))
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Update all widgets with fresh data following ChatGPT pattern
        runBlocking {
            val glanceManager = GlanceAppWidgetManager(context)
            appWidgetIds.forEach { appWidgetId ->
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                updateWidgetState(context, glanceId, appWidgetId)
            }
            glanceAppWidget.updateAll(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        //Clean up widget preferences
        appWidgetIds.forEach { id ->
            context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                remove(getFeatureIdPref(id))
                apply()
            }
        }

        runBlocking { glanceAppWidget.updateAll(context) }
    }

    private suspend fun updateWidgetState(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
    ) {
        val featureId = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(getFeatureIdPref(appWidgetId), -1L)
        updateWidgetState(
            context = context,
            glanceId = glanceId,
            featureId = featureId,
        )
    }

    /**
     * Pre-compute widget state and pack into individual preference keys
     */
    private suspend fun updateWidgetState(
        context: Context,
        glanceId: GlanceId,
        featureId: Long,
    ) {
        val tracker = if (featureId != -1L) {
            try {
                dataInteractor.tryGetDisplayTrackerByFeatureIdSync(featureId)
            } catch (e: Exception) {
                null
            }
        } else null

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.updateFromTracker(featureId, tracker)
        }
    }

    private fun updateWidgetsForFeature(context: Context, featureId: Long) {
        runBlocking {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TrackWidgetProvider::class.java)
            )
            val glanceManager = GlanceAppWidgetManager(context)

            appWidgetIds.forEach { appWidgetId ->
                val widgetFeatureId = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
                    .getLong(getFeatureIdPref(appWidgetId), -1L)
                if (widgetFeatureId == featureId) {
                    val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                    updateWidgetState(context, glanceId, featureId)
                }
            }
            glanceAppWidget.updateAll(context)
        }
    }
}

/**
 * ActionCallback for starting a timer from the widget
 */
class StartTimerAction : ActionCallback {
    private lateinit var dataInteractor: DataInteractor
    private lateinit var timerServiceInteractor: TimerServiceInteractor

    /**
     * Entry point to expose dependencies from the Singleton graph
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetDependencies {
        fun dataInteractor(): DataInteractor
        fun timerServiceInteractor(): TimerServiceInteractor
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val dependencies = EntryPointAccessors.fromApplication(
                context = context.applicationContext,
                entryPoint = WidgetDependencies::class.java
            )
            dataInteractor = dependencies.dataInteractor()
            timerServiceInteractor = dependencies.timerServiceInteractor()

            val featureId = parameters[FeatureIdKey] ?: return

            val tracker = dataInteractor.tryGetDisplayTrackerByFeatureIdSync(featureId) ?: return
            dataInteractor.playTimerForTracker(tracker.id)
            timerServiceInteractor.startTimerNotificationService()

            // Update widget state with fresh data
            val updatedTracker = dataInteractor.tryGetDisplayTrackerByFeatureIdSync(featureId)
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.updateFromTracker(featureId, updatedTracker)
            }
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            TrackWidgetGlance().update(context, glanceId)
        }
    }

    companion object {
        val FeatureIdKey = ActionParameters.Key<Long>("featureId")
    }
}

