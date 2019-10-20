package com.samco.grapheasy.graphstatinput

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController

import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import com.samco.grapheasy.databinding.FragmentGraphStatInputBinding
import kotlinx.coroutines.*
import org.threeten.bp.Period
import java.lang.Exception
import java.text.DecimalFormat

class GraphStatInputFragment : Fragment() {
    private var navController: NavController? = null
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
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_graph_stat_input, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProviders.of(this).get(GraphStatInputViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        listenToGraphName()
        listenToGraphTypeSpinner()
        listenToMovingAveragePeriod()
        listenToTimePeriod()
        listenToAllFeatures()
        listenToErrorText()
        listenToViewModelState()
        return binding.root
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(this, Observer {
            when (it) {
                GraphStatInputState.INITIALIZING -> binding.progressBar.visibility = View.VISIBLE
                GraphStatInputState.WAITING -> binding.progressBar.visibility = View.INVISIBLE
                GraphStatInputState.ADDING -> binding.progressBar.visibility = View.VISIBLE
                GraphStatInputState.FINISHED -> {
                    val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                    navController?.popBackStack()
                }
            }

        })
    }

    private fun listenToErrorText() {
        viewModel.errorText.observe(this, Observer {
            binding.errorText.postDelayed({
                binding.errorText.text = viewModel.errorText.value!!
            },200)
        })
    }

    private fun listenToGraphName() {
        binding.graphStatNameInput.setText(viewModel.graphName.value)
        binding.graphStatNameInput.addTextChangedListener { editText ->
            viewModel.graphName.value = editText.toString()
            validateForm()
        }
    }

    private fun listenToAllFeatures() {
        viewModel.allFeatures.observe(this, Observer {
            initPieChartAdapter(it)
            listenToAddLineGraphFeatureButton(it)
            createLineGraphFeatureViews(it)
            listenToValueStat(it)
            listenToAddButton()
        })
    }

    private fun listenToAddButton() {
        binding.addButton.setOnClickListener {
            viewModel.createGraphOrStat()
        }
    }

