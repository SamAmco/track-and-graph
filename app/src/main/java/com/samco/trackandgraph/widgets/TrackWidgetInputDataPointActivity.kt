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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

class TrackWidgetInputDataPointActivity : AppCompatActivity() {
    private val viewModel by viewModels<TrackWidgetInputDataPointViewModel>()

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

            viewModel.init(this.application, featureId)
            observeFeature()
        } ?: finish()
    }

    private fun observeFeature() = viewModel.feature.observe(this, Observer { feature ->
        if (feature == null) finish()
        else when (feature.hasDefaultValue) {
            true -> {
                viewModel.addDefaultDataPoint()
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
    private var dataSource: TrackAndGraphDatabaseDao? = null
    lateinit var feature: LiveData<Feature?> private set

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    fun init(application: Application, featureId: Long) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        feature = dataSource!!.tryGetFeatureById(featureId)
    }

    fun addDefaultDataPoint() = feature.value?.let {
        ioScope.launch {
            val label =
                if (it.featureType == FeatureType.DISCRETE)
                    it.discreteValues.first { dv -> dv.index == it.defaultValue.toInt() }.label
                else ""
            val newDataPoint = DataPoint(OffsetDateTime.now(), it.id, it.defaultValue, label, "")
            dataSource!!.insertDataPoint(newDataPoint)
        }
    }
}