package com.samco.trackandgraph.addtracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.DataType
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

    var trackerName = ""
    var trackerDescription = ""
    val dataType = MutableLiveData(DataType.CONTINUOUS)
    val durationNumericConversionMode =
        MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
    val trackerHasDefaultValue = MutableLiveData(false)
    val trackerDefaultValue = MutableLiveData(1.0)
    lateinit var disallowedNames: List<String>
        private set

    private val _state = MutableLiveData(AddTrackerState.INITIALIZING)
    val state: LiveData<AddTrackerState> get() = _state

    var updateMode: Boolean = false
        private set
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
                withContext(ui) {
                    trackerName = existingTracker!!.name
                    trackerDescription = existingTracker!!.description
                    dataType.value = existingTracker!!.dataType
                    trackerHasDefaultValue.value = existingTracker!!.hasDefaultValue
                    trackerDefaultValue.value = existingTracker!!.defaultValue
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
        dataInteractor.updateTracker(
            oldTracker = existingTracker!!,
            durationNumericConversionMode = durationNumericConversionMode.value,
            newName = trackerName,
            newType = dataType.value,
            hasDefaultValue = trackerHasDefaultValue.value,
            defaultValue = trackerDefaultValue.value,
            featureDescription = trackerDescription,
            defaultLabel = ""//TODO implement default label
        )
    }

    private suspend fun addTracker() {
        val tracker = Tracker(
            id = 0L,
            name = trackerName,
            groupId = groupId,
            featureId = 0L,
            displayIndex = 0,
            description = trackerDescription,
            dataType = dataType.value!!,
            hasDefaultValue = trackerHasDefaultValue.value!!,
            defaultValue = trackerDefaultValue.value!!,
            defaultLabel = ""//TODO implement default label
        )
        dataInteractor.insertTracker(tracker)
    }
}