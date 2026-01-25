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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import com.samco.trackandgraph.ui.compose.ui.dateTimeButtonWidth
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime

@Composable
fun MonthDayReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.MonthDayParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    onSetCleanup: (() -> Unit) -> Unit = {},
    viewModel: MonthDayReminderConfigurationViewModel = hiltViewModel<MonthDayReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val occurrence by viewModel.occurrence.collectAsState()
    val dayType by viewModel.dayType.collectAsState()
    val endsEnabled by viewModel.endsEnabled.collectAsState()
    val ends by viewModel.ends.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    LaunchedEffect(viewModel) {
        onSetCleanup { viewModel.reset() }
    }

    MonthDayReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        selectedTime = selectedTime,
        onTimeSelected = viewModel::updateSelectedTime,
        occurrence = occurrence,
        onOccurrenceChanged = viewModel::updateOccurrence,
        dayType = dayType,
        onDayTypeChanged = viewModel::updateDayType,
        endsEnabled = endsEnabled,
        onEndsEnabledChanged = viewModel::updateEndsEnabled,
        ends = ends,
        onEndsChanged = viewModel::updateEnds,
        isEditMode = editReminder != null,
        onConfirm = {
            onUpsertReminder(viewModel.getReminder())
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun MonthDayReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    occurrence: MonthDayOccurrence,
    onOccurrenceChanged: (MonthDayOccurrence) -> Unit,
    dayType: MonthDayType,
    onDayTypeChanged: (MonthDayType) -> Unit,
    endsEnabled: Boolean,
    onEndsEnabledChanged: (Boolean) -> Unit,
    ends: LocalDateTime,
    onEndsChanged: (LocalDateTime) -> Unit,
    isEditMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            focusRequester.requestFocus()
        }
    }

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

    // Repeat every section
    Text(
        text = stringResource(R.string.repeat_every),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.tngColors.onSurface
    )
    DialogInputSpacing()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Occurrence selector
        TextMapSpinner(
            strings = mapOf(
                MonthDayOccurrence.FIRST to stringResource(R.string.occurrence_first),
                MonthDayOccurrence.SECOND to stringResource(R.string.occurrence_second),
                MonthDayOccurrence.THIRD to stringResource(R.string.occurrence_third),
                MonthDayOccurrence.FOURTH to stringResource(R.string.occurrence_fourth),
                MonthDayOccurrence.LAST to stringResource(R.string.occurrence_last)
            ),
            selectedItem = occurrence,
            onItemSelected = onOccurrenceChanged,
            modifier = Modifier.weight(1f)
        )

        // Day type selector
        TextMapSpinner(
            strings = mapOf(
                MonthDayType.DAY to stringResource(R.string.day),
                MonthDayType.MONDAY to stringResource(R.string.monday),
                MonthDayType.TUESDAY to stringResource(R.string.tuesday),
                MonthDayType.WEDNESDAY to stringResource(R.string.wednesday),
                MonthDayType.THURSDAY to stringResource(R.string.thursday),
                MonthDayType.FRIDAY to stringResource(R.string.friday),
                MonthDayType.SATURDAY to stringResource(R.string.saturday),
                MonthDayType.SUNDAY to stringResource(R.string.sunday)
            ),
            selectedItem = dayType,
            onItemSelected = onDayTypeChanged,
            modifier = Modifier.weight(1f)
        )
    }

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

    InputSpacingLarge()
    HorizontalDivider()
    InputSpacingLarge()

    // End date/time (optional)
    RowCheckbox(
        checked = endsEnabled,
        onCheckedChange = onEndsEnabledChanged,
        text = stringResource(R.string.ending_at),
        textStyle = MaterialTheme.typography.titleSmall,
    )

    AnimatedVisibility(endsEnabled) {
        DialogInputSpacing()
        DateTimeButtonRow(
            modifier = Modifier.fillMaxWidth(),
            selectedDateTime = ends.atOffset(OffsetDateTime.now().offset),
            onDateTimeSelected = { newEndDate ->
                onEndsChanged(newEndDate.toLocalDateTime())
            }
        )
    }
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


@Preview(showBackground = true)
@Composable
fun MonthDayReminderConfigurationContentPreview() {
    TnGComposeTheme {
        MonthDayReminderConfigurationContent(
            reminderName = "Monthly Report",
            onReminderNameChanged = {},
            selectedTime = LocalTime.of(9, 0),
            onTimeSelected = {},
            occurrence = MonthDayOccurrence.FIRST,
            onOccurrenceChanged = {},
            dayType = MonthDayType.MONDAY,
            onDayTypeChanged = {},
            endsEnabled = false,
            onEndsEnabledChanged = {},
            ends = LocalDateTime.of(2025, 12, 31, 23, 59),
            onEndsChanged = {},
            isEditMode = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
