package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.sampling.DataSampleProperties
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface FeatureHistoryNavigationViewModel {
    fun initViewModel(featureId: Long)
}

interface FeatureHistoryViewModel {
    val isDuration: LiveData<Boolean>
    val isTracker: LiveData<Boolean>
    val dataPoints: LiveData<List<DataPoint>>
    val showFeatureInfo: LiveData<Feature?>
    val showDataPointInfo: LiveData<DataPoint?>
    val showDeleteConfirmDialog: LiveData<Boolean>

    fun onEditClicked(dataPoint: DataPoint)
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
) : ViewModel(), FeatureHistoryViewModel, FeatureHistoryNavigationViewModel {
    private val featureIdFlow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 1)

    private data class RawData(
        val dataSampleProperties: DataSampleProperties,
        val dataPoints: List<DataPoint>
    )

    private val dataSample =
        combine(
            featureIdFlow,
            dataInteractor.getDataUpdateEvents().onStart { emit(Unit) }) { id, _ -> id }
            .map {
                val dataSample = dataInteractor.getDataSampleForFeatureId(it)
                dataSample.iterator().apply { while (hasNext()) next() }
                val answer = RawData(dataSample.dataSampleProperties, dataSample.getRawDataPoints())
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

    override val isTracker: LiveData<Boolean> = featureIdFlow
        .map { dataInteractor.getTrackerByFeatureId(it) != null }
        .asLiveData(viewModelScope.coroutineContext)

    private var featureId: Long? = null

    override fun initViewModel(featureId: Long) {
        if (this.featureId != null) return
        this.featureId = featureId
        viewModelScope.launch(io) { featureIdFlow.emit(featureId) }
    }

    override fun onEditClicked(dataPoint: DataPoint) {
        //TODO("Not yet implemented")
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
}