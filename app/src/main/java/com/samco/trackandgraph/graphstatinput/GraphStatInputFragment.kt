package com.samco.trackandgraph.graphstatinput

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
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
import androidx.navigation.fragment.navArgs

import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentGraphStatInputBinding
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import java.lang.Exception
import java.text.DecimalFormat

class GraphStatInputFragment : Fragment() {
    private var navController: NavController? = null
    private val args: GraphStatInputFragmentArgs by navArgs()
    private lateinit var binding: FragmentGraphStatInputBinding
    private lateinit var viewModel: GraphStatInputViewModel

    private val updateDemoHandler = Handler()
    private val decimalFormat = DecimalFormat("0.###############")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_graph_stat_input, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.graphStatNameInput.filters = arrayOf(InputFilter.LengthFilter(MAX_GRAPH_STAT_NAME_LENGTH))
        viewModel = ViewModelProviders.of(this).get(GraphStatInputViewModel::class.java)
        viewModel.initViewModel(requireActivity(), args.graphStatGroupId, args.graphStatId)
        binding.demoGraphStatView.hideMenuButton()
        listenToViewModelState()
        return binding.root
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(this, Observer {
            when (it) {
                GraphStatInputState.INITIALIZING -> binding.inputProgressBar.visibility = View.VISIBLE
                GraphStatInputState.WAITING -> {
                    binding.inputProgressBar.visibility = View.INVISIBLE
                    listenToUpdateMode()
                    listenToGraphTypeSpinner()
                    listenToGraphName()
                    listenToTimeDuration()
                    listenToAllFeatures()
                    listenToFormValid()
                }
                GraphStatInputState.ADDING -> binding.inputProgressBar.visibility = View.VISIBLE
                else -> navController?.popBackStack()
            }
        })
    }

    private fun listenToUpdateMode() {
        viewModel.updateMode.observe(this, Observer { b ->
            if (b) {
                binding.addBar.addButton.setText(R.string.update)
                binding.graphStatTypeLayout.visibility = View.GONE
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
        binding.addBar.addButton.setOnClickListener {
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
            val nextColorIndex = (viewModel.lineGraphFeatures.size * dataVisColorGenerator) % dataVisColorList.size
            val newLineGraphFeature = LineGraphFeature(-1, "", nextColorIndex,
                LineGraphAveraginModes.NO_AVERAGING, LineGraphPlottingModes.WHEN_TRACKED, 0.toDouble(), 1.toDouble())
            viewModel.lineGraphFeatures = viewModel.lineGraphFeatures.plus(newLineGraphFeature)
            inflateLineGraphFeatureView(newLineGraphFeature, features)
        }
        binding.scrollView.fullScroll(View.FOCUS_DOWN)
    }

    private fun createLineGraphFeatureViews(features: List<FeatureAndTrackGroup>) {
        viewModel.lineGraphFeatures.forEach { lgf -> inflateLineGraphFeatureView(lgf, features) }
    }

    private fun inflateLineGraphFeatureView(lgf: LineGraphFeature, features: List<FeatureAndTrackGroup>) {
        val view = GraphFeatureListItemView(context!!, features, dataVisColorList, lgf)
        view.setOnRemoveListener {
            viewModel.lineGraphFeatures = viewModel.lineGraphFeatures.minus(lgf)
            binding.lineGraphFeaturesLayout.removeView(view)
            binding.addFeatureButton.isEnabled = true
        }
        view.setOnUpdateListener { onFormUpdate() }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = params
        binding.lineGraphFeaturesLayout.addView(view)
        binding.lineGraphFeaturesLayout.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
            view.requestFocus()
        }
        if (viewModel.lineGraphFeatures.size == MAX_LINE_GRAPH_FEATURES) {
            binding.addFeatureButton.isEnabled = false
        }
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
        updateViewForSelectedGraphStatType(viewModel.graphStatType.value!!)
        binding.graphTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                val graphStatType = graphTypes[index]
                viewModel.graphStatType.value = graphStatType
                updateViewForSelectedGraphStatType(graphStatType)
            }
        }
    }

    private fun updateViewForSelectedGraphStatType(graphStatType: GraphStatType) {
        onFormUpdate()
        when(graphStatType) {
            GraphStatType.LINE_GRAPH -> initLineGraphView()
            GraphStatType.PIE_CHART -> initPieChartView()
            GraphStatType.AVERAGE_TIME_BETWEEN -> initAverageTimeBetweenView()
            GraphStatType.TIME_SINCE -> initTimeSinceView()
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
            binding.addBar.addButton.isEnabled = errorNow == null
            binding.addBar.errorText.postDelayed({
                val errorThen = viewModel.formValid.value
                val text = errorThen?.let { getString(it.errorMessageId) } ?: ""
                binding.addBar.errorText.text = text
            }, 200)
        })
    }

    private fun onFormUpdate() {
        viewModel.validateForm()
        updateDemoView()
    }

    private fun updateDemoView() {
        updateDemoHandler.removeCallbacksAndMessages(null)
        updateDemoHandler.postDelayed(Runnable {
            if (viewModel.formValid.value != null) {
                binding.demoGraphStatView.initInvalid()
            } else {
                val graphOrStat = viewModel.constructGraphOrStat()
                when (viewModel.graphStatType.value) {
                    GraphStatType.LINE_GRAPH -> binding.demoGraphStatView
                        .initFromLineGraph(graphOrStat, viewModel.constructLineGraph(graphOrStat.id))
                    GraphStatType.PIE_CHART -> binding.demoGraphStatView
                        .initFromPieChart(graphOrStat, viewModel.constructPieChart(graphOrStat.id))
                    GraphStatType.AVERAGE_TIME_BETWEEN -> binding.demoGraphStatView
                        .initAverageTimeBetweenStat(graphOrStat, viewModel.constructAverageTimeBetween(graphOrStat.id))
                    GraphStatType.TIME_SINCE -> binding.demoGraphStatView
                        .initTimeSinceStat(graphOrStat, viewModel.constructTimeSince(graphOrStat.id))
                    else -> binding.demoGraphStatView.initInvalid()
                }
            }
        }, 500)
    }

    override fun onDestroyView() {
        binding.demoGraphStatView.dispose()
        super.onDestroyView()
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}