    private fun listenToValueStat(features: List<FeatureAndTrackGroup>) {
        val itemNames = features.map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.valueStatFeatureSpinner.adapter = adapter
        val selected = viewModel.selectedValueStatFeature.value
        if (selected != null) binding.valueStatFeatureSpinner.setSelection(features.indexOf(selected))
        binding.valueStatFeatureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.selectedValueStatFeature.value = features[index]
                validateForm()
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
                validateForm()
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
                        validateForm()
                    }
                }
            }
        })
    }

    private fun listenToValueStatContinuousRange() {
        if (viewModel.selectedValueStatToValue.value != null)
            binding.valueStatToInput.setText(decimalFormat.format(viewModel.selectedValueStatToValue.value!!))
        binding.valueStatToInput.addTextChangedListener { editText ->
            viewModel.selectedValueStatToValue.value = editText.toString().toDoubleOrNull() ?: 0.toDouble()
            validateForm()
        }
        if (viewModel.selectedValueStatFromValue.value != null)
            binding.valueStatFromInput.setText(decimalFormat.format(viewModel.selectedValueStatFromValue.value!!))
        binding.valueStatFromInput.addTextChangedListener { editText ->
            viewModel.selectedValueStatFromValue.value = editText.toString().toDoubleOrNull() ?: 0.toDouble()
            validateForm()
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
        view.setOnUpdateListener { validateForm() }
        val params = LinearLayout.LayoutParams(
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
                validateForm()
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
                validateForm()
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
                validateForm()
            }
        }
    }

    private fun validateForm() {
        try {
            if (viewModel.graphName.value!!.isEmpty()) throw ValidationException(R.string.graph_stat_validation_no_name)
            when (viewModel.graphStatType.value) {
                GraphStatType.LINE_GRAPH -> validateLineGraph()
                GraphStatType.PIE_CHART -> validatePieChart()
                GraphStatType.TIME_SINCE -> validateTimeSince()
                GraphStatType.AVERAGE_TIME_BETWEEN -> validateAverageTimeBetween()
                else -> failValidation(R.string.graph_stat_validation_bad_type)
            }
            passValidation()
        } catch (e: Exception) {
            if (e is ValidationException) failValidation(e.errorMessageId)
            else failValidation(R.string.graph_stat_validation_unknown)
        }
    }

    private class ValidationException(val errorMessageId: Int): Exception()

    private fun failValidation(errorMessageId: Int) {
        viewModel.errorText.value = getString(errorMessageId)
        binding.addButton.isEnabled = false
    }

    private fun passValidation() {
        viewModel.errorText.value = ""
        binding.addButton.isEnabled = true
    }

    private fun validateAverageTimeBetween() { validateValueStat() }

    private fun validateTimeSince() { validateValueStat() }

    private fun validateValueStat() {
        if (viewModel.selectedValueStatFeature.value == null)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        if (viewModel.selectedValueStatFeature.value!!.featureType == FeatureType.DISCRETE) {
            if (viewModel.selectedValueStatDiscreteValue.value == null)
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_discrete_value)
            if (!viewModel.selectedValueStatFeature.value!!.discreteValues
                    .contains(viewModel.selectedValueStatDiscreteValue.value!!))
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_discrete_value)
        }
        if (viewModel.selectedValueStatFeature.value!!.featureType == FeatureType.CONTINUOUS) {
            if (viewModel.selectedValueStatFromValue.value!! > viewModel.selectedValueStatToValue.value!!)
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        }
    }

    private fun validatePieChart() {
        if (viewModel.selectedPieChartFeature.value == null
            || viewModel.selectedPieChartFeature.value!!.featureType != FeatureType.DISCRETE)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
    }

    private fun validateLineGraph() {
        if (viewModel.lineGraphFeatures.size == 0)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        viewModel.lineGraphFeatures.forEach { f ->
            if (!colorList.contains(f.colorId))
                throw ValidationException(R.string.graph_stat_validation_unrecognised_color)
            if (viewModel.allFeatures.value?.map { feat -> feat.id }?.contains(f.featureId) != true)
                throw ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature)
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
                validateForm()
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


enum class GraphStatInputState { INITIALIZING, WAITING, ADDING, FINISHED }
class GraphStatInputViewModel : ViewModel() {
    private var dataSource: GraphEasyDatabaseDao? = null

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    val state: LiveData<GraphStatInputState> get() { return _state }
    val _state = MutableLiveData<GraphStatInputState>(GraphStatInputState.INITIALIZING)
    val errorText = MutableLiveData<String>("")
    val graphName = MutableLiveData<String>("")
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
        _state.value = GraphStatInputState.INITIALIZING
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
        _state.value = GraphStatInputState.WAITING
    }

    fun createGraphOrStat() {
        if (_state.value != GraphStatInputState.WAITING) return
        _state.value = GraphStatInputState.ADDING
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val graphStatId = dataSource!!.insertGraphOrStat(GraphOrStat(0,
                    graphName.value!!, graphStatType.value!!))
                when (graphStatType.value) {
                    GraphStatType.LINE_GRAPH -> addLineGraph(graphStatId)
                    GraphStatType.PIE_CHART -> addPieChart(graphStatId)
                    GraphStatType.AVERAGE_TIME_BETWEEN -> addAverageTimeBetweenStat(graphStatId)
                    GraphStatType.TIME_SINCE -> addTimeSinceStat(graphStatId)
                }
            }
            _state.value = GraphStatInputState.FINISHED
        }
    }

    private fun addLineGraph(graphStatId: Long) {
        dataSource?.insertLineGraph(LineGraph(0, graphStatId, lineGraphFeatures,
            samplePeriod.value, movingAveragePeriod.value))
    }

    private fun addPieChart(graphStatId: Long) {
        dataSource?.insertPieChart(PieChart(0, graphStatId,
            selectedPieChartFeature.value!!.id, samplePeriod.value))
    }

    private fun addAverageTimeBetweenStat(graphStatId: Long) {
        dataSource?.insertAverageTimeBetweenStat(AverageTimeBetweenStat(0, graphStatId,
            selectedValueStatFeature.value!!.id, getFromValue(), getToValue(), samplePeriod.value))
    }

    private fun addTimeSinceStat(graphStatId: Long) {
        dataSource?.insertTimeSinceLastStat(TimeSinceLastStat(0, graphStatId,
            selectedValueStatFeature.value!!.id, getFromValue(), getToValue()))
    }

    private fun getFromValue(): String {
        return if (selectedValueStatFeature.value!!.featureType == FeatureType.DISCRETE)
            selectedValueStatDiscreteValue.value!!.index.toString()
        else selectedValueStatFromValue.value!!.toString()
    }

    private fun getToValue(): String {
        return if (selectedValueStatFeature.value!!.featureType == FeatureType.DISCRETE)
            selectedValueStatDiscreteValue.value!!.index.toString()
        else selectedValueStatToValue.value!!.toString()
    }
}
