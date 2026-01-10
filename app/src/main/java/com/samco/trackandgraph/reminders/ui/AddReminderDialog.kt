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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
fun AddReminderDialog(
    editReminderId: Long?,
    onDismiss: () -> Unit
) {
    val viewModel: AddReminderViewModel = hiltViewModel<AddReminderViewModelImpl>()

    LaunchedEffect(editReminderId) {
        viewModel.loadStateForReminder(editReminderId)
    }

    LaunchedEffect(viewModel.onComplete) {
        for (event in viewModel.onComplete) onDismiss()
    }

    val editingReminder = viewModel.editingReminder.collectAsStateWithLifecycle().value
    val editMode = viewModel.editMode.collectAsStateWithLifecycle().value

    AddReminderDialog(
        onConfirm = viewModel::upsertReminder,
        onDismiss = onDismiss,
        editMode = editMode,
        editingReminder = editingReminder
    )
}

@Composable
private fun AddReminderDialog(
    onConfirm: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    editMode: Boolean,
    editingReminder: Reminder? = null,
) {
    var onCleanup by remember { mutableStateOf<() -> Unit>({}) }

    val wrappedDismiss = {
        onCleanup()
        onDismiss()
    }

    CustomDialog(
        onDismissRequest = wrappedDismiss,
        supportSmoothHeightAnimation = true,
        paddingValues = PaddingValues(
            start = inputSpacingLarge,
            end = inputSpacingLarge,
            bottom = halfDialogInputSpacing,
            top = inputSpacingLarge,
        )
    ) {
        if (editMode && editingReminder != null) {
            when (val params = editingReminder.params) {
                is ReminderParams.WeekDayParams -> WeekDayReminderConfigurationScreen(
                    editReminder = editingReminder,
                    editParams = params,
                    onUpsertReminder = onConfirm,
                    onDismiss = wrappedDismiss,
                    onSetCleanup = { onCleanup = it }
                )
                is ReminderParams.PeriodicParams -> PeriodicReminderConfigurationScreen(
                    editReminder = editingReminder,
                    editParams = params,
                    onUpsertReminder = onConfirm,
                    onDismiss = wrappedDismiss,
                    onSetCleanup = { onCleanup = it }
                )
                is ReminderParams.MonthDayParams -> MonthDayReminderConfigurationScreen(
                    editReminder = editingReminder,
                    editParams = params,
                    onUpsertReminder = onConfirm,
                    onDismiss = wrappedDismiss,
                    onSetCleanup = { onCleanup = it }
                )
                is ReminderParams.TimeSinceLastParams -> TimeSinceLastReminderConfigurationScreen(
                    editReminder = editingReminder,
                    editParams = params,
                    onUpsertReminder = onConfirm,
                    onDismiss = wrappedDismiss,
                    onSetCleanup = { onCleanup = it }
                )
            }
        } else {
            // Add mode: show navigation flow
            AddReminderDialogContent(
                onConfirm = onConfirm,
                onDismiss = wrappedDismiss,
                onSetCleanup = { onCleanup = it }
            )
        }
    }
}


@Preview
@Composable
private fun AddReminderDialogPreview() {
    AddReminderDialog(
        onConfirm = {},
        onDismiss = {},
        editMode = false,
    )
}