/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.addtracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.TrackerHelper
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.*

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_3)
fun AddTrackerView() {
    TnGComposeTheme {
        AddTrackerView(viewModel = object : AddTrackerViewModel {
            override val trackerName = MutableLiveData("Tracker name")
            override val trackerDescription = MutableLiveData("Tracker description")
            override val isDuration = MutableLiveData(false)
            override val isLoading = MutableLiveData(false)
            override val hasDefaultValue = MutableLiveData(true)
            override val defaultValue = MutableLiveData("1.0")
            override val defaultLabel = MutableLiveData("")
            override val createButtonEnabled = MutableLiveData(false)
            override val errorText: LiveData<Int?> = MutableLiveData(null)
            override val durationNumericConversionMode =
                MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
            override val shouldShowDurationConversionModeSpinner = MutableLiveData(true)
            override val isUpdateMode = MutableLiveData(true)
            override val showUpdateWarningAlertDialog = MutableLiveData(false)
            override val hours = MutableLiveData("")
            override val minutes = MutableLiveData("")
            override val seconds = MutableLiveData("")

            override fun onTrackerNameChanged(name: String) {}

            override fun onTrackerDescriptionChanged(description: String) {}

            override fun onIsDurationCheckChanged(isDuration: Boolean) {}

            override fun onHasDefaultValueChanged(hasDefaultValue: Boolean) {}

            override fun onDefaultValueChanged(defaultValue: String) {}

            override fun onDefaultLabelChanged(defaultLabel: String) {}

            override fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode) {}

            override fun onConfirmUpdate() {}

            override fun onDismissUpdateWarningCancel() {}

            override fun onCreateUpdateClicked() {}

            override fun setHours(value: String) {}

            override fun setMinutes(value: String) {}

            override fun setSeconds(value: String) {}

            override fun setDurationFromDouble(value: Double) { }

            override fun getDurationAsDouble() = 0.0
        })
    }
}

@Composable
fun AddTrackerView(viewModel: AddTrackerViewModel) {
    val focusRequester = FocusRequester()
    val isUpdateMode by viewModel.isUpdateMode.observeAsState(false)
    val errorText by viewModel.errorText.observeAsState()
    val openDialog by viewModel.showUpdateWarningAlertDialog.observeAsState(false)

    Column(Modifier.fillMaxSize()) {

        AddTrackerInputForm(
            modifier = Modifier.weight(1f),
            viewModel = viewModel,
            focusRequester = focusRequester
        )

        AddCreateBar(
            errorText = errorText,
            onCreateUpdateClicked = viewModel::onCreateUpdateClicked,
            isUpdateMode = isUpdateMode
        )

        if (openDialog) UpdateWarningDialog(
            onDismissRequest = viewModel::onDismissUpdateWarningCancel,
            onConfirm = viewModel::onConfirmUpdate
        )

        LaunchedEffect(true) { focusRequester.requestFocus() }
    }
}

