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

package com.samco.trackandgraph.adddatapoint

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.settings.mockSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.AddChipButton
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.DurationInputView
import com.samco.trackandgraph.ui.compose.ui.FadingLazyRow
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.TextChip
import com.samco.trackandgraph.ui.compose.ui.TrackerNameHeadline
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.threeten.bp.OffsetDateTime

// Data classes for state representation
@Immutable
internal data class TrackerPageState(
    val name: String = "",
    val timestamp: OffsetDateTime? = null,
    val suggestedValues: List<SuggestedValueViewData>? = null,
    val currentValueAsSuggestion: SuggestedValueViewData? = null,
    val label: TextFieldValue = TextFieldValue(),
    val note: TextFieldValue = TextFieldValue(),
    val trackerType: TrackerType = TrackerType.NUMERICAL,
    val value: TextFieldValue = TextFieldValue(), // For numerical
    // Duration fields (from DurationInputViewModel interface)
    val hours: TextFieldValue = TextFieldValue(),
    val minutes: TextFieldValue = TextFieldValue(),
    val seconds: TextFieldValue = TextFieldValue()
)

internal enum class TrackerType {
    NUMERICAL, DURATION
}

// Callback interfaces
internal interface TrackerPageCallbacks {
    fun onTimestampChanged(timestamp: OffsetDateTime)
    fun onSuggestedValueSelected(suggestedValue: SuggestedValueViewData)
    fun onSuggestedValueLongPress(suggestedValue: SuggestedValueViewData)
    fun onLabelChanged(label: TextFieldValue)
    fun onNoteChanged(note: TextFieldValue)
    fun onValueChanged(value: TextFieldValue)
    fun onHoursChanged(hours: TextFieldValue)
    fun onMinutesChanged(minutes: TextFieldValue)
    fun onSecondsChanged(seconds: TextFieldValue)
    fun onAddDataPoint()
}

@Composable
internal fun TrackerPage(
    modifier: Modifier,
    viewModel: AddDataPointViewModel,
    currentPage: Boolean,
    suggestedValues: List<SuggestedValueViewData>?,
    valueFocusRequester: FocusRequester? = null,
) {
    // Extract state from ViewModel
    val name by viewModel.name.observeAsState("")
    val timestamp by viewModel.timestamp.observeAsState(null)
    val selectedSuggestedValue by viewModel.currentValueAsSuggestion.observeAsState()
    val label = viewModel.label
    val note = viewModel.note

    // Create callbacks
    val callbacks = remember(viewModel) {
        object : TrackerPageCallbacks {
            override fun onTimestampChanged(timestamp: OffsetDateTime) = viewModel.updateTimestamp(timestamp)
            override fun onSuggestedValueSelected(suggestedValue: SuggestedValueViewData) =
                viewModel.onSuggestedValueSelected(suggestedValue)

            override fun onSuggestedValueLongPress(suggestedValue: SuggestedValueViewData) =
                viewModel.onSuggestedValueLongPress(suggestedValue)

            override fun onLabelChanged(label: TextFieldValue) = viewModel.updateLabel(label)
            override fun onNoteChanged(note: TextFieldValue) = viewModel.updateNote(note)
            override fun onValueChanged(value: TextFieldValue) {
                if (viewModel is AddDataPointViewModel.NumericalDataPointViewModel) {
                    viewModel.setValueText(value)
                }
            }

            override fun onHoursChanged(hours: TextFieldValue) {
                if (viewModel is AddDataPointViewModel.DurationDataPointViewModel) {
                    viewModel.setHoursText(hours)
                }
            }

            override fun onMinutesChanged(minutes: TextFieldValue) {
                if (viewModel is AddDataPointViewModel.DurationDataPointViewModel) {
                    viewModel.setMinutesText(minutes)
                }
            }

            override fun onSecondsChanged(seconds: TextFieldValue) {
                if (viewModel is AddDataPointViewModel.DurationDataPointViewModel) {
                    viewModel.setSecondsText(seconds)
                }
            }

            override fun onAddDataPoint() = viewModel.addDataPoint()
        }
    }

    // Create state object
    val trackerType = remember(viewModel) {
        when (viewModel) {
            is AddDataPointViewModel.NumericalDataPointViewModel -> TrackerType.NUMERICAL
            is AddDataPointViewModel.DurationDataPointViewModel -> TrackerType.DURATION
            else -> TrackerType.NUMERICAL
        }
    }

    val value = if (viewModel is AddDataPointViewModel.NumericalDataPointViewModel) {
        viewModel.value
    } else remember(viewModel) { TextFieldValue() }

    // Extract duration fields if it's a duration tracker
    val (hours, minutes, seconds) =
        if (viewModel is AddDataPointViewModel.DurationDataPointViewModel) {
            Triple(viewModel.hours, viewModel.minutes, viewModel.seconds)
        } else {
            remember(viewModel) { Triple(TextFieldValue(), TextFieldValue(), TextFieldValue()) }
        }

    val state = TrackerPageState(
        name = name,
        timestamp = timestamp,
        suggestedValues = suggestedValues,
        currentValueAsSuggestion = selectedSuggestedValue,
        label = label,
        note = note,
        trackerType = trackerType,
        value = value,
        hours = hours,
        minutes = minutes,
        seconds = seconds
    )

    TrackerPageView(
        modifier = modifier,
        state = state,
        isCurrentPage = currentPage,
        callbacks = callbacks,
        valueFocusRequester = valueFocusRequester,
    )
}

