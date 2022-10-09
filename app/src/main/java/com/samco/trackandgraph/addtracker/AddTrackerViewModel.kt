package com.samco.trackandgraph.addtracker

import androidx.lifecycle.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.TrackerHelper
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModelImpl
import com.samco.trackandgraph.ui.compose.viewmodels.asValidatedDouble
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AddTrackerViewModel : DurationInputViewModel {
    //Outputs
    val trackerName: LiveData<String>
    val trackerDescription: LiveData<String>
    val isDuration: LiveData<Boolean>
    val isLoading: LiveData<Boolean>
    val hasDefaultValue: LiveData<Boolean>
    val defaultValue: LiveData<String>
    val defaultLabel: LiveData<String>
    val createButtonEnabled: LiveData<Boolean>
    val errorText: LiveData<Int?>
    val durationNumericConversionMode: LiveData<TrackerHelper.DurationNumericConversionMode>
    val shouldShowDurationConversionModeSpinner: LiveData<Boolean>
    val isUpdateMode: LiveData<Boolean>
    val showUpdateWarningAlertDialog: LiveData<Boolean>

    //Inputs
    fun onTrackerNameChanged(name: String)
    fun onTrackerDescriptionChanged(description: String)
    fun onIsDurationCheckChanged(isDuration: Boolean)
    fun onHasDefaultValueChanged(hasDefaultValue: Boolean)
    fun onDefaultValueChanged(defaultValue: String)
    fun onDefaultLabelChanged(defaultLabel: String)
    fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode)
    fun onConfirmUpdate()
    fun onDismissUpdateWarningCancel()
    fun onCreateUpdateClicked()
}

