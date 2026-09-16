/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.reminders.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.ui.RowCheckbox
import com.samco.trackandgraph.ui.ui.RowRadioButton
import com.samco.trackandgraph.ui.ui.buttonSize
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@Composable
fun OneTimeReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.OneTimeParams? = null,
    onUpsertReminder: (ReminderInput) -> Unit,
    onDismiss: () -> Unit,
    viewModel: OneTimeReminderConfigurationViewModel =
        hiltViewModel<OneTimeReminderConfigurationViewModelImpl>(),
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val enabled by viewModel.enabled.collectAsState()
    val startType by viewModel.startType.collectAsState()
    val starts by viewModel.starts.collectAsState()
    val delayInterval by viewModel.delayInterval.collectAsState()
    val delayPeriod by viewModel.delayPeriod.collectAsState()
    val hasRepeatInterval by viewModel.hasRepeatInterval.collectAsState()
    val repeatInterval by viewModel.repeatInterval.collectAsState()
    val repeatPeriod by viewModel.repeatPeriod.collectAsState()

    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }

    OneTimeReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        enabled = enabled,
        onEnabledChanged = viewModel::updateEnabled,
        startType = startType,
        onStartTypeChanged = viewModel::updateStartType,
        starts = starts,
        onStartsChanged = viewModel::updateStarts,
        delayInterval = delayInterval,
        onDelayIntervalChanged = viewModel::updateDelayInterval,
        delayPeriod = delayPeriod,
        onDelayPeriodChanged = viewModel::updateDelayPeriod,
        hasRepeatInterval = hasRepeatInterval,
        onHasRepeatIntervalChanged = viewModel::updateHasRepeatInterval,
        repeatInterval = repeatInterval,
        onRepeatIntervalChanged = viewModel::updateRepeatInterval,
        repeatPeriod = repeatPeriod,
        onRepeatPeriodChanged = viewModel::updateRepeatPeriod,
        isEditMode = editReminder != null,
        onConfirm = { onUpsertReminder(viewModel.getReminderInput()) },
        onDismiss = onDismiss,
    )
}

@Composable
private fun OneTimeReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    startType: OneTimeStartType,
    onStartTypeChanged: (OneTimeStartType) -> Unit,
    starts: OffsetDateTime,
    onStartsChanged: (OffsetDateTime) -> Unit,
    delayInterval: String,
    onDelayIntervalChanged: (String) -> Unit,
    delayPeriod: Period,
    onDelayPeriodChanged: (Period) -> Unit,
    hasRepeatInterval: Boolean,
    onHasRepeatIntervalChanged: (Boolean) -> Unit,
    repeatInterval: String,
    onRepeatIntervalChanged: (String) -> Unit,
    repeatPeriod: Period,
    onRepeatPeriodChanged: (Period) -> Unit,
    isEditMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditMode) {
        if (!isEditMode) focusRequester.requestFocus()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DialogInputSpacing()
        ReminderNameTextField(
            reminderName = reminderName,
            onReminderNameChanged = onReminderNameChanged,
            enabled = enabled,
            onEnabledChanged = onEnabledChanged,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        InputSpacingLarge()

        Text(
            text = stringResource(R.string.first_reminder),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onSurface,
        )

        InputSpacingLarge()

        RowRadioButton(
            modifier = Modifier.fillMaxWidth(),
            selected = startType == OneTimeStartType.DATE_TIME,
            text = stringResource(R.string.remind_at_date_and_time),
            onClick = { onStartTypeChanged(OneTimeStartType.DATE_TIME) },
            textStyle = MaterialTheme.typography.titleSmall,
        )
        AnimatedVisibility(startType == OneTimeStartType.DATE_TIME) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = buttonSize),
            ) {
                DialogInputSpacing()
                DateTimeButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedDateTime = starts,
                    onDateTimeSelected = onStartsChanged,
                )
            }
        }

        DialogInputSpacing()

        RowRadioButton(
            modifier = Modifier.fillMaxWidth(),
            selected = startType == OneTimeStartType.AFTER_DELAY,
            text = stringResource(R.string.remind_after_a_delay),
            onClick = { onStartTypeChanged(OneTimeStartType.AFTER_DELAY) },
            textStyle = MaterialTheme.typography.titleSmall,
        )
        AnimatedVisibility(startType == OneTimeStartType.AFTER_DELAY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = buttonSize),
            ) {
                DialogInputSpacing()
                IntervalPeriodRow(
                    interval = delayInterval,
                    onIntervalChanged = onDelayIntervalChanged,
                    period = delayPeriod,
                    onPeriodChanged = onDelayPeriodChanged,
                )
            }
        }

        InputSpacingLarge()
        HorizontalDivider()
        DialogInputSpacing()

        RowCheckbox(
            modifier = Modifier.fillMaxWidth(),
            checked = hasRepeatInterval,
            onCheckedChange = onHasRepeatIntervalChanged,
            text = stringResource(R.string.then_remind_every),
            textStyle = MaterialTheme.typography.titleSmall,
        )
        AnimatedVisibility(hasRepeatInterval) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = buttonSize),
            ) {
                DialogInputSpacing()
                IntervalPeriodRow(
                    interval = repeatInterval,
                    onIntervalChanged = onRepeatIntervalChanged,
                    period = repeatPeriod,
                    onPeriodChanged = onRepeatPeriodChanged,
                )
            }
        }

        DialogInputSpacing()
        ContinueCancelButtons(
            cancelVisible = true,
            cancelText = R.string.cancel,
            continueText = if (isEditMode) R.string.update else R.string.add,
            onContinue = onConfirm,
            onCancel = onDismiss,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneTimeReminderConfigurationContentPreview() {
    TnGComposeTheme {
        OneTimeReminderConfigurationContent(
            reminderName = "Call the dentist",
            onReminderNameChanged = {},
            enabled = true,
            onEnabledChanged = {},
            startType = OneTimeStartType.AFTER_DELAY,
            onStartTypeChanged = {},
            starts = OffsetDateTime.of(2026, 9, 16, 14, 30, 0, 0, ZoneOffset.UTC),
            onStartsChanged = {},
            delayInterval = "2",
            onDelayIntervalChanged = {},
            delayPeriod = Period.HOURS,
            onDelayPeriodChanged = {},
            hasRepeatInterval = true,
            onHasRepeatIntervalChanged = {},
            repeatInterval = "1",
            onRepeatIntervalChanged = {},
            repeatPeriod = Period.DAYS,
            onRepeatPeriodChanged = {},
            isEditMode = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
