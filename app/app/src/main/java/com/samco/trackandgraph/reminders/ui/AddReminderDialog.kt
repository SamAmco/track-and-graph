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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import kotlinx.serialization.Serializable

private sealed class AddReminderSessionNavKey : NavKey {
    @Serializable
    data class Session(val editReminderId: Long?) : AddReminderSessionNavKey()
}

@Composable
fun AddReminderDialog(
    visible: Boolean,
    editReminderId: Long?,
    onDismiss: () -> Unit
) {
    val navBackStack = rememberNavBackStack()
    val sessionKey = AddReminderSessionNavKey.Session(editReminderId)

    LaunchedEffect(visible, sessionKey) {
        when {
            visible && navBackStack.lastOrNull() != sessionKey -> {
                navBackStack.clear()
                navBackStack.add(sessionKey)
            }
            !visible && navBackStack.isNotEmpty() -> navBackStack.clear()
        }
    }

    val dismiss = {
        navBackStack.clear()
        onDismiss()
    }

    val entries = rememberDecoratedNavEntries(
        backStack = navBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { navKey -> addReminderSessionEntry(navKey, dismiss) },
    )

    if (visible && entries.isNotEmpty()) {
        CustomDialog(
            onDismissRequest = dismiss,
            supportSmoothHeightAnimation = true,
            paddingValues = PaddingValues(
                start = inputSpacingLarge,
                end = inputSpacingLarge,
                bottom = halfDialogInputSpacing,
                top = inputSpacingLarge,
            )
        ) {
            NavDisplay(
                entries = entries,
                onBack = dismiss,
            )
        }
    }
}

private fun addReminderSessionEntry(
    navKey: NavKey,
    onDismiss: () -> Unit,
): NavEntry<NavKey> = when (navKey) {
    is AddReminderSessionNavKey.Session -> NavEntry(navKey) {
        AddReminderSessionDestination(
            editReminderId = navKey.editReminderId,
            onDismiss = onDismiss,
        )
    }
    else -> error("Unknown navKey: $navKey")
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
