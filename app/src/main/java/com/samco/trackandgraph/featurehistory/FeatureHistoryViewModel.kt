@file:OptIn(FlowPreview::class)

package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.database.sampling.DataSampleProperties
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface FeatureHistoryNavigationViewModel {
    fun initViewModel(featureId: Long)

    fun showUpdateAllDialog()
}

interface FeatureHistoryViewModel : UpdateDialogViewModel {
    val tracker: LiveData<Tracker?>
    val dataPoints: LiveData<List<DataPoint>>
    val showFeatureInfo: LiveData<Feature?>
    val showDataPointInfo: LiveData<DataPoint?>
    val showDeleteConfirmDialog: LiveData<Boolean>
    val showUpdateDialog: LiveData<Boolean>

    fun onDeleteClicked(dataPoint: DataPoint)
    fun onDeleteConfirmed()
    fun onDeleteDismissed()
    fun onDataPointClicked(dataPoint: DataPoint)
    fun onDismissDataPoint()
    fun onShowFeatureInfo()
    fun onHideFeatureInfo()
}

@HiltViewModel
class FeatureHistoryViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : UpdateDialogViewModelImpl(),
    FeatureHistoryViewModel,
    FeatureHistoryNavigationViewModel {
    private val featureIdFlow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 1)

    private data class RawData(
        val dataSampleProperties: DataSampleProperties,
        val dataPoints: List<DataPoint>
    )

    private val dataUpdates = dataInteractor
        .getDataUpdateEvents()
        .map { }
        .debounce(10)
        .onStart { emit(Unit) }

    private val dataSample =
        combine(featureIdFlow, dataUpdates) { id, _ -> id }
            .map { featureId ->
                val dataSample = dataInteractor.getDataSampleForFeatureId(featureId)
                val answer = RawData(
                    dataSample.dataSampleProperties,
                    dataSample.getAllRawDataPoints()
                        .sortedByDescending { it.timestamp.toEpochSecond() }
                )
                dataSample.dispose()
                return@map answer
            }
            .flowOn(io)

    override val isDuration: LiveData<Boolean> = dataSample.map {
        it.dataSampleProperties.isDuration
    }.asLiveData(viewModelScope.coroutineContext)

    override val dataPoints: LiveData<List<DataPoint>> = dataSample.map {
        it.dataPoints
    }.asLiveData(viewModelScope.coroutineContext)

    private val showFeatureInfoFlow = MutableStateFlow(false)

    override val showFeatureInfo: LiveData<Feature?> = combine(
        showFeatureInfoFlow,
        featureIdFlow.map { dataInteractor.getFeatureById(it) }) { show, feature ->
        if (show) feature else null
    }.asLiveData(viewModelScope.coroutineContext)

    override val showDataPointInfo = MutableLiveData<DataPoint?>(null)

    private val confirmDeleteDataPoint = MutableStateFlow<DataPoint?>(null)

    override val showDeleteConfirmDialog = confirmDeleteDataPoint
        .map { it != null }
        .asLiveData(viewModelScope.coroutineContext)

    override val showUpdateDialog = MutableLiveData(false)

    private val trackerFlow: StateFlow<Tracker?> = featureIdFlow
        .map { dataInteractor.getTrackerByFeatureId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val tracker: LiveData<Tracker?> = trackerFlow
        .asLiveData(viewModelScope.coroutineContext)

    private var featureId: Long? = null

    override fun initViewModel(featureId: Long) {
        if (this.featureId != null) return
        this.featureId = featureId
        viewModelScope.launch(io) { featureIdFlow.emit(featureId) }
    }

    override fun onDeleteClicked(dataPoint: DataPoint) {
        confirmDeleteDataPoint.value = dataPoint
    }

    override fun onDeleteConfirmed() {
        viewModelScope.launch(io) {
            confirmDeleteDataPoint.value?.let {
                dataInteractor.deleteDataPoint(it)
            }
            confirmDeleteDataPoint.value = null
        }
    }

    override fun onDeleteDismissed() {
        confirmDeleteDataPoint.value = null
    }

    override fun onDataPointClicked(dataPoint: DataPoint) {
        showFeatureInfoFlow.value = false
        showDataPointInfo.value = dataPoint
    }

    override fun onDismissDataPoint() {
        showDataPointInfo.value = null
    }

    override fun onShowFeatureInfo() {
        showDataPointInfo.value = null
        showFeatureInfoFlow.value = true
    }

    override fun onHideFeatureInfo() {
        showFeatureInfoFlow.value = false
    }

    override val isUpdating = MutableLiveData(false)

    override fun showUpdateAllDialog() {
        showUpdateDialog.value = true
    }

    override fun onConfirmUpdateWarning() {
        showUpdateWarning.value = false
        showUpdateDialog.value = false
        viewModelScope.launch(io) {
            withContext(ui) { isUpdating.value = true }
            trackerFlow.value?.let {
                dataInteractor.updateDataPoints(
                    trackerId = it.id,
                    whereValue = getWhereValueDouble(),
                    whereLabel = getWhereLabelString(),
                    toValue = getToValueDouble(),
                    toLabel = getToLabelString()
                )
            }
            withContext(ui) { isUpdating.value = false }
        }
    }

    override fun onCancelUpdate() {
        showUpdateDialog.value = false
        showUpdateWarning.value = false
    }
}