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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.CheckedDays.Companion.withSet
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.TextChip
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.dateTimeButtonWidth
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import org.threeten.bp.LocalTime

@Composable
fun WeekDayReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.WeekDayParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    onSetCleanup: (() -> Unit) -> Unit = {},
    viewModel: WeekDayReminderConfigurationViewModel = hiltViewModel<WeekDayReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val checkedDays by viewModel.checkedDays.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    LaunchedEffect(viewModel) {
        onSetCleanup { viewModel.reset() }
    }

    WeekDayReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        selectedTime = selectedTime,
        onTimeSelected = viewModel::updateSelectedTime,
        checkedDays = checkedDays,
        onCheckedDaysChanged = viewModel::updateCheckedDays,
        isEditMode = editReminder != null,
        onConfirm = {
            onUpsertReminder(viewModel.getReminder())
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun WeekDayReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    checkedDays: CheckedDays,
    onCheckedDaysChanged: (CheckedDays) -> Unit,
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
            label = { Text(stringResource(R.string.reminder_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // Repeat on week days section
        Text(
            text = stringResource(R.string.repeat_on_week_days),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        DayCheckboxes(
            checkedDays = checkedDays,
            onCheckedDaysChanged = onCheckedDaysChanged
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // At time section
        Text(
            text = stringResource(R.string.at_time),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        DialogInputSpacing()
        TimeButton(
            modifier = Modifier.widthIn(min = dateTimeButtonWidth),
            time = selectedTime,
            onTimeSelected = onTimeSelected
        )

        DialogInputSpacing()

        // Action buttons
        ContinueCancelButtons(
            cancelVisible = true,
            cancelText = R.string.cancel,
            continueText = if (isEditMode) R.string.update else R.string.add,
            onContinue = onConfirm,
            onCancel = onDismiss
        )
    }
}

@Composable
fun DayCheckboxes(
    checkedDays: CheckedDays,
    onCheckedDaysChanged: (CheckedDays) -> Unit
) {
    val dayNames = listOf(
        stringResource(id = R.string.mon),
        stringResource(id = R.string.tue),
        stringResource(id = R.string.wed),
        stringResource(id = R.string.thu),
        stringResource(id = R.string.fri),
        stringResource(id = R.string.sat),
        stringResource(id = R.string.sun)
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            dialogInputSpacing,
            Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        dayNames.forEachIndexed { index, dayName ->
            TextChip(
                modifier = Modifier.widthIn(min = buttonSize),
                text = dayName,
                isSelected = checkedDays[index],
                onClick = {
                    onCheckedDaysChanged(checkedDays.withSet(index, !checkedDays[index]))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeekDayReminderConfigurationContentPreview() {
    TnGComposeTheme {
        WeekDayReminderConfigurationContent(
            reminderName = "Morning Exercise",
            onReminderNameChanged = {},
            selectedTime = LocalTime.of(9, 0),
            onTimeSelected = {},
            checkedDays = CheckedDays.all().copy(
                wednesday = false,
                sunday = false
            ),
            onCheckedDaysChanged = {},
            isEditMode = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
