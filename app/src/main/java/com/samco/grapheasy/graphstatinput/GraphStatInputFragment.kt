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
import org.threeten.bp.Duration
import java.lang.Exception
import java.text.DecimalFormat

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

class GraphStatInputFragment : Fragment() {
    private var navController: NavController? = null
    private lateinit var binding: FragmentGraphStatInputBinding
    private lateinit var viewModel: GraphStatInputViewModel

    private val decimalFormat = DecimalFormat("0.###############")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_graph_stat_input, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProviders.of(this).get(GraphStatInputViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        listenToGraphName()
        listenToGraphTypeSpinner()
        listenToTimeDuration()
        listenToAllFeatures()
        listenToViewModelState()
        listenToFormValid()
        return binding.root
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(this, Observer {
            when (it) {
                GraphStatInputState.INITIALIZING -> binding.progressBar.visibility = View.VISIBLE
                GraphStatInputState.WAITING -> binding.progressBar.visibility = View.INVISIBLE
                GraphStatInputState.ADDING -> binding.progressBar.visibility = View.VISIBLE
                GraphStatInputState.FINISHED -> navController?.popBackStack()
            }
        })
    }

    private fun listenToGraphName() {
        binding.graphStatNameInput.setText(viewModel.graphName.value)
        binding.graphStatNameInput.addTextChangedListener { editText ->
            viewModel.graphName.value = editText.toString()
            onFormUpdate()
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
                onFormUpdate()
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
                onFormUpdate()
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
                        onFormUpdate()
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
            onFormUpdate()
        }
        if (viewModel.selectedValueStatFromValue.value != null)
            binding.valueStatFromInput.setText(decimalFormat.format(viewModel.selectedValueStatFromValue.value!!))
        binding.valueStatFromInput.addTextChangedListener { editText ->
            viewModel.selectedValueStatFromValue.value = editText.toString().toDoubleOrNull() ?: 0.toDouble()
            onFormUpdate()
        }
    }