enum class GraphStatInputState { INITIALIZING, WAITING, ADDING, FINISHED }
class GraphStatInputViewModel : ViewModel() {
    class ValidationException(val errorMessageId: Int): Exception()

    private var dataSource: TrackAndGraphDatabaseDao? = null

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var graphStatGroupId: Long = -1L
    private var graphStatId: Long? = null
    private var id: Long? = null

    val graphName = MutableLiveData<String>("")
    val graphStatType = MutableLiveData<GraphStatType>(GraphStatType.LINE_GRAPH)
    val sampleDuration = MutableLiveData<Duration?>(null)
    val selectedPieChartFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val selectedValueStatFeature = MutableLiveData<FeatureAndTrackGroup?>(null)
    val selectedValueStatDiscreteValue = MutableLiveData<DiscreteValue?>(null)
    val selectedValueStatFromValue = MutableLiveData<Double>(0.toDouble())
    val selectedValueStatToValue = MutableLiveData<Double>(0.toDouble())
    var lineGraphFeatures = listOf<LineGraphFeature>()
    val updateMode: LiveData<Boolean> get() { return _updateMode }
    private val _updateMode = MutableLiveData<Boolean>(false)
    val state: LiveData<GraphStatInputState> get() { return _state }
    private val _state = MutableLiveData<GraphStatInputState>(GraphStatInputState.INITIALIZING)
    val formValid: LiveData<ValidationException?> get() { return _formValid }
    private val _formValid = MutableLiveData<ValidationException?>(null)
    lateinit var allFeatures: LiveData<List<FeatureAndTrackGroup>> private set


    fun initViewModel(activity: Activity, graphStatGroupId: Long, graphStatId: Long) {
        if (dataSource != null) return
        this.graphStatGroupId = graphStatGroupId
        _state.value = GraphStatInputState.INITIALIZING
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allFeatures = dataSource!!.getAllFeaturesAndTrackGroups()
        if (graphStatId != -1L) initFromExistingGraphStat(graphStatId)
        else _state.value = GraphStatInputState.WAITING
    }

