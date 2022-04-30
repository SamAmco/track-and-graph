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
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase

class TrackWidgetJobIntentService : JobIntentService() {
    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, TrackWidgetJobIntentService::class.java, 0, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        val appWidgetId = intent.getIntExtra(APP_WIDGET_ID_EXTRA, -1)
        val disableWidget = intent.getBooleanExtra(DISABLE_WIDGET_EXTRA, false)

        val featureId = this.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).getLong(
            TrackWidgetProvider.getFeatureIdPref(appWidgetId), -1
        )

        val dao = TrackAndGraphDatabase.getInstance(this).trackAndGraphDatabaseDao

        val feature = dao.tryGetFeatureByIdSync(featureId)
        val title = feature?.name
        val requireInput = !(feature?.hasDefaultValue ?: false)
        val remoteViews = TrackWidgetProvider.createRemoteViews(
            this,
            appWidgetId,
            title,
            requireInput,
            disableWidget
        )
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }
}