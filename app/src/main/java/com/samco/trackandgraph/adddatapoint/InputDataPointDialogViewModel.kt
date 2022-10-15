package com.samco.trackandgraph.adddatapoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject


enum class InputDataPointDialogState { LOADING, WAITING, ADDING, ADDED }

@HiltViewModel
class InputDataPointDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableLiveData(InputDataPointDialogState.LOADING)
    val state: LiveData<InputDataPointDialogState> get() = _state

    lateinit var uiStates: Map<Tracker, DataPointInputView.DataPointInputData>

    private val _trackers = MutableLiveData<List<Tracker>>(emptyList())
    val trackers: LiveData<List<Tracker>> get() = _trackers

    private val _currentTrackerIndex = MutableLiveData<Int?>(null)
    val currentTrackerIndex: MutableLiveData<Int?> get() = _currentTrackerIndex

    private var initialized = false

    fun init(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        durationSeconds: Double?
    ) {
        if (initialized) return
        initialized = true

        _state.value = InputDataPointDialogState.LOADING
        viewModelScope.launch(io) {
            val trackers = dataInteractor.getTrackersByIdsSync(trackerIds)
            val dataPointData = dataPointTimestamp?.let {
                dataInteractor.getDataPointByTimestampAndTrackerSync(trackers[0].id, it)
            }
            uiStates = getUIStatesForFeatures(trackers, dataPointData, durationSeconds)

            withContext(ui) {
                _trackers.value = trackers
                _currentTrackerIndex.value = 0
                _state.value = InputDataPointDialogState.WAITING
            }
        }
    }

    private fun getUIStatesForFeatures(
        trackers: List<Tracker>,
        dataPointData: DataPoint?,
        durationSeconds: Double?
    ): Map<Tracker, DataPointInputView.DataPointInputData> {
        val timestamp = dataPointData?.timestamp ?: OffsetDateTime.now()
        val timeFixed = dataPointData != null
        return trackers.associateWith { f ->
            val dataPointValue = when {
                dataPointData?.value != null -> dataPointData.value
                durationSeconds != null -> durationSeconds
                f.hasDefaultValue -> f.defaultValue
                f.dataType == DataType.CONTINUOUS -> 1.0
                else -> 0.0
            }
            val dataPointLabel = dataPointData?.label
                ?: if (f.hasDefaultValue) f.defaultLabel else ""
            val dataPointNote = dataPointData?.note ?: ""
            DataPointInputView.DataPointInputData(
                f,
                timestamp,
                dataPointValue,
                dataPointLabel,
                dataPointNote,
                timeFixed,
                this@InputDataPointDialogViewModel::onDateTimeSelected,
                dataPointData
            )
        }
    }

    private fun onDateTimeSelected(dateTime: OffsetDateTime) {
        uiStates.values.forEach { dp ->
            if (!dp.timeFixed) {
                dp.dateTime = dateTime
            }
        }
    }

    fun onDataPointInput(newDataPoint: DataPoint, oldDataPoint: DataPoint?) {
        if (state.value != InputDataPointDialogState.WAITING) return
        _state.value = InputDataPointDialogState.ADDING
        viewModelScope.launch(io) {
            if (oldDataPoint != newDataPoint) {
                if (oldDataPoint != null) dataInteractor.deleteDataPoint(oldDataPoint)
                dataInteractor.insertDataPoint(newDataPoint)
            }
            withContext(ui) { _state.value = InputDataPointDialogState.ADDED }
        }
    }

    fun onFinishedTransition() {
        _state.value = InputDataPointDialogState.WAITING
    }
}