    private fun initFromExistingGraphStat(graphStatId: Long) {
        ioScope.launch {
            val graphStat = dataSource!!.getGraphStatById(graphStatId)

            if (graphStat == null) {
                withContext(Dispatchers.Main) { _state.value = GraphStatInputState.WAITING }
                return@launch
            }

            val existingId = when(graphStat.type) {
                GraphStatType.LINE_GRAPH -> tryInitLineGraph(graphStat)
                GraphStatType.PIE_CHART -> tryInitPieChart(graphStat)
                GraphStatType.TIME_SINCE -> tryInitTimeSinceStat(graphStat)
                GraphStatType.AVERAGE_TIME_BETWEEN -> tryInitAverageTimeBetween(graphStat)
            }

            if (existingId != null) withContext(Dispatchers.Main) {
                this@GraphStatInputViewModel.graphName.value = graphStat.name
                this@GraphStatInputViewModel.graphStatType.value = graphStat.type
                this@GraphStatInputViewModel.graphStatId = graphStat.id
                this@GraphStatInputViewModel.id = existingId
                this@GraphStatInputViewModel._updateMode.value = true
            }
            withContext(Dispatchers.Main) { _state.value = GraphStatInputState.WAITING }
        }
    }

    private suspend fun tryInitPieChart(graphStat: GraphOrStat): Long? {
        val pieChart = dataSource!!.getPieChartByGraphStatId(graphStat.id) ?: return null
        val pieChartFeature = dataSource!!.getFeatureAndTrackGroupByFeatureId(pieChart.featureId) ?: return null
        withContext(Dispatchers.Main) {
            this@GraphStatInputViewModel.selectedPieChartFeature.value = pieChartFeature
            this@GraphStatInputViewModel.sampleDuration.value = pieChart.duration
        }
        return pieChart.id
    }

    private suspend fun tryInitAverageTimeBetween(graphStat: GraphOrStat): Long? {
        val avTimeStat = dataSource!!.getAverageTimeBetweenStatByGraphStatId(graphStat.id) ?: return null
        val feature = dataSource!!.getFeatureAndTrackGroupByFeatureId(avTimeStat.featureId) ?: return null
        withContext(Dispatchers.Main) {
            this@GraphStatInputViewModel.selectedValueStatFeature.value = feature
            if (feature.featureType == FeatureType.DISCRETE) {
                this@GraphStatInputViewModel.selectedValueStatDiscreteValue.value =
                    feature.discreteValues.firstOrNull { dv -> dv.index == avTimeStat.fromValue.toInt() }
            } else {
                this@GraphStatInputViewModel.selectedValueStatFromValue.value = avTimeStat.fromValue.toDouble()
                this@GraphStatInputViewModel.selectedValueStatToValue.value = avTimeStat.toValue.toDouble()
            }
            this@GraphStatInputViewModel.sampleDuration.value = avTimeStat.duration
        }
        return avTimeStat.id
    }

    private suspend fun tryInitTimeSinceStat(graphStat: GraphOrStat): Long? {
        val timeSinceStat = dataSource!!.getTimeSinceLastStatByGraphStatId(graphStat.id) ?: return null
        val feature = dataSource!!.getFeatureAndTrackGroupByFeatureId(timeSinceStat.featureId) ?: return null
        withContext(Dispatchers.Main) {
            this@GraphStatInputViewModel.selectedValueStatFeature.value = feature
            if (feature.featureType == FeatureType.DISCRETE) {
                this@GraphStatInputViewModel.selectedValueStatDiscreteValue.value =
                    feature.discreteValues.firstOrNull { dv -> dv.index == timeSinceStat.fromValue.toInt() }
            } else {
                this@GraphStatInputViewModel.selectedValueStatFromValue.value = timeSinceStat.fromValue.toDouble()
                this@GraphStatInputViewModel.selectedValueStatToValue.value = timeSinceStat.toValue.toDouble()
            }
        }
        return timeSinceStat.id
    }

