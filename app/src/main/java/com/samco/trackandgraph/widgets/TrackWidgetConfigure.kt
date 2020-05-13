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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.FeatureAndTrackGroup
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.databinding.TrackWidgetConfigureBinding
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class TrackWidgetConfigure : AppCompatActivity() {

    private var appWidgetId: Int? = null
    private lateinit var viewModel: TrackWidgetConfigureViewModel
    private lateinit var binding: TrackWidgetConfigureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = TrackWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(TrackWidgetConfigureViewModel::class.java)
        viewModel.initViewModel(this)
        viewModel.allFeatures.observe(this, Observer { features ->
            if (features.isEmpty()) {
                Toast.makeText(applicationContext, "Create a data set first!", Toast.LENGTH_SHORT).show()
                finish()
            }
            val itemNames = features.map {ft -> "${ft.trackGroupName} -> ${ft.name}"}
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemNames)
            binding.featureSpinner.adapter = adapter
            binding.featureSpinner.setSelection(0)
            binding.featureSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.featureId = features[position].id
                }
            }
        })
    }

    fun onConfirm(view: View) {
        appWidgetId?.let { id ->
            val featureId = viewModel.featureId
            if (featureId == null) {
                Toast.makeText(applicationContext, "Select a data set", Toast.LENGTH_SHORT).show()
                return
            }

            val sharedPref = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit()
            sharedPref.putLong(TrackWidgetProvider.getFeatureIdPref(id), featureId)
            sharedPref.apply()

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, TrackWidgetProvider::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
            sendBroadcast(intent)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    fun onCancel(view: View) {
        finish()
    }
}

class TrackWidgetConfigureViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set
    var featureId: Long? = null

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
    }
}