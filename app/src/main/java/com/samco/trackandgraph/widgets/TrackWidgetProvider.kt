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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.R

const val WIDGET_PREFS_NAME = "TrackWidget"
const val DELETE_FEATURE_ID = "DELETE_FEATURE_ID"
const val UPDATE_FEATURE_ID = "UPDATE_FEATURE_ID"
const val APP_WIDGET_ID_EXTRA = "appWidgetIdExtra"
const val DISABLE_WIDGET_EXTRA = "disableWidgetExtra"

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
        /**
         * Return key to get the feature id for the widget id from shared preferences.
         */
        fun getFeatureIdPref(widgetId: Int): String {
            return "widget_feature_id_$widgetId"
        }

        /**
         * Construct the RemoteViews for a widget.
         */
        fun createRemoteViews(
            context: Context, appWidgetId: Int,
            title: String?, requireInput: Boolean? = true,
            disable: Boolean = false
        ): RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)

            if (disable) {
                remoteViews.setTextViewText(R.id.track_widget_title, "")
                remoteViews.setOnClickPendingIntent(
                    R.id.widget_container,
                    PendingIntent.getActivity(context, appWidgetId, Intent(), 0)
                )
            } else {
                remoteViews.setOnClickPendingIntent(
                    R.id.widget_container,
                    getOnClickPendingIntent(context, appWidgetId)
                )

                title?.let {
                    remoteViews.setTextViewText(R.id.track_widget_title, it)
                }
            }

            setWidgetDrawable(disable, requireInput, remoteViews)

            return remoteViews
        }

        /**
         * Return the PendingIntent for an app widget that starts the feature input dialog.
         */
        private fun getOnClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
            Intent(context, TrackWidgetInputDataPointActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }.let {
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        /**
         * Set the appropriate drawable for the widget according to the status of the widget.
         * Pre-Lollipop, construct a bitmap for the drawable.
         */
        private fun setWidgetDrawable(
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

        /**
         * Update all widgets for the given feature id.
         */
        fun updateWidgetsWithFeatureId(context: Context, featureId: Long) {
            getWidgetIdsForFeatureId(context, featureId).forEach { id ->
                val workIntent = Intent(context, TrackWidgetJobIntentService::class.java)
                workIntent.putExtra("appWidgetIdExtra", id)
                TrackWidgetJobIntentService.enqueueWork(context, workIntent)
            }
        }

        /**
         * Delete all widgets for the given feature id.
         */
        fun deleteWidgetsWithFeatureId(context: Context, featureId: Long) {
            getWidgetIdsForFeatureId(context, featureId).forEach { id ->
                val workIntent = Intent(context, TrackWidgetJobIntentService::class.java)
                workIntent.putExtra(APP_WIDGET_ID_EXTRA, id)
                workIntent.putExtra(DISABLE_WIDGET_EXTRA, true)
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds?.forEach { id ->
            // Fire a background job to load the feature name.
            val workIntent = Intent(context, TrackWidgetJobIntentService::class.java)
            workIntent.putExtra(APP_WIDGET_ID_EXTRA, id)
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
                    updateWidgetsWithFeatureId(context, extras.getLong(UPDATE_FEATURE_ID))
                }
                extras.containsKey(DELETE_FEATURE_ID) -> {
                    deleteWidgetsWithFeatureId(context, extras.getLong(DELETE_FEATURE_ID))
                }
                else -> super.onReceive(context, intent)
            }
        } ?: super.onReceive(context, intent)
    }
}