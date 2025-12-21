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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.samco.trackandgraph.ui.compose.ui.TextChip
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.dateTimeButtonWidth
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import kotlinx.coroutines.selects.select
import org.threeten.bp.LocalDateTime

@Composable
fun PeriodicReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.PeriodicParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PeriodicReminderConfigurationViewModel = hiltViewModel<PeriodicReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val starts by viewModel.starts.collectAsState()
    val ends by viewModel.ends.collectAsState()
    val interval by viewModel.interval.collectAsState()
    val period by viewModel.period.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    PeriodicReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        starts = starts,
        onStartsChanged = viewModel::updateStarts,
        ends = ends,
        onEndsChanged = viewModel::updateEnds,
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
fun PeriodicReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    starts: LocalDateTime,
    onStartsChanged: (LocalDateTime) -> Unit,
    ends: LocalDateTime?,
    onEndsChanged: (LocalDateTime?) -> Unit,
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

        // Start date/time
        Text(
            text = "Start Date & Time",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        DialogInputSpacing()
        DateTimeButtonRow(
            modifier = Modifier.widthIn(min = dateTimeButtonWidth),
            selectedDateTime = starts,
            onDateTimeSelected = onStartsChanged
        )

        DialogInputSpacing()

        // End date/time (optional)
        Text(
            text = "End Date & Time (Optional)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        DialogInputSpacing()
        DateTimeButtonRow(
            modifier = Modifier.widthIn(min = dateTimeButtonWidth),
            dateTime = ends,
            onDateTimeSelected = onEndsChanged,
            allowNull = true,
            nullText = "No End Date"
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
                label = { Text("Number") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Period selector
            PeriodSelector(
                modifier = Modifier.weight(2f),
                selectedPeriod = period,
                onPeriodSelected = onPeriodChanged
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

@Composable
fun PeriodSelector(
    modifier: Modifier = Modifier,
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit
) {
    val periods = listOf(
        Period.DAYS to "Days",
        Period.WEEKS to "Weeks", 
        Period.MONTHS to "Months",
        Period.YEARS to "Years"
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            dialogInputSpacing,
            Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        periods.forEach { (period, label) ->
            TextChip(
                modifier = Modifier.widthIn(min = buttonSize),
                text = label,
                isSelected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PeriodicReminderConfigurationContentPreview() {
    TnGComposeTheme {
        PeriodicReminderConfigurationContent(
            reminderName = "Daily Exercise",
            onReminderNameChanged = {},
            starts = LocalDateTime.of(2025, 1, 1, 9, 0),
            onStartsChanged = {},
            ends = null,
            onEndsChanged = {},
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
