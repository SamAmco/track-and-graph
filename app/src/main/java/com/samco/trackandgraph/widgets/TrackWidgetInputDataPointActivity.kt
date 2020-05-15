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

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY
import org.threeten.bp.OffsetDateTime

class TrackWidgetInputDataPointActivity : FragmentActivity() {
    private lateinit var viewModel: TrackWidgetInputDataPointViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras

        bundle?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)?.let { widgetId ->
            val sharedPref = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val featureId = sharedPref.getLong(TrackWidgetProvider.getFeatureIdPref(widgetId), -1)
            if (featureId == -1L) {
                finish()
                return
            }

            viewModel = ViewModelProviders.of(this)
                .get(TrackWidgetInputDataPointViewModel::class.java)
            viewModel.init(this.application, featureId)
            observeFeature()
        } ?: finish()
    }

    private fun observeFeature() = viewModel.feature.observe(this, Observer { feature ->
        if (feature == null) finish()
        else when (feature.featureType) {
            FeatureType.TIMESTAMP -> {
                viewModel.addDataPoint(feature.id)
                finish()
            }
            else -> showDialog(feature.id)
        }
    })

    private fun showDialog(featureId: Long) {
        val dialog = TrackWidgetInputDataPointDialog()
        val args = Bundle()
        args.putLongArray(FEATURE_LIST_KEY, longArrayOf(featureId))
        dialog.arguments = args
        supportFragmentManager.let {
            dialog.show(it, "input_data_points_dialog")
        }
    }
}

class TrackWidgetInputDataPointViewModel : ViewModel() {
    private lateinit var dao: TrackAndGraphDatabaseDao
    lateinit var feature: LiveData<Feature?> private set

    fun init(application: Application, featureId: Long) {
        dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        feature = dao.tryGetFeatureById(featureId)
    }

    fun addDataPoint(featureId: Long) {
        val newDataPoint = DataPoint(OffsetDateTime.now(), featureId, 1.0, "")
        dao.insertDataPoint(newDataPoint)
    }
}