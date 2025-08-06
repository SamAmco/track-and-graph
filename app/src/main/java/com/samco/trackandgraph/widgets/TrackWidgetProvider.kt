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
package com.samco.trackandgraph.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.samco.trackandgraph.widgets.TrackWidgetJobIntentService.Companion.APP_WIDGET_ID_EXTRA
import com.samco.trackandgraph.widgets.TrackWidgetJobIntentService.Companion.DISABLE_WIDGET_EXTRA
import com.samco.trackandgraph.widgets.TrackWidgetJobIntentService.Companion.UPDATE_FEATURE_TIMER_EXTRA

/**
 * Class for managing the track widgets.
 *
 * Besides normally updating widgets by widget id, this class can also be used to update or delete
 * widgets of a particular track feature.
 * In these cases, send a broadcast with a field in the extras of the intent.
 * Updating of the UI is done in the background in WidgetJobIntentService.
 */
class TrackWidgetProvider : AppWidgetProvider() {
    companion object {
        const val WIDGET_PREFS_NAME = "TrackWidget"
        const val DELETE_FEATURE_ID = "DELETE_FEATURE_ID"
        const val UPDATE_FEATURE_ID = "UPDATE_FEATURE_ID"
        const val UPDATE_FEATURE_TIMER = "UPDATE_FEATURE_TIMER"

        /**
         * Return key to get the feature id for the widget id from shared preferences.
         */
        fun getFeatureIdPref(widgetId: Int): String {
            return "widget_feature_id_$widgetId"
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds?.forEach { id ->
            val workIntent = Intent(context, TrackWidgetJobIntentService::class.java).apply {
                putExtra(APP_WIDGET_ID_EXTRA, id)
            }
            TrackWidgetJobIntentService.enqueueWork(context, workIntent)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach { id ->
            context?.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
                remove(getFeatureIdPref(id))
                apply()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.let { extras ->
            when {
                extras.containsKey(UPDATE_FEATURE_ID) -> {
                    val id = extras.getLong(UPDATE_FEATURE_ID)
                    val updateTimer =
                        if (!extras.containsKey(UPDATE_FEATURE_TIMER)) null
                        else extras.getBoolean(UPDATE_FEATURE_TIMER)
                    updateWidgetsWithFeatureId(context, id, updateTimer)
                }
                extras.containsKey(DELETE_FEATURE_ID) -> {
                    deleteWidgetsWithFeatureId(context, extras.getLong(DELETE_FEATURE_ID))
                }
                else -> {
                    super.onReceive(context, intent)
                }
            }
        } ?: super.onReceive(context, intent)
    }

    /**
     * Update all widgets for the given feature id. and optionally put an extra to tell the job to
     * start or stop the timer before updating the view.
     */
    private fun updateWidgetsWithFeatureId(
        context: Context,
        featureId: Long,
        timerRunning: Boolean?
    ) {
        getWidgetIdsForFeatureId(context, featureId).forEach { id ->
            val workIntent = Intent(context, TrackWidgetJobIntentService::class.java).apply {
                putExtra(APP_WIDGET_ID_EXTRA, id)
                if (timerRunning != null) putExtra(UPDATE_FEATURE_TIMER_EXTRA, timerRunning)
            }
            TrackWidgetJobIntentService.enqueueWork(context, workIntent)
        }
    }

    /**
     * Delete all widgets for the given feature id.
     */
    private fun deleteWidgetsWithFeatureId(context: Context, featureId: Long) {
        getWidgetIdsForFeatureId(context, featureId).forEach { id ->
            val workIntent = Intent(context, TrackWidgetJobIntentService::class.java).apply {
                putExtra(APP_WIDGET_ID_EXTRA, id)
                putExtra(DISABLE_WIDGET_EXTRA, true)
            }
            TrackWidgetJobIntentService.enqueueWork(context, workIntent)
        }
    }

    /**
     * Return a list of all widget ids for the given feature id.
     */
    private fun getWidgetIdsForFeatureId(context: Context, featureId: Long): List<Int> {
        return context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .let { sharedPrefs ->
                AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, TrackWidgetProvider::class.java))
                    .let { ids ->
                        ids.filter {
                            sharedPrefs.getLong(getFeatureIdPref(it), -1) == featureId
                        }
                    }
            }
    }
}