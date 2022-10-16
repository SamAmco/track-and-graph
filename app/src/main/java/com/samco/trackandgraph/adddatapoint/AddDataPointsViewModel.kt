package com.samco.trackandgraph.adddatapoint

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModelImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface AddDataPointBaseViewModel {
    //TODO need a list of quick track buttons and a function for pressing one
    val name: LiveData<String>
    val timestamp: LiveData<OffsetDateTime>
    val label: LiveData<String>
    val note: LiveData<String>

    fun getTracker(): Tracker
    fun updateLabel(label: String)
    fun updateNote(note: String)
    fun updateTimestamp(timestamp: OffsetDateTime)
}

sealed interface AddDataPointViewModel : AddDataPointBaseViewModel {
    interface NumericalDataPointViewModel : AddDataPointViewModel {
        val value: LiveData<Double?>
        fun setValue(value: Double)
    }

    interface DurationDataPointViewModel : AddDataPointViewModel, DurationInputViewModel
}

interface AddDataPointsViewModel {

    val updateMode: LiveData<Boolean>
    val indexText: LiveData<String>
    val skipButtonVisible: LiveData<Boolean>
    val dataPointPages: LiveData<Int>
    val currentPageIndex: LiveData<Int>

    fun getViewModel(pageIndex: Int): LiveData<AddDataPointViewModel>

    fun onCancelClicked()
    fun onSkipClicked()
    fun onAddClicked()
    fun updateCurrentPage(page: Int)
}

interface AddDataPointsNavigationViewModel : AddDataPointsViewModel {
    val dismiss: LiveData<Boolean>

    fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        customInitialValue: Double?
    )
}

@HiltViewModel
class AddDataPointsViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), AddDataPointsNavigationViewModel {

    private data class Config(
        val tracker: Tracker,
        val timestamp: OffsetDateTime?,
        val label: String?,
        val value: Double?,
        val note: String?
    )

    private val configFlow = MutableStateFlow<List<Config>>(emptyList())
    private val indexFlow = MutableStateFlow(0)
    override val dismiss = MutableLiveData(false)

    override val dataPointPages = configFlow
        .map { it.size }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

    override val currentPageIndex = indexFlow
        .asLiveData(viewModelScope.coroutineContext)

    private val viewModels: SharedFlow<List<AddDataPointViewModel>> = configFlow
        .map { it.map { config -> getViewModel(config) } }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    override fun getViewModel(pageIndex: Int) = viewModels
        .map { it.getOrNull(pageIndex) }
        .filterNotNull()
        .asLiveData(viewModelScope.coroutineContext)

    private fun getViewModel(config: Config): AddDataPointViewModel {
        return when (config.tracker.dataType) {
            DataType.DURATION -> getDurationViewModel(config)
            DataType.CONTINUOUS -> getNumericalViewModel(config)
        }
    }

    private val now = OffsetDateTime.now()

    private fun getBaseViewModel(config: Config) = object : AddDataPointBaseViewModel {
        override val name = MutableLiveData(config.tracker.name)
        override val timestamp = MutableLiveData(config.timestamp ?: now)
        override val label = MutableLiveData(config.label ?: "")
        override val note = MutableLiveData(config.note ?: "")

        override fun getTracker() = config.tracker

        override fun updateLabel(label: String) {
            this.label.value = label
        }

        override fun updateNote(note: String) {
            this.note.value = note
        }

        override fun updateTimestamp(timestamp: OffsetDateTime) {
            this.timestamp.value = timestamp
        }
    }

    private fun getNumericalViewModel(config: Config) =
        object : AddDataPointViewModel.NumericalDataPointViewModel,
            AddDataPointBaseViewModel by getBaseViewModel(config) {
            override val value = MutableLiveData(config.value)

            override fun setValue(value: Double) {
                this.value.value = value
            }
        }

    private fun getDurationViewModel(config: Config) =
        object : DurationInputViewModel by DurationInputViewModelImpl(),
            AddDataPointBaseViewModel by getBaseViewModel(config),
            AddDataPointViewModel.DurationDataPointViewModel {}

    override val updateMode: LiveData<Boolean> = configFlow.map {
        it.size == 1 && it[0].timestamp != null
    }.asLiveData(viewModelScope.coroutineContext)

    override val indexText = combine(indexFlow, configFlow) { index, configs ->
        if (configs.size == 1) ""
        else "${index + 1} / ${configs.size}"
    }.asLiveData(viewModelScope.coroutineContext)

    override val skipButtonVisible = combine(indexFlow, configFlow) { index, configs ->
        if (configs.size == 1) false
        else index < configs.size
    }.asLiveData(viewModelScope.coroutineContext)

    private var initialized = false

    override fun onCancelClicked() {
        dismiss.value = true
    }

    override fun onSkipClicked() = incrementPageIndex()

    override fun onAddClicked() {
        viewModelScope.launch {
            combine(indexFlow, viewModels) { index, viewModels -> viewModels.getOrNull(index) }
                .take(1)
                .filterNotNull()
                .collect { viewModel ->

                    val doubleValue = when (viewModel) {
                        is AddDataPointViewModel.NumericalDataPointViewModel ->
                            viewModel.value.value ?: 1.0
                        is AddDataPointViewModel.DurationDataPointViewModel ->
                            viewModel.getDurationAsDouble()
                    }

                    dataInteractor.insertDataPoint(
                        DataPoint(
                            timestamp = viewModel.timestamp.value ?: return@collect,
                            featureId = viewModel.getTracker().featureId,
                            value = doubleValue,
                            label = viewModel.label.value ?: "",
                            note = viewModel.note.value ?: ""
                        )
                    )
                    incrementPageIndex()
                }
        }
    }

    override fun updateCurrentPage(page: Int) {
        setPageIndex(page)
    }

    private fun incrementPageIndex() {
        if (!setPageIndex(indexFlow.value + 1)) dismiss.value = true
    }

    private fun setPageIndex(index: Int): Boolean {
        return if (index >= 0 && index < configFlow.value.size) {
            indexFlow.value = index
            true
        } else false
    }

    override fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        customInitialValue: Double?
    ) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {

            val configs = trackerIds
                .mapNotNull { dataInteractor.getTrackerById(it) }
                .map { tracker ->
                    val dataPoint = dataPointTimestamp?.let { timestamp ->
                        dataInteractor.getDataPointByTimestampAndTrackerSync(
                            tracker.id,
                            timestamp
                        )
                    }
                    Config(
                        tracker,
                        dataPointTimestamp,
                        dataPoint?.label,
                        customInitialValue ?: dataPoint?.value,
                        dataPoint?.note
                    )
                }

            if (configs.isEmpty()) dismiss.value = true
            else configFlow.emit(configs)
        }
    }
}