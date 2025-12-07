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
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit
) {
    val viewModel: AddReminderViewModel = hiltViewModel<AddReminderViewModelImpl>()

    LaunchedEffect(viewModel.onAddComplete) {
        for (event in viewModel.onAddComplete) onDismiss()
    }

    AddReminderDialog(
        onAddReminder = viewModel::addReminder,
        onDismiss = onDismiss
    )
}

@Composable
private fun AddReminderDialog(
    onAddReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) = CustomDialog(
    onDismissRequest = onDismiss,
    scrollContent = false,
    paddingValues = PaddingValues(
        start = inputSpacingLarge,
        end = inputSpacingLarge,
        bottom = halfDialogInputSpacing,
        top = inputSpacingLarge,
    )
) {
    AddReminderDialogContent(
        onAddReminder = onAddReminder,
        onDismiss = onDismiss
    )
}


@Preview
@Composable
private fun AddReminderDialogPreview() {
    AddReminderDialog(
        onAddReminder = {},
        onDismiss = {}
    )
}