    private suspend fun tryInitLineGraph(graphStat: GraphOrStat): Long? {
        val lineGraph = dataSource!!.getLineGraphByGraphStatId(graphStat.id) ?: return null
        withContext(Dispatchers.Main) {
            this@GraphStatInputViewModel.sampleDuration.value = lineGraph.duration
            this@GraphStatInputViewModel.lineGraphFeatures = lineGraph.features
        }
        return lineGraph.id
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
        if (lineGraphFeatures.isEmpty())
            throw ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        lineGraphFeatures.forEach { f ->
            if (f.colorIndex !in dataVisColorList.indices)
                throw ValidationException(R.string.graph_stat_validation_unrecognised_color)
            if (allFeatures.value?.map { feat -> feat.id }?.contains(f.featureId) != true)
                throw ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature)
        }
    }

    fun createGraphOrStat() {
        if (_state.value != GraphStatInputState.WAITING) return
        _state.value = GraphStatInputState.ADDING
        ioScope.launch {
            val graphStatId = if (_updateMode.value!!) {
                dataSource!!.updateGraphOrStat(constructGraphOrStat())
                graphStatId!!
            } else {
                shiftUpGraphStatViewIndexes()
                dataSource!!.insertGraphOrStat(constructGraphOrStat())
            }

            when (graphStatType.value) {
                GraphStatType.LINE_GRAPH -> addLineGraph(constructLineGraph(graphStatId))
                GraphStatType.PIE_CHART -> addPieChart(constructPieChart(graphStatId))
                GraphStatType.AVERAGE_TIME_BETWEEN -> addAvTimeBetween(constructAverageTimeBetween(graphStatId))
                GraphStatType.TIME_SINCE -> addTimeSince(constructTimeSince(graphStatId))
            }
            withContext(Dispatchers.Main) { _state.value = GraphStatInputState.FINISHED }
        }
    }

    private fun shiftUpGraphStatViewIndexes() {
        val newList = dataSource!!.getAllGraphStatsSync().map {
            it.copy(displayIndex = it.displayIndex + 1)
        }
        dataSource!!.updateGraphStats(newList)
    }

    private fun addLineGraph(lineGraph: LineGraph) {
        if (_updateMode.value!!) dataSource!!.updateLineGraph(lineGraph)
        else dataSource!!.insertLineGraph(lineGraph)
    }

    private fun addPieChart(pieChart: PieChart) {
        if (_updateMode.value!!) dataSource!!.updatePieChart(pieChart)
        else dataSource!!.insertPieChart(pieChart)
    }

    private fun addAvTimeBetween(averageTimeBetweenStat: AverageTimeBetweenStat) {
        if (_updateMode.value!!) dataSource!!.updateAverageTimeBetweenStat(averageTimeBetweenStat)
        else dataSource!!.insertAverageTimeBetweenStat(averageTimeBetweenStat)
    }

    private fun addTimeSince(timeSinceLastStat: TimeSinceLastStat) {
        if (_updateMode.value!!) dataSource!!.updateTimeSinceLastStat(timeSinceLastStat)
        else dataSource!!.insertTimeSinceLastStat(timeSinceLastStat)
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    fun constructGraphOrStat() = GraphOrStat.create(
        graphStatId ?: 0L, graphStatGroupId, graphName.value!!, graphStatType.value!!, 0
    )

    fun constructLineGraph(graphStatId: Long) = LineGraph.create(
        id ?: 0L, graphStatId, lineGraphFeatures, sampleDuration.value
    )

    fun constructPieChart(graphStatId: Long) = PieChart(
        id ?: 0L, graphStatId,
        selectedPieChartFeature.value!!.id, sampleDuration.value
    )

    fun constructAverageTimeBetween(graphStatId: Long) = AverageTimeBetweenStat(
        id ?: 0L, graphStatId,
        selectedValueStatFeature.value!!.id, getFromValue(),
        getToValue(), sampleDuration.value
    )

    fun constructTimeSince(graphStatId: Long) = TimeSinceLastStat(
        id ?: 0L, graphStatId,
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