    private fun listenToAddLineGraphFeatureButton(features: List<FeatureAndTrackGroup>) {
        binding.addFeatureButton.isClickable = true
        binding.addFeatureButton.setOnClickListener {
            val color = colorList[(colorList.size - 1).coerceAtMost(viewModel.lineGraphFeatures.size)]
            val newLineGraphFeature = LineGraphFeature(-1, color,
                LineGraphFeatureMode.TRACKED_VALUES, 0.toDouble(), 1.toDouble())
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
        view.setOnUpdateListener { onFormUpdate() }
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
                onFormUpdate()
            }
        }
    }

    private fun listenToTimeDuration() {
        //TODO move these types of declarations into their corresponding data class files
        val timeDurations = listOf(
            null,
            Duration.ofDays(1),
            Duration.ofDays(7),
            Duration.ofDays(31),
            Duration.ofDays(93),
            Duration.ofDays(183),
            Duration.ofDays(365)
        )
        binding.sampleDurationSpinner.setSelection(timeDurations.indexOf(viewModel.sampleDuration.value))
        binding.sampleDurationSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                viewModel.sampleDuration.value = timeDurations[index]
                onFormUpdate()
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
                val graphStatType = graphTypes[index]
                viewModel.graphStatType.value = graphStatType
                onFormUpdate()
                when(graphStatType) {
                    GraphStatType.LINE_GRAPH -> initLineGraphView()
                    GraphStatType.PIE_CHART -> initPieChartView()
                    GraphStatType.AVERAGE_TIME_BETWEEN -> initAverageTimeBetweenView()
                    GraphStatType.TIME_SINCE -> initTimeSinceView()
                }
            }
        }
    }

    private fun initTimeSinceView() {
        binding.timeDurationLayout.visibility = View.GONE
        binding.pieChartSelectFeatureLayout.visibility = View.GONE
        binding.addFeatureButton.visibility = View.GONE
        binding.lineGraphFeaturesLayout.visibility = View.GONE
        binding.valueStatInputLayout.visibility = View.VISIBLE
    }

    private fun initAverageTimeBetweenView() {
        binding.timeDurationLayout.visibility = View.VISIBLE
        binding.pieChartSelectFeatureLayout.visibility = View.GONE
        binding.addFeatureButton.visibility = View.GONE
        binding.lineGraphFeaturesLayout.visibility = View.GONE
        binding.valueStatInputLayout.visibility = View.VISIBLE
    }

    private fun initPieChartView() {
        binding.timeDurationLayout.visibility = View.VISIBLE
        binding.pieChartSelectFeatureLayout.visibility = View.VISIBLE
        binding.addFeatureButton.visibility = View.GONE
        binding.lineGraphFeaturesLayout.visibility = View.GONE
        binding.valueStatInputLayout.visibility = View.GONE
    }

    private fun initLineGraphView() {
        binding.timeDurationLayout.visibility = View.VISIBLE
        binding.pieChartSelectFeatureLayout.visibility = View.GONE
        binding.addFeatureButton.visibility = View.VISIBLE
        binding.lineGraphFeaturesLayout.visibility = View.VISIBLE
        binding.valueStatInputLayout.visibility = View.GONE
    }

    private fun listenToFormValid() {
        viewModel.formValid.observe(this, Observer { errorNow ->
            binding.addButton.isEnabled = errorNow == null
            binding.errorText.postDelayed({
                val errorThen = viewModel.formValid.value
                val text = errorThen?.let { getString(it.errorMessageId) } ?: ""
                binding.errorText.text = text
            }, 200)
        })
    }

    private fun onFormUpdate() {
        viewModel.validateForm()
        updateDemoView()
    }

    private fun updateDemoView() {
        if (viewModel.formValid.value != null) {
            binding.demoGraphStatView.initInvalid()
        } else {
            val view = binding.demoGraphStatView
            val graphOrStat = viewModel.constructGraphOrStat()
            when (viewModel.graphStatType.value) {
                GraphStatType.LINE_GRAPH -> view.initFromLineGraph(graphOrStat, viewModel.constructLineGraph(-1))
                else -> view.initInvalid()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.demoGraphStatView.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}

enum class GraphStatInputState { INITIALIZING, WAITING, ADDING, FINISHED }
class GraphStatInputViewModel : ViewModel() {
    class ValidationException(val errorMessageId: Int): Exception()

    private var dataSource: GraphEasyDatabaseDao? = null

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    val state: LiveData<GraphStatInputState> get() { return _state }
    private val _state = MutableLiveData<GraphStatInputState>(GraphStatInputState.INITIALIZING)
    val graphName = MutableLiveData<String>("")
    val graphStatType = MutableLiveData<GraphStatType>(GraphStatType.LINE_GRAPH)
    val sampleDuration = MutableLiveData<Duration?>(null)
    val selectedPieChartFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val selectedValueStatFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val selectedValueStatDiscreteValue = MutableLiveData<DiscreteValue?>(null)
    val selectedValueStatFromValue = MutableLiveData<Double>(0.toDouble())
    val selectedValueStatToValue = MutableLiveData<Double>(0.toDouble())
    val formValid: LiveData<ValidationException?> get() { return _formValid }
    private val _formValid = MutableLiveData<ValidationException?>(null)
    val lineGraphFeatures = mutableListOf<LineGraphFeature>()

    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set


    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        _state.value = GraphStatInputState.INITIALIZING
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
        _state.value = GraphStatInputState.WAITING
    }

    fun validateForm() {
        try {
            if (graphName.value!!.isEmpty()) throw ValidationException(R.string.graph_stat_validation_no_name)
            when (graphStatType.value) {
                GraphStatType.LINE_GRAPH -> validateLineGraph()
                GraphStatType.PIE_CHART -> validatePieChart()
                GraphStatType.TIME_SINCE -> validateTimeSince()
                GraphStatType.AVERAGE_TIME_BETWEEN -> validateAverageTimeBetween()
                else -> throw Exception("")
            }
            _formValid.value = null
        } catch (e: Exception) {
            if (e is ValidationException) _formValid.value = e
            else _formValid.value = ValidationException(R.string.graph_stat_validation_unknown)
        }
    }

    private fun validateAverageTimeBetween() { validateValueStat() }

    private fun validateTimeSince() { validateValueStat() }

    private fun validateValueStat() {
        if (selectedValueStatFeature.value == null)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        if (selectedValueStatFeature.value!!.featureType == FeatureType.DISCRETE) {
            if (selectedValueStatDiscreteValue.value == null)
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_discrete_value)
            if (!selectedValueStatFeature.value!!.discreteValues
                    .contains(selectedValueStatDiscreteValue.value!!))
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_discrete_value)
        }
        if (selectedValueStatFeature.value!!.featureType == FeatureType.CONTINUOUS) {
            if (selectedValueStatFromValue.value!! > selectedValueStatToValue.value!!)
                throw ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        }
    }

    private fun validatePieChart() {
        if (selectedPieChartFeature.value == null
            || selectedPieChartFeature.value!!.featureType != FeatureType.DISCRETE)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
    }

    private fun validateLineGraph() {
        if (lineGraphFeatures.size == 0)
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        lineGraphFeatures.forEach { f ->
            if (!colorList.contains(f.colorId))
                throw ValidationException(R.string.graph_stat_validation_unrecognised_color)
            if (allFeatures.value?.map { feat -> feat.id }?.contains(f.featureId) != true)
                throw ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature)
        }
    }

    fun createGraphOrStat() {
        if (_state.value != GraphStatInputState.WAITING) return
        _state.value = GraphStatInputState.ADDING
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val graphStatId = dataSource!!.insertGraphOrStat(constructGraphOrStat())
                when (graphStatType.value) {
                    GraphStatType.LINE_GRAPH -> dataSource?.insertLineGraph(constructLineGraph(graphStatId))
                    GraphStatType.PIE_CHART -> dataSource?.insertPieChart(constructPieChart(graphStatId))
                    GraphStatType.AVERAGE_TIME_BETWEEN -> dataSource?.insertAverageTimeBetweenStat(constructAverageTimeBetween(graphStatId))
                    GraphStatType.TIME_SINCE -> dataSource?.insertTimeSinceLastStat(constructTimeSince(graphStatId))
                }
                return@withContext null
            }
            _state.value = GraphStatInputState.FINISHED
        }
    }

    override fun onCleared() {
        super.onCleared()
        uiScope.cancel()
    }

    fun constructGraphOrStat() = GraphOrStat(
        0, graphName.value!!, graphStatType.value!!
    )

    fun constructLineGraph(graphStatId: Long) = LineGraph(
        0, graphStatId, lineGraphFeatures, sampleDuration.value
    )

    fun constructPieChart(graphStatId: Long) = PieChart(
        0, graphStatId,
        selectedPieChartFeature.value!!.id, sampleDuration.value
    )

    fun constructAverageTimeBetween(graphStatId: Long) = AverageTimeBetweenStat(
        0, graphStatId,
        selectedValueStatFeature.value!!.id, getFromValue(),
        getToValue(), sampleDuration.value
    )

    fun constructTimeSince(graphStatId: Long) = TimeSinceLastStat(
        0, graphStatId,
        selectedValueStatFeature.value!!.id, getFromValue(), getToValue()
    )

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
