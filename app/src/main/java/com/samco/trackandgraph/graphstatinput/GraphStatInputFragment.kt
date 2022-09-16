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
package com.samco.trackandgraph.graphstatinput

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.databinding.FragmentGraphStatInputBinding
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.configviews.*
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.util.bindingForViewLifecycle
import com.samco.trackandgraph.util.focusAndShowKeyboard
import com.samco.trackandgraph.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class GraphStatInputFragment : Fragment() {
    private var navController: NavController? = null
    private val args: GraphStatInputFragmentArgs by navArgs()
    private var binding: FragmentGraphStatInputBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<GraphStatInputViewModel>()

    private val updateDemoHandler = Handler(Looper.getMainLooper())

    private lateinit var currentConfigView: GraphStatConfigView

    private var lastPreviewButtonDownPosY = 0f

    @Inject
    lateinit var gsiProvider: GraphStatInteractorProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = container?.findNavController()
        binding = FragmentGraphStatInputBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.initViewModel(args.groupId, args.graphStatId)
        listenToViewModelState()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.UP,
            getString(R.string.add_a_graph_or_stat)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.previewOverlay.visibility = View.GONE
        binding.btnPreivew.visibility = View.GONE
        binding.demoGraphStatCardView.hideMenuButton()
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it) {
                GraphStatInputState.INITIALIZING -> binding.inputProgressBar.visibility =
                    View.VISIBLE
                GraphStatInputState.SET_FOCUS -> binding.graphStatNameInput.focusAndShowKeyboard()
                GraphStatInputState.WAITING -> {
                    binding.inputProgressBar.visibility = View.INVISIBLE
                    listenToUpdateMode()
                    listenToGraphTypeSpinner()
                    listenToGraphName()
                    listenToFormValid()
                    listenToDemoViewData()
                    listenToAddButton()
                    listenToPreviewButton()
                }
                GraphStatInputState.ADDING -> binding.inputProgressBar.visibility = View.VISIBLE
                else -> navController?.popBackStack()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listenToPreviewButton() {
        binding.btnPreivew.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && binding.btnPreivew.isEnabled) {
                lastPreviewButtonDownPosY = event.y
                v.isPressed = true
                binding.previewOverlay.visibility = View.VISIBLE
                return@setOnTouchListener true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val diff = lastPreviewButtonDownPosY - event.y
                binding.previewScrollView.scrollY = (diff * 3f).toInt()
            } else if (event.action == MotionEvent.ACTION_UP) {
                v.isPressed = false
                binding.previewOverlay.visibility = View.GONE
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }

    private fun listenToConfigView() {
        currentConfigView.setConfigChangedListener(viewModel::onNewConfigData)
        currentConfigView.setOnScrollListener { binding.scrollView.fullScroll(it) }
        currentConfigView.setOnHideKeyboardListener {
            requireActivity().window.hideKeyboard()
        }
    }

    private fun listenToDemoViewData() {
        viewModel.demoViewData.observe(viewLifecycleOwner, Observer {
            updateDemoView(it)
        })
    }

    private fun listenToUpdateMode() {
        viewModel.updateMode.observe(viewLifecycleOwner, Observer { b ->
            if (b) {
                binding.addBar.addButton.setText(R.string.update)
                binding.graphStatTypeLayout.visibility = View.GONE
            }
        })
    }

    private fun listenToGraphName() {
        binding.graphStatNameInput.setText(viewModel.graphName.value)
        binding.graphStatNameInput.addTextChangedListener { editText ->
            viewModel.setGraphName(editText.toString())
        }
    }

    private fun listenToAddButton() {
        binding.addBar.addButton.setOnClickListener {
            viewModel.createGraphOrStat()
        }
    }

    private fun listenToGraphTypeSpinner() {
        val graphTypes = GraphStatType.values()
        binding.graphTypeSpinner.setSelection(graphTypes.indexOf(viewModel.graphStatType.value))
        viewModel.graphStatType.observe(viewLifecycleOwner, Observer {
            updateViewForSelectedGraphStatType(it)
        })
        binding.graphTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    viewModel.setGraphType(graphTypes[index])
                }
            }
    }

    private fun updateViewForSelectedGraphStatType(graphStatType: GraphStatType) {
        binding.configLayout.removeAllViews()
        inflateConfigView(graphStatType)
        currentConfigView.initFromConfigData(
            viewModel.configData.value,
            viewModel.featureDataProvider
        )
        listenToConfigView()
    }

    private fun inflateConfigView(graphStatType: GraphStatType) {
        gsiProvider.getConfigView(graphStatType, requireContext()).let {
            currentConfigView = it
            binding.configLayout.addView(it)
        }
    }

    private fun listenToFormValid() {
        viewModel.formValid.observe(viewLifecycleOwner, Observer { errorNow ->
            binding.addBar.addButton.isEnabled = errorNow == null
            binding.addBar.errorText.postDelayed({
                val errorThen = viewModel.formValid.value
                val text = errorThen?.let { getString(it.errorMessageId) } ?: ""
                binding.addBar.errorText.text = text
            }, 200)
        })
    }

    private fun updateDemoView(data: IGraphStatViewData?) {
        updateDemoHandler.removeCallbacksAndMessages(null)
        updateDemoHandler.postDelayed(Runnable {
            if (data == null) {
                binding.btnPreivew.visibility = View.GONE
                binding.previewOverlay.visibility = View.GONE
            } else {
                binding.btnPreivew.visibility = View.VISIBLE
                val decorator = gsiProvider.getDecorator(data.graphOrStat.type, true)
                binding.demoGraphStatCardView.graphStatView.initFromGraphStat(data, decorator)
            }
            if (viewModel.formValid.value != null) {
                binding.demoGraphStatCardView.graphStatView.initError(
                    R.string.graph_stat_view_invalid_setup
                )
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.hideKeyboard()
    }
}

enum class GraphStatInputState { INITIALIZING, SET_FOCUS, WAITING, ADDING, FINISHED }
class ValidationException(val errorMessageId: Int) : Exception()

@HiltViewModel
class GraphStatInputViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @DefaultDispatcher private val worker: CoroutineDispatcher
) : ViewModel() {

    private var graphStatGroupId: Long = -1L
    private var graphStatId: Long? = null
    private var graphStatDisplayIndex: Int? = null

    val graphName: LiveData<String> get() = _graphName
    private val _graphName = MutableLiveData("")

    val graphStatType: LiveData<GraphStatType> get() = _graphStatType
    private val _graphStatType = MutableLiveData(GraphStatType.LINE_GRAPH)

    val updateMode: LiveData<Boolean> get() = _updateMode
    private val _updateMode = MutableLiveData(false)

    val state: LiveData<GraphStatInputState> get() = _state
    private val _state = MutableLiveData(GraphStatInputState.INITIALIZING)

    internal val formValid: LiveData<ValidationException?> get() = _formValid
    private val _formValid = MutableLiveData<ValidationException?>(null)

    private var configException: ValidationException? = null
    private var subConfigException: ValidationException? = null

    val configData: LiveData<Any?> get() = _configData
    private val _configData = MutableLiveData<Any?>(null)
    private val configCache = mutableMapOf<GraphStatType, Any?>()

    val demoViewData: LiveData<IGraphStatViewData?> get() = _demoViewData
    private val _demoViewData = MutableLiveData<IGraphStatViewData?>(null)

    lateinit var featureDataProvider: FeatureDataProvider private set

    fun initViewModel(graphStatGroupId: Long, graphStatId: Long) {
        if (this.graphStatGroupId != -1L) return
        this.graphStatGroupId = graphStatGroupId
        _state.value = GraphStatInputState.INITIALIZING
        viewModelScope.launch(io) {
            val allFeatures = dataInteractor.getAllFeaturesSync()
            val allGroups = dataInteractor.getAllGroupsSync()
            val dataSourceData = allFeatures.map { feature ->
                FeatureDataProvider.DataSourceData(
                    feature,
                    dataInteractor.getLabelsForFeatureId(feature.featureId),
                    dataInteractor.getDataSampleForFeatureId(feature.featureId).dataSampleProperties
                )
            }
            featureDataProvider =
                FeatureDataProvider(dataSourceData.mapNotNull { data ->
                    val group = allGroups
                        .firstOrNull { it.id == data.feature.groupId }
                        ?: return@mapNotNull null
                    data to group
                }.toMap())
            if (graphStatId != -1L) initFromExistingGraphStat(graphStatId)
            else moveToWaiting()
        }
    }

    private suspend fun moveToWaiting() = withContext(ui) {
        _state.value = GraphStatInputState.SET_FOCUS
        _state.value = GraphStatInputState.WAITING
    }

    private suspend fun initFromExistingGraphStat(graphStatId: Long) {
        val graphStat = dataInteractor.tryGetGraphStatById(graphStatId)

        if (graphStat == null) {
            moveToWaiting()
            return
        }

        val configData = gsiProvider.getDataSourceAdapter(graphStat.type).getConfigData(graphStatId)

        configData?.first?.let {
            withContext(ui) {
                this@GraphStatInputViewModel._graphName.value = graphStat.name
                this@GraphStatInputViewModel._graphStatType.value = graphStat.type
                this@GraphStatInputViewModel.graphStatId = graphStat.id
                this@GraphStatInputViewModel.graphStatDisplayIndex = graphStat.displayIndex
                this@GraphStatInputViewModel._updateMode.value = true
                this@GraphStatInputViewModel._configData.value = configData.second
            }
        }
        withContext(ui) { moveToWaiting() }
    }

    fun setGraphName(name: String) {
        this._graphName.value = name
        validateConfiguration()
        updateDemoData()
    }

    fun setGraphType(type: GraphStatType) {
        this._configData.value = configCache[type]
        this._graphStatType.value = type
        validateConfiguration()
        updateDemoData()
    }

    internal fun onNewConfigData(config: Any?, exception: ValidationException?) {
        worker.cancelChildren()
        _configData.value = config
        configCache[_graphStatType.value!!] = config
        subConfigException = exception
        validateConfiguration()
        updateDemoData()
    }

    private fun updateDemoData() {
        if (_formValid.value == null) updateDemoViewData()
        else _demoViewData.value = null
    }

    private fun updateDemoViewData() = viewModelScope.launch(worker) {
        if (_configData.value == null) return@launch
        val graphOrStat = constructGraphOrStat()
        withContext(ui) {
            _demoViewData.value = IGraphStatViewData.loading(graphOrStat)
        }
        val demoData = gsiProvider.getDataFactory(graphOrStat.type)
            .getViewData(graphOrStat, _configData.value!!) {}
        withContext(ui) { _demoViewData.value = demoData }
    }

    private fun validateConfiguration() {
        configException = when {
            _graphName.value!!.isEmpty() -> ValidationException(R.string.graph_stat_validation_no_name)
            _graphStatType.value == null -> ValidationException(R.string.graph_stat_validation_unknown)
            else -> null
        }
        _formValid.value = configException ?: subConfigException
    }

    fun createGraphOrStat() {
        if (_state.value != GraphStatInputState.WAITING) return
        if (_configData.value == null) return
        _state.value = GraphStatInputState.ADDING
        viewModelScope.launch(io) {
            gsiProvider
                .getDataSourceAdapter(_graphStatType.value!!)
                .writeConfig(constructGraphOrStat(), _configData.value!!, _updateMode.value!!)
            withContext(ui) { _state.value = GraphStatInputState.FINISHED }
        }
    }

    private fun constructGraphOrStat() = GraphOrStat(
        graphStatId ?: 0L, graphStatGroupId, _graphName.value!!, _graphStatType.value!!,
        graphStatDisplayIndex ?: 0
    )
}
