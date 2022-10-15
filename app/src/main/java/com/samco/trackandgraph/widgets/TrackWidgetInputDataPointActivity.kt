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
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.adddatapoint.TRACKER_LIST_KEY
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.base.service.TrackWidgetProvider
import com.samco.trackandgraph.base.service.TrackWidgetProvider.Companion.WIDGET_PREFS_NAME
import com.samco.trackandgraph.util.hideKeyboard
import com.samco.trackandgraph.util.performTrackVibrate
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@AndroidEntryPoint
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

            viewModel.initFromFeatureId(featureId)
            observeFeature()
        } ?: finish()
    }

    private fun observeFeature() = viewModel.tracker.observe(this) { tracker ->
        if (tracker == null) finish()
        else when (tracker.hasDefaultValue) {
            true -> {
                viewModel.addDefaultDataPoint()
                performTrackVibrate()
                finish()
            }
            else -> showDialog(tracker.id)
        }
    }

    private fun showDialog(trackerId: Long) {
        val dialog = TrackWidgetDataPointInputDialog()
        val args = Bundle()
        args.putLongArray(TRACKER_LIST_KEY, longArrayOf(trackerId))
        dialog.arguments = args
        dialog.show(supportFragmentManager, "input_data_points_dialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        window.hideKeyboard()
    }
}

@HiltViewModel
class TrackWidgetInputDataPointViewModel @Inject constructor(
    private var dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _tracker = MutableLiveData<Tracker?>()
    val tracker: LiveData<Tracker?> get() = _tracker

    private var initialized = false

    fun initFromFeatureId(featureId: Long) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(io) {
            val tracker = dataInteractor.getTrackerByFeatureId(featureId)
            withContext(ui) { _tracker.value = tracker }
        }
    }

    fun addDefaultDataPoint() = tracker.value?.let {
        viewModelScope.launch(io) {
            val newDataPoint = DataPoint(
                timestamp = OffsetDateTime.now(),
                featureId = it.featureId,
                value = it.defaultValue,
                label = it.defaultLabel,
                note = ""
            )
            dataInteractor.insertDataPoint(newDataPoint)
        }
    }
}