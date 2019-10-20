package com.samco.grapheasy.graphstatinput

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*

import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import com.samco.grapheasy.databinding.FragmentGraphStatInputBinding
import org.threeten.bp.Period
import java.text.DecimalFormat

class GraphStatInputFragment : Fragment() {
    private lateinit var binding: FragmentGraphStatInputBinding
    private lateinit var viewModel: GraphStatInputViewModel

    private val decimalFormat = DecimalFormat("0.###############")

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
            listentToValueStat(it)
            binding.progressBar.visibility = View.GONE
        })
    }

    private fun listentToValueStat(features: List<FeatureAndTrackGroup>) {
        val itemNames = features.map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.valueStatFeatureSpinner.adapter = adapter
        val selected = viewModel.selectedValueStatFeature.value
        if (selected != null) binding.valueStatFeatureSpinner.setSelection(features.indexOf(selected))
        binding.valueStatFeatureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.selectedValueStatFeature.value = features[index]
            }
        }
        listenToValueStatFeature()
    }

    private fun listenToValueStatFeature() {
        binding.valueStatDiscreteValueInputLayout.visibility = View.GONE
        binding.valueStatContinuousValueInputLayout.visibility = View.GONE
        viewModel.selectedValueStatFeature.observe(this, Observer {
            it?.let {
                if (it.featureType == FeatureType.DISCRETE) {
                    binding.valueStatDiscreteValueInputLayout.visibility = View.VISIBLE
                    binding.valueStatContinuousValueInputLayout.visibility = View.GONE
                }
                else {
                    binding.valueStatDiscreteValueInputLayout.visibility = View.GONE
                    binding.valueStatContinuousValueInputLayout.visibility = View.VISIBLE
                }
            }
        })
        listenToValueStatDiscreteValueSpinner()
        listenToValueStatContinuousRange()
    }

    private fun listenToValueStatDiscreteValueSpinner() {
        viewModel.selectedValueStatFeature.observe(this, Observer {
            if(it != null && it.featureType == FeatureType.DISCRETE) {
                val itemNames = it.discreteValues.map { dv -> dv.label }
                val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, itemNames)
                binding.valueStatDiscreteValueSpinner.adapter = adapter
                val selected = viewModel.selectedValueStatDiscreteValue.value
                val index = it.discreteValues.indexOf(selected)
                if (selected != null && index >= 0) binding.valueStatDiscreteValueSpinner.setSelection(index)
                else binding.valueStatDiscreteValueSpinner.setSelection(0)
                binding.valueStatDiscreteValueSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) { }
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                        viewModel.selectedValueStatDiscreteValue.value = it.discreteValues[index]
                    }
                }
            }
        })
    }

    private fun listenToValueStatContinuousRange() {
        if (viewModel.selectedValueStatToValue.value != null)
            binding.valueStatToInput.setText(decimalFormat.format(viewModel.selectedValueStatToValue.value!!))
        binding.valueStatToInput.addTextChangedListener { editText ->
            val string = editText.toString()
            viewModel.selectedValueStatToValue.value = if (string.isEmpty()) 0.toDouble() else string.toDouble()
        }
        if (viewModel.selectedValueStatFromValue.value != null)
            binding.valueStatFromInput.setText(decimalFormat.format(viewModel.selectedValueStatFromValue.value!!))
        binding.valueStatFromInput.addTextChangedListener { editText ->
            val string = editText.toString()
            viewModel.selectedValueStatFromValue.value = if (string.isEmpty()) 0.toDouble() else string.toDouble()
        }
    }

    private fun listenToAddLineGraphFeatureButton(features: List<FeatureAndTrackGroup>) {
        binding.addFeatureButton.isClickable = true
        binding.addFeatureButton.setOnClickListener {
            val color = colorList[(colorList.size - 1).coerceAtMost(viewModel.lineGraphFeatures.size)]
            val newLineGraphFeature = LineGraphFeature(-1, color, 0.toDouble(), 1.toDouble())
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
        val discreteFeatures = features
            .filter { f -> f.featureType == FeatureType.DISCRETE }
        val itemNames = discreteFeatures
            .map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.pieChartFeatureSpinner.adapter = adapter
        val selected = viewModel.selectedPieChartFeature.value
        if (selected != null) binding.pieChartFeatureSpinner.setSelection(discreteFeatures.indexOf(selected))
        binding.pieChartFeatureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.selectedPieChartFeature.value = discreteFeatures[index]
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
                    binding.valueStatInputLayout.visibility = View.GONE
                }
                GraphStatType.PIE_CHART -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.VISIBLE
                    binding.pieChartSelectFeatureLayout.visibility = View.VISIBLE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                    binding.valueStatInputLayout.visibility = View.GONE
                }
                GraphStatType.AVERAGE_TIME_BETWEEN -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.VISIBLE
                    binding.pieChartSelectFeatureLayout.visibility = View.GONE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                    binding.valueStatInputLayout.visibility = View.VISIBLE
                }
                GraphStatType.TIME_SINCE -> {
                    binding.movingAverageLayout.visibility = View.GONE
                    binding.timePeriodLayout.visibility = View.GONE
                    binding.pieChartSelectFeatureLayout.visibility = View.GONE
                    binding.addFeatureButton.visibility = View.GONE
                    binding.lineGraphFeaturesLayout.visibility = View.GONE
                    binding.valueStatInputLayout.visibility = View.VISIBLE
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
    val selectedValueStatFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val selectedValueStatDiscreteValue = MutableLiveData<DiscreteValue?>(null)
    val selectedValueStatFromValue = MutableLiveData<Double>(0.toDouble())
    val selectedValueStatToValue = MutableLiveData<Double>(0.toDouble())
    val lineGraphFeatures = mutableListOf<LineGraphFeature>()

    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
    }
}