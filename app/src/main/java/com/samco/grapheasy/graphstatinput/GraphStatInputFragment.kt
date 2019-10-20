package com.samco.grapheasy.graphstatinput

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*

import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import com.samco.grapheasy.databinding.FragmentGraphStatInputBinding
import org.threeten.bp.Period

class GraphStatInputFragment : Fragment() {
    private lateinit var binding: FragmentGraphStatInputBinding
    private lateinit var viewModel: GraphStatInputViewModel

    private val colorList = listOf(
        R.color.visColor1,
        R.color.visColor2,
        R.color.visColor3,
        R.color.visColor4,
        R.color.visColor5,
        R.color.visColor6,
        R.color.visColor7,
        R.color.visColor8,
        R.color.visColor9,
        R.color.visColor10
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_graph_stat_input, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProviders.of(this).get(GraphStatInputViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        listenToGraphTypeSpinner()
        listenToMovingAveragePeriod()
        listenToTimePeriod()
        listenToAllFeatures()

        return binding.root
    }

    private fun listenToAllFeatures() {
        viewModel.allFeatures.observe(this, Observer {
            initPieChartAdapter(it)
            listenToAddLineGraphFeatureButton(it)
            createLineGraphFeatureViews(it)
            binding.progressBar.visibility = View.GONE
        })
    }

    private fun listenToAddLineGraphFeatureButton(features: List<FeatureAndTrackGroup>) {
        binding.addFeatureButton.isClickable = true
        binding.addFeatureButton.setOnClickListener {
            val color = colorList[(colorList.size - 1).coerceAtMost(viewModel.lineGraphFeatures.size)]
            val newLineGraphFeature = LineGraphFeature(-1, color, 0f, 1f)
            viewModel.lineGraphFeatures.add(newLineGraphFeature)
            inflateLineGraphFeatureView(newLineGraphFeature, features)
        }
    }

    private fun createLineGraphFeatureViews(features: List<FeatureAndTrackGroup>) {
        viewModel.lineGraphFeatures.forEach { lgf -> inflateLineGraphFeatureView(lgf, features) }
    }

    private fun inflateLineGraphFeatureView(lgf: LineGraphFeature, features: List<FeatureAndTrackGroup>) {
        val view = GraphFeatureListItemView(context!!, features, colorList, lgf)
        view.setOnRemoveListener {
            viewModel.lineGraphFeatures.remove(lgf)
            binding.lineGraphFeaturesLayout.removeView(view)
        }
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = params
        binding.lineGraphFeaturesLayout.addView(view)
    }

    private fun initPieChartAdapter(features: List<FeatureAndTrackGroup>) {
        val itemNames = features.map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.pieChartFeatureSpinner.adapter = adapter
        val selected = viewModel.selectedPieChartFeature.value
        if (selected != null) binding.pieChartFeatureSpinner.setSelection(features.indexOf(selected))
        binding.pieChartFeatureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.selectedPieChartFeature.value = features[index]
            }
        }
    }

    private fun listenToTimePeriod() {
        val timePeriods = listOf(
            null,
            Period.ofDays(1),
            Period.ofWeeks(1),
            Period.ofMonths(1),
            Period.ofMonths(3),
            Period.ofMonths(6),
            Period.ofYears(1)
        )
        binding.samplePeriodSpinner.setSelection(timePeriods.indexOf(viewModel.samplePeriod.value))
        binding.samplePeriodSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.samplePeriod.value = timePeriods[index]
            }
        }
    }

    private fun listenToMovingAveragePeriod() {
        val timePeriods = listOf(
            null,
            Period.ofDays(1),
            Period.ofDays(3),
            Period.ofWeeks(1),
            Period.ofMonths(1),
            Period.ofMonths(3),
            Period.ofMonths(6),
            Period.ofYears(1)
        )
        binding.movingAverageSpinner.setSelection(timePeriods.indexOf(viewModel.movingAveragePeriod.value))
        binding.movingAverageSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.movingAveragePeriod.value = timePeriods[index]
            }
        }
    }

    private fun listenToGraphTypeSpinner() {
        val graphTypes = listOf(
            GraphStatType.LINE_GRAPH,
            GraphStatType.PIE_CHART,
            GraphStatType.AVERAGE_TIME_BETWEEN,
            GraphStatType.TIME_SINCE
        )
        binding.graphTypeSpinner.setSelection(graphTypes.indexOf(viewModel.graphStatType.value))
        binding.graphTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.graphStatType.value = graphTypes[index]
            }
        }
        viewModel.graphStatType.observe(this, Observer { graphType ->
            when(graphType) {
                GraphStatType.LINE_GRAPH -> {
                    binding.movingAverageLayout.visibility = View.VISIBLE
                    binding.timePeriodLayout.visibility = View.VISIBLE
                    binding.pieChartSelectFeatureLayout.visibility = View.GONE
                    binding.addFeatureButton.visibility = View.VISIBLE
                    binding.lineGraphFeaturesLayout.visibility = View.VISIBLE
                }
                GraphStatType.PIE_CHART -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.VISIBLE
                    binding.pieChartSelectFeatureLayout.visibility = View.VISIBLE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                }
                GraphStatType.AVERAGE_TIME_BETWEEN -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.VISIBLE
                    binding.pieChartSelectFeatureLayout.visibility = View.GONE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                }
                GraphStatType.TIME_SINCE -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.GONE
                    binding.pieChartSelectFeatureLayout.visibility = View.GONE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                }
            }
        })
    }

}


class GraphStatInputViewModel : ViewModel() {
    private var dataSource: GraphEasyDatabaseDao? = null

    val graphStatType = MutableLiveData<GraphStatType>(GraphStatType.LINE_GRAPH)
    val movingAveragePeriod = MutableLiveData<Period?>(null)
    val samplePeriod = MutableLiveData<Period?>(null)
    val selectedPieChartFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val lineGraphFeatures = mutableListOf<LineGraphFeature>()

    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
    }
}