@Composable
private fun UpdateWarningDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) = ConfirmCancelDialog(
    body = R.string.ru_sure_update_data,
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AddTrackerInputForm(
    modifier: Modifier,
    viewModel: AddTrackerViewModel,
    focusRequester: FocusRequester
) = Column(
    modifier = modifier
        .padding(dimensionResource(id = R.dimen.card_padding))
        .fillMaxWidth()
        .fillMaxHeight()
        .verticalScroll(state = rememberScrollState())
) {
    val trackerName = viewModel.trackerName.observeAsState("")
    val trackerDescription = viewModel.trackerDescription.observeAsState("")
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isDuration = viewModel.isDuration.observeAsState(false)
    val hasDefaultValue = viewModel.hasDefaultValue.observeAsState(true)
    val defaultValue = viewModel.defaultValue.observeAsState("")
    val defaultLabel = viewModel.defaultLabel.observeAsState("")

    SpacingSmall()

    NameInput(
        trackerName.value,
        viewModel,
        focusManager,
        focusRequester,
        keyboardController
    )

    SpacingLarge()

    DescriptionInput(trackerDescription.value, viewModel)

    SpacingLarge()

    DurationCheckbox(isDuration.value, viewModel)

    val shouldShowConversionSpinner =
        viewModel.shouldShowDurationConversionModeSpinner.observeAsState(true)
    val durationConversionMode = viewModel.durationNumericConversionMode.observeAsState()

    if (shouldShowConversionSpinner.value) {
        SpacingSmall()
        DurationConversionModeInput(
            isDuration.value,
            durationConversionMode.value,
            viewModel
        )
    }

    SpacingLarge()

    DefaultValueCheckbox(hasDefaultValue.value, viewModel)

    if (hasDefaultValue.value) {

        if (isDuration.value) DurationInputRow(viewModel)
        else ValueInputRow(defaultValue.value, viewModel, focusManager)

        SpacingSmall()

        LabelInputRow(defaultLabel.value, viewModel)
    }
}

@Composable
private fun DurationConversionModeInput(
    isDuration: Boolean,
    durationConversionMode: TrackerHelper.DurationNumericConversionMode?,
    viewModel: AddTrackerViewModel
) {
    val strings = mapOf(
        TrackerHelper.DurationNumericConversionMode.HOURS to stringResource(id = R.string.hours),
        TrackerHelper.DurationNumericConversionMode.MINUTES to stringResource(id = R.string.minutes),
        TrackerHelper.DurationNumericConversionMode.SECONDS to stringResource(id = R.string.seconds)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        val name =
            if (isDuration) stringResource(id = R.string.numeric_to_duration_mode_header)
            else stringResource(id = R.string.duration_to_numeric_mode_header)
        Text(
            text = name,
            modifier = Modifier
                .weight(1f)
                .padding(end = dimensionResource(id = R.dimen.card_padding))
        )
        TextMapSpinner(
            strings = strings,
            selectedItem = durationConversionMode
                ?: TrackerHelper.DurationNumericConversionMode.HOURS,
            onItemSelected = viewModel::onDurationNumericConversionModeChanged
        )
    }
}

@Composable
private fun LabelInputRow(
    defaultLabel: String,
    viewModel: AddTrackerViewModel
) {
    LabeledRow(label = stringResource(id = R.string.label_colon)) {
        LabelInputTextField(
            value = defaultLabel,
            onValueChanged = viewModel::onDefaultLabelChanged,
        )
    }
}

@Composable
private fun ValueInputRow(
    defaultValue: String,
    viewModel: AddTrackerViewModel,
    focusManager: FocusManager
) {

    LabeledRow(label = stringResource(id = R.string.value_colon)) {
        ValueInputTextField(
            value = defaultValue,
            onValueChanged = viewModel::onDefaultValueChanged,
            focusManager = focusManager
        )
    }
}

@Composable
private fun DurationInputRow(viewModel: AddTrackerViewModel) {
    LabeledRow(label = stringResource(id = R.string.value_colon)) {
        DurationInput(viewModel = viewModel)
    }
}

@Composable
private fun DefaultValueCheckbox(
    hasDefaultValue: Boolean,
    viewModel: AddTrackerViewModel
) {
    RowCheckbox(
        checked = hasDefaultValue,
        onCheckedChange = { viewModel.onHasDefaultValueChanged(it) },
        text = stringResource(id = R.string.use_default_value)
    )
}

@Composable
private fun DurationCheckbox(
    isDuration: Boolean,
    viewModel: AddTrackerViewModel
) {
    RowCheckbox(
        checked = isDuration,
        onCheckedChange = { viewModel.onIsDurationCheckChanged(it) },
        text = stringResource(id = R.string.tracker_type)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DescriptionInput(
    trackerDescription: String,
    viewModel: AddTrackerViewModel
) = FullWidthTextField(
    value = trackerDescription,
    onValueChange = viewModel::onTrackerDescriptionChanged,
    label = stringResource(id = R.string.add_a_longer_description_optional),
    singleLine = false
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NameInput(
    trackerName: String,
    viewModel: AddTrackerViewModel,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?
) = FullWidthTextField(
    value = trackerName,
    onValueChange = viewModel::onTrackerNameChanged,
    label = stringResource(id = R.string.tracker_name),
    focusManager = focusManager,
    focusRequester = focusRequester,
    keyboardController = keyboardController
)
