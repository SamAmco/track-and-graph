package com.samco.trackandgraph.addtracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.TrackerHelper
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AddTrackerViewModel : DurationInputViewModel {
    val trackerName: TextFieldValue
    val trackerDescription: TextFieldValue
    val isDuration: LiveData<Boolean>
    val isLoading: LiveData<Boolean>
    val hasDefaultValue: LiveData<Boolean>
    val defaultValue: TextFieldValue
    val defaultLabel: TextFieldValue
    val createButtonEnabled: LiveData<Boolean>
    val errorText: LiveData<Int?>
    val durationNumericConversionMode: LiveData<TrackerHelper.DurationNumericConversionMode>
    val shouldShowDurationConversionModeSpinner: LiveData<Boolean>
    val isUpdateMode: LiveData<Boolean>
    val showUpdateWarningAlertDialog: LiveData<Boolean>
    val suggestionType: LiveData<TrackerSuggestionType>
    val suggestionOrder: LiveData<TrackerSuggestionOrder>
    val complete: ReceiveChannel<Unit>

    fun init(groupId: Long, existingTrackerId: Long)
    fun onTrackerNameChanged(name: TextFieldValue)
    fun onTrackerDescriptionChanged(description: TextFieldValue)
    fun onIsDurationCheckChanged(isDuration: Boolean)
    fun onHasDefaultValueChanged(hasDefaultValue: Boolean)
    fun onDefaultValueChanged(defaultValue: TextFieldValue)
    fun onDefaultLabelChanged(defaultLabel: TextFieldValue)
    fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode)
    fun onConfirmUpdate()
    fun onDismissUpdateWarningCancel()
    fun onCreateUpdateClicked()
    fun onSuggestionTypeChanged(suggestionType: TrackerSuggestionType)
    fun onSuggestionOrderChanged(suggestionOrder: TrackerSuggestionOrder)
}

@HiltViewModel
class AddTrackerViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val timerServiceInteractor: TimerServiceInteractor,
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

    override var trackerName by mutableStateOf(TextFieldValue(""))
    override var trackerDescription by mutableStateOf(TextFieldValue(""))
    override val isLoading = MutableLiveData(false)
    override val hasDefaultValue = MutableLiveData(false)
    override var defaultValue by mutableStateOf(TextFieldValue("1.0", TextRange(0, 3)))
    override var defaultLabel by mutableStateOf(TextFieldValue())
    override val isDuration = isDurationModeFlow
        .asLiveData(viewModelScope.coroutineContext)
    override val isUpdateMode: LiveData<Boolean> =
        isUpdateModeFlow.asLiveData(viewModelScope.coroutineContext)
    override val showUpdateWarningAlertDialog = MutableLiveData(false)

    override val suggestionType = MutableLiveData(TrackerSuggestionType.LABEL_ONLY)
    override val suggestionOrder = MutableLiveData(TrackerSuggestionOrder.LABEL_ASCENDING)

    override fun onTrackerNameChanged(name: TextFieldValue) {
        trackerName = name
    }

    override fun onTrackerDescriptionChanged(description: TextFieldValue) {
        trackerDescription = description
    }

    private val validationErrorFlow = snapshotFlow { trackerName }
        .map {
            when {
                it.text.isBlank() -> ValidationError.NoName
                disallowedNames?.contains(it.text) == true -> ValidationError.NameAlreadyExists
                else -> null
            }
        }

    override val createButtonEnabled = validationErrorFlow
        .map { it == null }
        .asLiveData(viewModelScope.coroutineContext)
    override val errorText = validationErrorFlow
        .map {
            when (it) {
                ValidationError.NoName -> R.string.feature_name_cannot_be_null
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

    override val complete = Channel<Unit>(1)

    private var groupId: Long = -1
    private var existingTracker: Tracker? = null
    private var initialized = false

    override fun init(groupId: Long, existingTrackerId: Long) {
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
        trackerName = TextFieldValue(tracker.name, TextRange(tracker.name.length))
        trackerDescription = TextFieldValue(tracker.description, TextRange(tracker.name.length))

        isDurationModeFlow.value = tracker.dataType == DataType.DURATION
        hasDefaultValue.value = tracker.hasDefaultValue
        val defaultValueStr = tracker.defaultValue.toString()
        if (isDurationModeFlow.value) setDurationFromDouble(tracker.defaultValue)
        else defaultValue = TextFieldValue(defaultValueStr, TextRange(defaultValueStr.length))

        defaultLabel = TextFieldValue(tracker.defaultLabel, TextRange(tracker.defaultLabel.length))
        suggestionType.value = tracker.suggestionType
        suggestionOrder.value = tracker.suggestionOrder
        isUpdateModeFlow.value = true
    }

    override fun onIsDurationCheckChanged(isDuration: Boolean) {
        this.isDurationModeFlow.value = isDuration
    }

    override fun onHasDefaultValueChanged(hasDefaultValue: Boolean) {
        this.hasDefaultValue.value = hasDefaultValue
    }

    override fun onDefaultValueChanged(defaultValue: TextFieldValue) {
        this.defaultValue = defaultValue.copy(text = defaultValue.text.asValidatedDouble())
    }

    override fun onDefaultLabelChanged(defaultLabel: TextFieldValue) {
        this.defaultLabel = defaultLabel
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
            withContext(ui) { complete.send(Unit) }
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
                withContext(ui) { complete.send(Unit) }
            }
        }
    }

    override fun onSuggestionTypeChanged(suggestionType: TrackerSuggestionType) {
        this.suggestionType.value = suggestionType
    }

    override fun onSuggestionOrderChanged(suggestionOrder: TrackerSuggestionOrder) {
        this.suggestionOrder.value = suggestionOrder
    }

    private fun getDataType() = when (isDuration.value) {
        true -> DataType.DURATION
        else -> DataType.CONTINUOUS
    }

    private fun getDefaultValue() = when (isDuration.value) {
        true -> getDurationAsDouble()
        else -> defaultValue.text.toDoubleOrNull()
    }

    private suspend fun updateTracker(existingTracker: Tracker) {
        dataInteractor.updateTracker(
            oldTracker = existingTracker,
            durationNumericConversionMode = durationNumericConversionMode.value,
            newName = trackerName.text,
            newType = getDataType(),
            hasDefaultValue = hasDefaultValue.value,
            defaultValue = getDefaultValue() ?: 1.0,
            featureDescription = trackerDescription.text,
            defaultLabel = defaultLabel.text,
            suggestionType = suggestionType.value,
            suggestionOrder = suggestionOrder.value
        )
        timerServiceInteractor.requestWidgetUpdatesForFeatureId(featureId = existingTracker.featureId)
    }

    private suspend fun addTracker() {
        val tracker = Tracker(
            id = 0L,
            name = trackerName.text,
            groupId = groupId,
            featureId = 0L,
            displayIndex = 0,
            description = trackerDescription.text,
            dataType = getDataType(),
            hasDefaultValue = hasDefaultValue.value ?: false,
            defaultValue = getDefaultValue() ?: 1.0,
            defaultLabel = defaultLabel.text,
            suggestionType = suggestionType.value ?: TrackerSuggestionType.VALUE_AND_LABEL,
            suggestionOrder = suggestionOrder.value ?: TrackerSuggestionOrder.VALUE_ASCENDING
        )
        dataInteractor.insertTracker(tracker)
    }
}