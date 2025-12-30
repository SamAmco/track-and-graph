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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import org.threeten.bp.OffsetDateTime

@Composable
fun PeriodicReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.PeriodicParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PeriodicReminderConfigurationViewModel = hiltViewModel<PeriodicReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val startsOffset by viewModel.starts.collectAsState()
    val endsOffset by viewModel.ends.collectAsState()
    val hasEndDate by viewModel.hasEndDate.collectAsState()
    val interval by viewModel.interval.collectAsState()
    val period by viewModel.period.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    PeriodicReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        startsOffset = startsOffset,
        onStartsOffsetChanged = viewModel::updateStarts,
        endsOffset = endsOffset,
        onEndsOffsetChanged = viewModel::updateEnds,
        hasEndDate = hasEndDate,
        onHasEndDateChanged = viewModel::updateHasEndDate,
        interval = interval,
        onIntervalChanged = viewModel::updateInterval,
        period = period,
        onPeriodChanged = viewModel::updatePeriod,
        isEditMode = editReminder != null,
        onConfirm = {
            onUpsertReminder(viewModel.getReminder())
            viewModel.reset()
        },
        onDismiss = {
            viewModel.reset()
            onDismiss()
        }
    )
}

@Composable
private fun PeriodicReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    startsOffset: OffsetDateTime,
    onStartsOffsetChanged: (OffsetDateTime) -> Unit,
    endsOffset: OffsetDateTime,
    onEndsOffsetChanged: (OffsetDateTime) -> Unit,
    hasEndDate: Boolean,
    onHasEndDateChanged: (Boolean) -> Unit,
    interval: Int,
    onIntervalChanged: (Int) -> Unit,
    period: Period,
    onPeriodChanged: (Period) -> Unit,
    isEditMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DialogInputSpacing()

        // Name field
        OutlinedTextField(
            value = reminderName,
            onValueChange = onReminderNameChanged,
            label = { Text("Reminder Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // Interval and Period
        Text(
            text = "Repeat Every",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        DialogInputSpacing()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dialogInputSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interval number input
            OutlinedTextField(
                value = interval.toString(),
                onValueChange = { newValue ->
                    newValue.toIntOrNull()?.let { onIntervalChanged(it) }
                },
                modifier = Modifier.weight(0.4f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Period selector
            TextMapSpinner(
                strings = mapOf(
                    Period.MINUTES to stringResource(id = R.string.minutes_generic),
                    Period.HOURS to stringResource(id = R.string.hours_generic),
                    Period.DAYS to stringResource(id = R.string.days_generic),
                    Period.WEEKS to stringResource(id = R.string.weeks_generic),
                    Period.MONTHS to stringResource(id = R.string.months_generic),
                    Period.YEARS to stringResource(id = R.string.years_generic)
                ),
                selectedItem = period,
                onItemSelected = onPeriodChanged,
                modifier = Modifier.weight(1f)
            )
        }


        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // Start date/time
        Text(
            text = "Starting From",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        DialogInputSpacing()
        DateTimeButtonRow(
            modifier = Modifier.fillMaxWidth(),
            selectedDateTime = startsOffset,
            onDateTimeSelected = onStartsOffsetChanged
        )

        InputSpacingLarge()
        HorizontalDivider()
        InputSpacingLarge()

        // End date/time (optional)
        RowCheckbox(
            checked = hasEndDate,
            onCheckedChange = onHasEndDateChanged,
            text = "Ending At",
            textStyle = MaterialTheme.typography.titleSmall,
        )

        AnimatedVisibility(hasEndDate) {
            DialogInputSpacing()
            DateTimeButtonRow(
                modifier = Modifier.fillMaxWidth(),
                selectedDateTime = endsOffset,
                onDateTimeSelected = { newEndDate ->
                    onEndsOffsetChanged(newEndDate)
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
}

@Preview(showBackground = true)
@Composable
fun PeriodicReminderConfigurationContentPreview() {
    TnGComposeTheme {
        PeriodicReminderConfigurationContent(
            reminderName = "Daily Exercise",
            onReminderNameChanged = {},
            startsOffset = OffsetDateTime.parse("2025-12-23T14:30:00+00:00"),
            onStartsOffsetChanged = {},
            endsOffset = OffsetDateTime.parse("2026-12-23T14:30:00+00:00"),
            onEndsOffsetChanged = {},
            hasEndDate = true,
            onHasEndDateChanged = {},
            interval = 1,
            onIntervalChanged = {},
            period = Period.DAYS,
            onPeriodChanged = {},
            isEditMode = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