@HiltViewModel
class AddTrackerViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
) : ViewModel(), AddTrackerViewModel, DurationInputViewModel by DurationInputViewModelImpl() {

    private var disallowedNames: List<String>? = null

    private sealed interface ValidationError {
        object NoName : ValidationError
        object NameAlreadyExists : ValidationError
    }

    private val isUpdateModeFlow = MutableStateFlow(false)
    private val isDurationModeFlow = MutableStateFlow(false)

    private val trackerNameFlow = MutableStateFlow("")
    override val trackerName: LiveData<String> =
        trackerNameFlow.asLiveData(viewModelScope.coroutineContext)
    override val trackerDescription = MutableLiveData("")
    override val isLoading = MutableLiveData(false)
    override val hasDefaultValue = MutableLiveData(false)
    override val defaultValue = MutableLiveData("1.0")
    override val defaultLabel = MutableLiveData("")
    override val isDuration = isDurationModeFlow
        .asLiveData(viewModelScope.coroutineContext)
    override val isUpdateMode: LiveData<Boolean> =
        isUpdateModeFlow.asLiveData(viewModelScope.coroutineContext)
    override val showUpdateWarningAlertDialog = MutableLiveData(false)

    private val validationErrorFlow = trackerNameFlow.map {
        when {
            it.isBlank() -> ValidationError.NoName
            disallowedNames?.contains(it) == true -> ValidationError.NameAlreadyExists
            else -> null
        }
    }
    override val createButtonEnabled = validationErrorFlow
        .map { it == null }
        .asLiveData(viewModelScope.coroutineContext)
    override val errorText = validationErrorFlow
        .map {
            when (it) {
                ValidationError.NoName -> R.string.tracker_name_cannot_be_null
                ValidationError.NameAlreadyExists -> R.string.tracker_with_that_name_exists
                else -> null
            }
        }
        .asLiveData(viewModelScope.coroutineContext)
    override val durationNumericConversionMode =
        MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
    override val shouldShowDurationConversionModeSpinner: LiveData<Boolean> =
        combine(isUpdateModeFlow, isDurationModeFlow) { a, b ->
            //If we're in update mode and we've changed to or from duration we need to show
            // a conversion mode spinner
            a && (existingTracker?.dataType == DataType.DURATION) != b
        }.asLiveData(viewModelScope.coroutineContext)
    val complete = MutableLiveData(false)

    private var groupId: Long = -1
    private var existingTracker: Tracker? = null
    private var initialized = false

    fun init(groupId: Long, existingTrackerId: Long) {
        if (initialized) return
        initialized = true

        this.groupId = groupId
        viewModelScope.launch(io) {
            withContext(ui) { isLoading.value = true }
            dataInteractor.getTrackerById(existingTrackerId)?.let {
                initFromTracker(it)
            }
            disallowedNames = dataInteractor
                .getFeaturesForGroupSync(groupId)
                .map { it.name }
                .filter { it != existingTracker?.name }
            withContext(ui) { isLoading.value = false }
        }
    }

    private suspend fun initFromTracker(tracker: Tracker) = withContext(ui) {
        existingTracker = tracker
        trackerNameFlow.value = tracker.name
        trackerDescription.value = tracker.description
        isDurationModeFlow.value = tracker.dataType == DataType.DURATION
        hasDefaultValue.value = tracker.hasDefaultValue
        defaultValue.value = tracker.defaultValue.toString()
        defaultLabel.value = tracker.defaultLabel
        isUpdateModeFlow.value = true
    }

    override fun onTrackerNameChanged(name: String) {
        viewModelScope.launch(ui) {
            trackerNameFlow.emit(name)
        }
    }

    override fun onTrackerDescriptionChanged(description: String) {
        trackerDescription.value = description
    }

    override fun onIsDurationCheckChanged(isDuration: Boolean) {
        this.isDurationModeFlow.value = isDuration
    }

    override fun onHasDefaultValueChanged(hasDefaultValue: Boolean) {
        this.hasDefaultValue.value = hasDefaultValue
    }

    override fun onDefaultValueChanged(defaultValue: String) {
        this.defaultValue.value = defaultValue.asValidatedDouble()
    }

    override fun onDefaultLabelChanged(defaultLabel: String) {
        this.defaultLabel.value = defaultLabel
    }

    override fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode) {
        this.durationNumericConversionMode.value = durationNumericConversionMode
    }

    override fun onConfirmUpdate() {
        showUpdateWarningAlertDialog.value = false
        viewModelScope.launch(io) {
            withContext(ui) { isLoading.value = true }
            //should not be null here but just in case lets fallback to add
            existingTracker?.let { updateTracker(it) } ?: addTracker()
            withContext(ui) { isLoading.value = false }
            withContext(ui) { complete.value = true }
        }
    }

    override fun onDismissUpdateWarningCancel() {
        showUpdateWarningAlertDialog.value = false
    }

    override fun onCreateUpdateClicked() {
        if (isUpdateModeFlow.value) {
            showUpdateWarningAlertDialog.value = true
        } else {
            viewModelScope.launch(io) {
                withContext(ui) { isLoading.value = true }
                addTracker()
                withContext(ui) { isLoading.value = false }
                withContext(ui) { complete.value = true }
            }
        }
    }

    private fun getDataType() = when (isDuration.value) {
        true -> DataType.DURATION
        else -> DataType.CONTINUOUS
    }

    private fun getDefaultValue() = when (isDuration.value) {
        true -> getDurationAsDouble()
        else -> defaultValue.value?.toDoubleOrNull()
    }

    private suspend fun updateTracker(existingTracker: Tracker) {
        dataInteractor.updateTracker(
            oldTracker = existingTracker,
            durationNumericConversionMode = durationNumericConversionMode.value,
            newName = trackerName.value,
            newType = getDataType(),
            hasDefaultValue = hasDefaultValue.value,
            defaultValue = getDefaultValue(),
            featureDescription = trackerDescription.value,
            defaultLabel = defaultLabel.value
        )
    }

    private suspend fun addTracker() {
        val tracker = Tracker(
            id = 0L,
            name = trackerName.value ?: "",
            groupId = groupId,
            featureId = 0L,
            displayIndex = 0,
            description = trackerDescription.value ?: "",
            dataType = getDataType(),
            hasDefaultValue = hasDefaultValue.value ?: false,
            defaultValue = getDefaultValue() ?: 1.0,
            defaultLabel = defaultLabel.value ?: ""
        )
        dataInteractor.insertTracker(tracker)
    }
}