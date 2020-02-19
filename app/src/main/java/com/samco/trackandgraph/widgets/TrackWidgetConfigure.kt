package com.samco.trackandgraph.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.FeatureAndTrackGroup
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.databinding.TrackWidgetConfigureBinding
import timber.log.Timber
import java.lang.Exception

class TrackWidgetConfigure : FragmentActivity() {

    private var appWidgetId: Int? = null
    private lateinit var viewModel: TrackWidgetConfigureViewModel
    private lateinit var binding: TrackWidgetConfigureBinding
    private var featureId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.track_widget_configure)

        binding = TrackWidgetConfigureBinding.inflate(layoutInflater)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID


        setResult(RESULT_CANCELED)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        viewModel = ViewModelProviders.of(this).get(TrackWidgetConfigureViewModel::class.java)
        viewModel.initViewModel(this)
        viewModel.allFeatures.observe(this, Observer { features ->
            val itemNames = features.map {ft -> "${ft.trackGroupName} -> ${ft.name}"}
            Timber.d(itemNames.toString())
            val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
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
                    featureId = features[position].id
                }
            }
        })

        return null
    }

    fun onConfirm(view: View) {
        appWidgetId?.let { id ->
            if (appWidgetId == null) {
                return
            }

            val featureId: Long = 2 // temp
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
}

class TrackWidgetConfigureViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
    }
}