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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge

@Composable
fun AddReminderDialog(
    visible: Boolean,
    editReminderId: Long?,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val dialogOwner = rememberViewModelStoreOwner()

    CompositionLocalProvider(LocalViewModelStoreOwner provides dialogOwner) {
        CustomDialog(
            onDismissRequest = onDismiss,
            supportSmoothHeightAnimation = true,
            paddingValues = PaddingValues(
                start = inputSpacingLarge,
                end = inputSpacingLarge,
                bottom = halfDialogInputSpacing,
                top = inputSpacingLarge,
            )
        ) {
            AddReminderSessionDestination(
                editReminderId = editReminderId,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun AddReminderSessionDestination(
    editReminderId: Long?,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<AddReminderViewModelImpl>()

    LaunchedEffect(editReminderId) {
        viewModel.loadStateForReminder(editReminderId)
    }
    LaunchedEffect(viewModel.onComplete) {
        for (event in viewModel.onComplete) onDismiss()
    }

    AddReminderDialogBody(
        onConfirm = viewModel::saveReminder,
        onDismiss = onDismiss,
        editMode = viewModel.editMode.collectAsStateWithLifecycle().value,
        editingReminder = viewModel.editingReminder.collectAsStateWithLifecycle().value,
        hasAnyFeatures = viewModel.hasAnyFeatures.collectAsStateWithLifecycle().value,
    )
}

@Composable
private fun AddReminderDialogBody(
    onConfirm: (ReminderInput) -> Unit,
    onDismiss: () -> Unit,
    editMode: Boolean,
    editingReminder: Reminder? = null,
    hasAnyFeatures: Boolean = false,
) {
    if (editMode && editingReminder != null) {
        when (val params = editingReminder.params) {
            is ReminderParams.WeekDayParams -> WeekDayReminderConfigurationScreen(
                editReminder = editingReminder,
                editParams = params,
                onUpsertReminder = onConfirm,
                onDismiss = onDismiss,
            )
            is ReminderParams.PeriodicParams -> PeriodicReminderConfigurationScreen(
                editReminder = editingReminder,
                editParams = params,
                onUpsertReminder = onConfirm,
                onDismiss = onDismiss,
            )
            is ReminderParams.MonthDayParams -> MonthDayReminderConfigurationScreen(
                editReminder = editingReminder,
                editParams = params,
                onUpsertReminder = onConfirm,
                onDismiss = onDismiss,
            )
            is ReminderParams.TimeSinceLastParams -> TimeSinceLastReminderConfigurationScreen(
                editReminder = editingReminder,
                editParams = params,
                onUpsertReminder = onConfirm,
                onDismiss = onDismiss,
            )
        }
    } else {
        AddReminderDialogContent(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            hasAnyFeatures = hasAnyFeatures,
        )
    }
}


@Preview
@Composable
private fun AddReminderDialogPreview() {
    AddReminderDialogBody(
        onConfirm = {},
        onDismiss = {},
        editMode = false,
    )
}
