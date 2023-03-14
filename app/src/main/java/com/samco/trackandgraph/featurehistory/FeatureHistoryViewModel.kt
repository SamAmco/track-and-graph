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
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface FeatureHistoryNavigationViewModel {
    fun initViewModel(featureId: Long)

    data class EditDataPointData(
        val timestamp: OffsetDateTime,
        val trackerId: Long
    )

    val showEditDataPointDialog: LiveData<EditDataPointData?>

    fun showEditDataPointDialogComplete()

    fun showUpdateAllDialog()
}

interface FeatureHistoryViewModel : UpdateDialogViewModel {
    val isTracker: LiveData<Boolean>
    val dataPoints: LiveData<List<DataPoint>>
    val showFeatureInfo: LiveData<Feature?>
    val showDataPointInfo: LiveData<DataPoint?>
    val showDeleteConfirmDialog: LiveData<Boolean>
    val showUpdateDialog: LiveData<Boolean>

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
    @MainDispatcher private val ui: CoroutineDispatcher
) : UpdateDialogViewModelImpl(),
    FeatureHistoryViewModel,
    FeatureHistoryNavigationViewModel {
    private val featureIdFlow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 1)

    private data class RawData(
        val dataSampleProperties: DataSampleProperties,
        val dataPoints: List<DataPoint>
    )

    private val dataSample =
        combine(
            featureIdFlow,
            dataInteractor.getDataUpdateEvents().onStart { emit(Unit) }) { id, _ -> id }
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

    override val isTracker: LiveData<Boolean> = trackerFlow
        .map { it != null }
        .asLiveData(viewModelScope.coroutineContext)

    private var featureId: Long? = null

    private val editDataPoint = MutableStateFlow<OffsetDateTime?>(null)

    override fun initViewModel(featureId: Long) {
        if (this.featureId != null) return
        this.featureId = featureId
        viewModelScope.launch(io) { featureIdFlow.emit(featureId) }
    }

    override val showEditDataPointDialog = combine(
        trackerFlow.filterNotNull(),
        editDataPoint
    ) { tracker, timestamp ->
        timestamp?.let {
            FeatureHistoryNavigationViewModel.EditDataPointData(
                it,
                tracker.id
            )
        }
    }.asLiveData(viewModelScope.coroutineContext)

    override fun showEditDataPointDialogComplete() {
        editDataPoint.value = null
    }

    override fun onEditClicked(dataPoint: DataPoint) {
        editDataPoint.value = dataPoint.timestamp
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