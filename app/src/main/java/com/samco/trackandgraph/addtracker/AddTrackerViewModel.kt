package com.samco.trackandgraph.addtracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DiscreteValue
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.TrackerHelper
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject

enum class AddTrackerState { INITIALIZING, SET_FOCUS, WAITING, ADDING, DONE, ERROR }

@HiltViewModel
class AddTrackerViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    class MutableLabel(var label: String = "", val updateIndex: Int = -1)

    var trackerName = ""
    var trackerDescription = ""
    val dataType = MutableLiveData(DataType.DISCRETE)
    val durationNumericConversionMode =
        MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
    val trackerHasDefaultValue = MutableLiveData(false)
    val trackerDefaultValue = MutableLiveData(1.0)
    val discreteValues = mutableListOf<MutableLabel>()
    lateinit var disallowedNames: List<String>
        private set

    private val _state = MutableLiveData(AddTrackerState.INITIALIZING)
    val state: LiveData<AddTrackerState> get() = _state

    var updateMode: Boolean = false
        private set
    var haveWarnedAboutDeletingDiscreteValues = false
    private var groupId: Long = -1
    var existingTracker: Tracker? = null
        private set
    private var initialized = false

    fun init(groupId: Long, existingTrackerId: Long) {
        if (initialized) return
        initialized = true

        this.groupId = groupId
        viewModelScope.launch(io) {
            if (existingTrackerId > -1) {
                updateMode = true
                existingTracker = dataInteractor.getTrackerById(existingTrackerId)
                val existingDiscreteValues = existingTracker!!.discreteValues
                    .sortedBy { f -> f.index }
                    .map { f -> MutableLabel(f.label, f.index) }
                withContext(ui) {
                    trackerName = existingTracker!!.name
                    trackerDescription = existingTracker!!.description
                    dataType.value = existingTracker!!.dataType
                    trackerHasDefaultValue.value = existingTracker!!.hasDefaultValue
                    trackerDefaultValue.value = existingTracker!!.defaultValue
                    discreteValues.addAll(existingDiscreteValues)
                }
            }
            disallowedNames = dataInteractor
                .getFeaturesForGroupSync(groupId)
                .map { it.name }
                .filter { it != existingTracker?.name }

            withContext(ui) {
                _state.value = AddTrackerState.SET_FOCUS
                _state.value = AddTrackerState.WAITING
            }
        }
    }

    fun isFeatureTypeEnabled(type: DataType): Boolean {
        if (!updateMode) return true
        // disc -> cont Y
        // disc -> dur N
        // cont -> disc N
        // cont -> dur Y
        // dur -> disc N
        // dur -> cont Y
        return when (type) {
            DataType.DISCRETE -> existingTracker!!.dataType == DataType.DISCRETE
            DataType.CONTINUOUS -> existingTracker!!.dataType == DataType.DURATION
                    || existingTracker!!.dataType == DataType.DISCRETE
                    || existingTracker!!.dataType == DataType.CONTINUOUS
            DataType.DURATION -> existingTracker!!.dataType == DataType.CONTINUOUS
                    || existingTracker!!.dataType == DataType.DURATION
        }
    }

    fun onAddOrUpdate() {
        _state.value = AddTrackerState.ADDING
        viewModelScope.launch(io) {
            try {
                if (updateMode) updateTracker()
                else addTracker()
                withContext(ui) { _state.value = AddTrackerState.DONE }
            } catch (e: Exception) {
                withContext(ui) { _state.value = AddTrackerState.ERROR }
            }
        }
    }

    private suspend fun updateTracker() {
        val existingDiscreteValues = existingTracker!!.discreteValues

        val newDiscVals = discreteValues.associateWith { mutableLabel ->
            val index = if (discreteValues.size == 1) 1 else discreteValues.indexOf(mutableLabel)
            DiscreteValue(index, mutableLabel.label)
        }

        val discValMap = existingDiscreteValues
            .filter { dv -> discreteValues.map { it.updateIndex }.contains(dv.index) }
            .associateWith {
                newDiscVals.getOrElse(discreteValues.first { new -> new.updateIndex == it.index }) {
                    throw Exception("Could not find discrete value update")
                }
            }

        dataInteractor.updateTracker(
            oldTracker = existingTracker!!,
            discreteValueMap = discValMap,
            durationNumericConversionMode = durationNumericConversionMode.value,
            newName = trackerName,
            newType = dataType.value,
            newDiscreteValues = newDiscVals.values.toList(),
            hasDefaultValue = trackerHasDefaultValue.value,
            defaultValue = trackerDefaultValue.value,
            featureDescription = trackerDescription
        )
    }

    private suspend fun addTracker() {
        val discVals = discreteValues.mapIndexed { i, s ->
            val index = if (discreteValues.size == 1) 1 else i
            DiscreteValue(index, s.label)
        }

        val tracker = Tracker(
            id = 0L,
            name = trackerName,
            groupId = groupId,
            featureId = 0L,
            displayIndex = 0,
            description = trackerDescription,
            dataType = dataType.value!!,
            discreteValues = discVals,
            hasDefaultValue = trackerHasDefaultValue.value!!,
            defaultValue = trackerDefaultValue.value!!
        )
        dataInteractor.insertTracker(tracker)
    }
}