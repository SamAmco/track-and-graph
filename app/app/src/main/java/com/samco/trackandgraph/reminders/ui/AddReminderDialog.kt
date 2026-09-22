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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDismiss: () -> Unit
) {
    if (!visible) return

    val dialogOwner = rememberViewModelStoreOwner()

    CompositionLocalProvider(LocalViewModelStoreOwner provides dialogOwner) {
        CustomDialog(
            onDismissRequest = onDismiss,
            scrollContent = false,
            supportSmoothHeightAnimation = true,
            paddingValues = PaddingValues(
                start = inputSpacingLarge,
                end = inputSpacingLarge,
                bottom = halfDialogInputSpacing,
                top = inputSpacingLarge,
            )
        ) {
            AddReminderDestination(onDismiss = onDismiss)
        }
    }
}

@Composable
fun EditReminderDialog(
    editReminderId: Long?,
    onDismiss: () -> Unit,
) {
    if (editReminderId == null) return

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
            EditReminderDestination(
                editReminderId = editReminderId,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun AddReminderDestination(onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<AddReminderViewModelImpl>()

    LaunchedEffect(Unit) {
        viewModel.loadStateForReminder(null)
    }
    LaunchedEffect(viewModel.onComplete) {
        for (event in viewModel.onComplete) onDismiss()
    }

    AddReminderDialogContent(
        onConfirm = viewModel::saveReminder,
        onDismiss = onDismiss,
        hasAnyFeatures = viewModel.hasAnyFeatures.collectAsStateWithLifecycle().value,
    )
}

@Composable
private fun EditReminderDestination(
    editReminderId: Long,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<AddReminderViewModelImpl>()
    val sessionState = viewModel.sessionState.collectAsStateWithLifecycle().value

    LaunchedEffect(editReminderId) {
        viewModel.loadStateForReminder(editReminderId)
    }
    LaunchedEffect(viewModel.onComplete) {
        for (event in viewModel.onComplete) onDismiss()
    }
    when (val state = sessionState) {
        ReminderSessionState.Loading,
        ReminderSessionState.Creating -> ReminderLoadingIndicator()

        ReminderSessionState.Missing -> {
            LaunchedEffect(Unit) { onDismiss() }
            ReminderLoadingIndicator()
        }

        is ReminderSessionState.Editing -> EditReminderDialogBody(
            editingReminder = state.reminder,
            onConfirm = viewModel::saveReminder,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ReminderLoadingIndicator() = Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
) {
    CircularProgressIndicator()
}

@Composable
private fun EditReminderDialogBody(
    editingReminder: Reminder,
    onConfirm: (ReminderInput) -> Unit,
    onDismiss: () -> Unit,
) {
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
        is ReminderParams.OneTimeParams -> OneTimeReminderConfigurationScreen(
            editReminder = editingReminder,
            editParams = params,
            onUpsertReminder = onConfirm,
            onDismiss = onDismiss,
        )
    }
}


@Preview
@Composable
private fun AddReminderDialogPreview() {
    AddReminderDialogContent(
        onConfirm = {},
        onDismiss = {},
    )
}
