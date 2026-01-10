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

package com.samco.trackandgraph.reminders.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.SelectorButton

@Composable
fun TimeSinceLastReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.TimeSinceLastParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    onSetCleanup: (() -> Unit) -> Unit = {},
    viewModel: TimeSinceLastReminderConfigurationViewModel = hiltViewModel<TimeSinceLastReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val firstInterval by viewModel.firstInterval.collectAsState()
    val firstPeriod by viewModel.firstPeriod.collectAsState()
    val secondInterval by viewModel.secondInterval.collectAsState()
    val secondPeriod by viewModel.secondPeriod.collectAsState()
    val hasSecondInterval by viewModel.hasSecondInterval.collectAsState()
    val featureName by viewModel.featureName.collectAsState()
    val continueEnabled by viewModel.continueEnabled.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    LaunchedEffect(viewModel) {
        onSetCleanup { viewModel.reset() }
    }

    TimeSinceLastReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        firstInterval = firstInterval,
        onFirstIntervalChanged = viewModel::updateFirstInterval,
        firstPeriod = firstPeriod,
        onFirstPeriodChanged = viewModel::updateFirstPeriod,
        secondInterval = secondInterval,
        onSecondIntervalChanged = viewModel::updateSecondInterval,
        secondPeriod = secondPeriod,
        onSecondPeriodChanged = viewModel::updateSecondPeriod,
        hasSecondInterval = hasSecondInterval,
        onHasSecondIntervalChanged = viewModel::updateHasSecondInterval,
        featureName = featureName,
        onFeatureIdChanged = viewModel::updateFeatureId,
        continueEnabled = continueEnabled,
        isEditMode = editReminder != null,
        onConfirm = {
            onUpsertReminder(viewModel.getReminder())
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun TimeSinceLastReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    firstInterval: String,
    onFirstIntervalChanged: (String) -> Unit,
    firstPeriod: Period,
    onFirstPeriodChanged: (Period) -> Unit,
    secondInterval: String,
    onSecondIntervalChanged: (String) -> Unit,
    secondPeriod: Period,
    onSecondPeriodChanged: (Period) -> Unit,
    hasSecondInterval: Boolean,
    onHasSecondIntervalChanged: (Boolean) -> Unit,
    featureName: String,
    onFeatureIdChanged: (Long?) -> Unit,
    continueEnabled: Boolean,
    isEditMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            focusRequester.requestFocus()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DialogInputSpacing()

        // Name field
        OutlinedTextField(
            value = reminderName,
            onValueChange = onReminderNameChanged,
            label = { Text("Reminder Name") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // Feature selection section
        Text(
            text = "Track Feature",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        var showFeatureSelectDialog by rememberSaveable { mutableStateOf(false) }

        SelectorButton(
            modifier = Modifier.fillMaxWidth(),
            text = featureName.ifEmpty { "Select a feature" },
            onClick = { showFeatureSelectDialog = true }
        )

        if (showFeatureSelectDialog) {
            SelectItemDialog(
                title = stringResource(R.string.select_a_feature),
                selectableTypes = setOf(SelectableItemType.FEATURE),
                onFeatureSelected = { selectedFeatureId ->
                    onFeatureIdChanged(selectedFeatureId)
                    showFeatureSelectDialog = false
                },
                onDismissRequest = {
                    showFeatureSelectDialog = false
                }
            )
        }

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // First interval section
        Text(
            text = "Remind After",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        IntervalPeriodRow(
            interval = firstInterval,
            onIntervalChanged = onFirstIntervalChanged,
            period = firstPeriod,
            onPeriodChanged = onFirstPeriodChanged
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // Second interval section (optional)
        RowCheckbox(
            checked = hasSecondInterval,
            onCheckedChange = onHasSecondIntervalChanged,
            text = "Then Remind Every",
            textStyle = MaterialTheme.typography.titleSmall,
        )

        AnimatedVisibility(hasSecondInterval) {
            Column {
                DialogInputSpacing()
                IntervalPeriodRow(
                    interval = secondInterval,
                    onIntervalChanged = onSecondIntervalChanged,
                    period = secondPeriod,
                    onPeriodChanged = onSecondPeriodChanged
                )
            }
        }

        DialogInputSpacing()

        // Action buttons
        ContinueCancelButtons(
            cancelVisible = true,
            cancelText = R.string.cancel,
            continueText = if (isEditMode) R.string.update else R.string.add,
            onContinue = onConfirm,
            continueEnabled = continueEnabled,
            onCancel = onDismiss
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimeSinceLastReminderConfigurationContentPreview() {
    TnGComposeTheme {
        TimeSinceLastReminderConfigurationContent(
            reminderName = "Exercise Reminder",
            onReminderNameChanged = {},
            firstInterval = "3",
            onFirstIntervalChanged = {},
            firstPeriod = Period.DAYS,
            onFirstPeriodChanged = {},
            secondInterval = "1",
            onSecondIntervalChanged = {},
            secondPeriod = Period.DAYS,
            onSecondPeriodChanged = {},
            hasSecondInterval = true,
            onHasSecondIntervalChanged = {},
            featureName = "Exercise Sessions",
            onFeatureIdChanged = {},
            continueEnabled = true,
            isEditMode = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