@Composable
internal fun TrackerPageView(
    modifier: Modifier = Modifier,
    state: TrackerPageState,
    isCurrentPage: Boolean,
    callbacks: TrackerPageCallbacks,
    valueFocusRequester: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current

    val shouldFocusValue = remember(state.suggestedValues) {
        state.suggestedValues != null && state.suggestedValues.all { it.value == null }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TrackerNameHeadline(name = state.name)

        DialogInputSpacing()

        if (state.timestamp != null) {
            DateTimeButtonRow(
                modifier = Modifier.fillMaxWidth(),
                selectedDateTime = state.timestamp,
                onDateTimeSelected = callbacks::onTimestampChanged
            )
        }

        DialogInputSpacing()

        if (state.suggestedValues?.isNotEmpty() == true) {
            SuggestedValues(
                list = state.suggestedValues,
                selectedItem = state.currentValueAsSuggestion,
                onSuggestedValueSelected = callbacks::onSuggestedValueSelected,
                onSuggestedValueLongPress = callbacks::onSuggestedValueLongPress
            )
            DialogInputSpacing()
        }

        // Track whether user has explicitly added label/note fields (separate from content)
        var labelFieldAdded by rememberSaveable { mutableStateOf(state.label.text.isNotEmpty()) }
        var noteFieldAdded by rememberSaveable { mutableStateOf(state.note.text.isNotEmpty()) }

        // Auto-show fields if they gain content
        LaunchedEffect(state.label.text.isNotEmpty()) {
            if (state.label.text.isNotEmpty()) {
                labelFieldAdded = true
            }
        }

        LaunchedEffect(state.note.text.isNotEmpty()) {
            if (state.note.text.isNotEmpty()) {
                noteFieldAdded = true
            }
        }

        when (state.trackerType) {
            TrackerType.NUMERICAL -> {
                ValueInputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = inputSpacingLarge),
                    textFieldValue = state.value,
                    onValueChange = callbacks::onValueChanged,
                    focusManager = focusManager,
                    focusRequester = if (shouldFocusValue) valueFocusRequester else null,
                    onNextOverride = {
                        if (labelFieldAdded || noteFieldAdded) focusManager.moveFocus(FocusDirection.Down)
                        else callbacks.onAddDataPoint()
                    }
                )
            }

            TrackerType.DURATION -> {
                DurationInputView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = inputSpacingLarge),
                    hours = state.hours,
                    minutes = state.minutes,
                    seconds = state.seconds,
                    onHoursChanged = callbacks::onHoursChanged,
                    onMinutesChanged = callbacks::onMinutesChanged,
                    onSecondsChanged = callbacks::onSecondsChanged,
                    focusManager = focusManager,
                    nextFocusDirection = FocusDirection.Down,
                    focusRequester = if (shouldFocusValue) valueFocusRequester else null
                )
            }
        }

        if (isCurrentPage) {
            LabelAndNoteInputsView(
                label = state.label,
                note = state.note,
                labelAdded = labelFieldAdded,
                noteAdded = noteFieldAdded,
                onLabelAdded = { labelFieldAdded = true },
                onNoteAdded = { noteFieldAdded = true },
                onLabelChanged = callbacks::onLabelChanged,
                onNoteChanged = callbacks::onNoteChanged,
                onAddDataPoint = callbacks::onAddDataPoint
            )
        }
    }
}

private enum class PendingFocus { Label, Note }

@Composable
private fun LabelAndNoteInputsView(
    label: TextFieldValue,
    note: TextFieldValue,
    labelAdded: Boolean,
    noteAdded: Boolean,
    onLabelAdded: () -> Unit,
    onNoteAdded: () -> Unit,
    onLabelChanged: (TextFieldValue) -> Unit,
    onNoteChanged: (TextFieldValue) -> Unit,
    onAddDataPoint: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()
    val noteInputFocusRequester = remember { FocusRequester() }
    val labelInputFocusRequester = remember { FocusRequester() }

    var pendingFocus by remember { mutableStateOf<PendingFocus?>(null) }

    // Consume pending focus once the field is visible
    LaunchedEffect(labelAdded, noteAdded, pendingFocus) {
        when {
            pendingFocus == PendingFocus.Label && labelAdded -> {
                withFrameNanos { }
                labelInputFocusRequester.requestFocus()
                pendingFocus = null
            }

            pendingFocus == PendingFocus.Note && noteAdded -> {
                withFrameNanos { }
                noteInputFocusRequester.requestFocus()
                pendingFocus = null
            }
        }
    }

    DialogInputSpacing()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    if (labelAdded) {
        LabelInputTextField(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(horizontal = inputSpacingLarge)
                .onFocusEvent { state ->
                    if (state.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
            textFieldValue = label,
            onValueChange = onLabelChanged,
            focusManager = focusManager,
            focusRequester = labelInputFocusRequester,
            onNextOverride = {
                if (noteAdded) focusManager.moveFocus(FocusDirection.Down)
                else onAddDataPoint()
            }
        )
    }

    if (noteAdded) {
        if (labelAdded) DialogInputSpacing()
        
        // Track focus state for the note field
        var noteFieldFocused by remember { mutableStateOf(false) }
        
        // Track when note text changes to scroll to cursor (only when focused)
        LaunchedEffect(note.text, noteFieldFocused) {
            if (noteFieldFocused && note.text.isNotEmpty()) {
                // Small delay to allow text field to recompose with new height
                withFrameNanos { }
                bringIntoViewRequester.bringIntoView()
            }
        }
        
        FullWidthTextField(
            modifier = Modifier
                .testTag("notesInput")
                .heightIn(min = inputSpacingLarge, max = 200.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(horizontal = inputSpacingLarge)
                .onFocusEvent { focusState ->
                    noteFieldFocused = focusState.isFocused
                },
            textFieldValue = note,
            onValueChange = onNoteChanged,
            focusRequester = noteInputFocusRequester,
            label = stringResource(id = R.string.note_input_hint),
            singleLine = false
        )
    }

    if (labelAdded xor noteAdded) DialogInputSpacing()

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = cardPadding),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (!labelAdded) {
            AddChipButton(text = stringResource(id = R.string.add_a_label)) {
                onLabelAdded()
                pendingFocus = PendingFocus.Label
            }
        }
        if (!noteAdded) {
            AddChipButton(
                modifier = Modifier.testTag("addNoteChip"),
                text = stringResource(id = R.string.add_a_note)) {
                onNoteAdded()
                pendingFocus = PendingFocus.Note
            }
        }
    }
}

@Composable
private fun SuggestedValues(
    list: List<SuggestedValueViewData>,
    selectedItem: SuggestedValueViewData?,
    onSuggestedValueSelected: (SuggestedValueViewData) -> Unit,
    onSuggestedValueLongPress: (SuggestedValueViewData) -> Unit
) {
    val focusManager = LocalFocusManager.current

    // Pre-calculate display text and create stable items
    val stableItems = remember(list) {
        list.map { suggestedValue ->
            val displayText = when {
                suggestedValue.label.isNullOrBlank() && suggestedValue.valueStr != null ->
                    suggestedValue.valueStr

                suggestedValue.valueStr.isNullOrBlank() && suggestedValue.label != null ->
                    suggestedValue.label

                else ->
                    "${suggestedValue.valueStr} : ${suggestedValue.label}"
            }

            SuggestedValueItem(
                data = suggestedValue,
                displayText = displayText,
                // Create stable key from value and label combination
                key = "${suggestedValue.value}_${suggestedValue.valueStr}_${suggestedValue.label}"
            )
        }
    }

    FadingLazyRow(
        horizontalArrangement = Arrangement.spacedBy(
            dialogInputSpacing,
            Alignment.CenterHorizontally
        )
    ) {
        items(
            items = stableItems,
            key = { it.key }
        ) { item ->
            TextChip(
                text = item.displayText,
                isSelected = item.data == selectedItem,
                onClick = {
                    onSuggestedValueSelected(item.data)
                },
                onLongPress = {
                    onSuggestedValueLongPress(item.data)
                }
            )
        }
    }
}

@Parcelize
private data class SuggestedValueItem(
    val data: SuggestedValueViewData,
    val displayText: String,
    val key: String
) : Parcelable

// Preview composables
@Preview(showBackground = true)
@Composable
fun TrackerPageViewPreview() {
    TnGComposeTheme {
        CompositionLocalProvider(LocalSettings provides mockSettings) {
            val sampleState = TrackerPageState(
                name = "Weight",
                timestamp = OffsetDateTime.parse("2023-06-15T14:30:00+01:00"),
                suggestedValues = listOf(
                    SuggestedValueViewData(70.0, "70.0", "Morning"),
                    SuggestedValueViewData(71.5, "71.5", "Evening")
                ),
                currentValueAsSuggestion = null,
                label = TextFieldValue("Morning weigh-in"),
                note = TextFieldValue("After breakfast"),
                trackerType = TrackerType.NUMERICAL,
                value = TextFieldValue("70.5")
            )

            val sampleCallbacks = object : TrackerPageCallbacks {
                override fun onTimestampChanged(timestamp: OffsetDateTime) {}
                override fun onSuggestedValueSelected(suggestedValue: SuggestedValueViewData) {}
                override fun onSuggestedValueLongPress(suggestedValue: SuggestedValueViewData) {}
                override fun onLabelChanged(label: TextFieldValue) {}
                override fun onNoteChanged(note: TextFieldValue) {}
                override fun onValueChanged(value: TextFieldValue) {}
                override fun onHoursChanged(hours: TextFieldValue) {}
                override fun onMinutesChanged(minutes: TextFieldValue) {}
                override fun onSecondsChanged(seconds: TextFieldValue) {}
                override fun onAddDataPoint() {}
            }

            TrackerPageView(
                state = sampleState,
                isCurrentPage = true,
                callbacks = sampleCallbacks
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackerPageViewPreviewWithChips() {
    TnGComposeTheme {
        CompositionLocalProvider(LocalSettings provides mockSettings) {
            val sampleState = TrackerPageState(
                name = "Weight",
                timestamp = OffsetDateTime.parse("2023-06-15T14:30:00+01:00"),
                suggestedValues = listOf(
                    SuggestedValueViewData(70.0, "70.0", "Morning"),
                    SuggestedValueViewData(71.5, "71.5", "Evening")
                ),
                currentValueAsSuggestion = null,
                label = TextFieldValue(""),
                note = TextFieldValue(""),
                trackerType = TrackerType.NUMERICAL,
                value = TextFieldValue("70.5")
            )

            val sampleCallbacks = object : TrackerPageCallbacks {
                override fun onTimestampChanged(timestamp: OffsetDateTime) {}
                override fun onSuggestedValueSelected(suggestedValue: SuggestedValueViewData) {}
                override fun onSuggestedValueLongPress(suggestedValue: SuggestedValueViewData) {}
                override fun onLabelChanged(label: TextFieldValue) {}
                override fun onNoteChanged(note: TextFieldValue) {}
                override fun onValueChanged(value: TextFieldValue) {}
                override fun onHoursChanged(hours: TextFieldValue) {}
                override fun onMinutesChanged(minutes: TextFieldValue) {}
                override fun onSecondsChanged(seconds: TextFieldValue) {}
                override fun onAddDataPoint() {}
            }

            TrackerPageView(
                state = sampleState,
                isCurrentPage = true,
                callbacks = sampleCallbacks
            )
        }
    }
}
