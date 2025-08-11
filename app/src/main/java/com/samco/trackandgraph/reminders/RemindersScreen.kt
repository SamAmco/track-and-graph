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

package com.samco.trackandgraph.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.EmptyPageHintText
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.WideButton
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.inputSpacingXLarge
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun RemindersScreen(
    reminders: List<ReminderViewData>,
    isLoading: Boolean,
    hasChanges: Boolean,
    onSaveChanges: () -> Unit,
    onDeleteReminder: (ReminderViewData) -> Unit,
    onMoveReminder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveReminder(from.index, to.index)
    }

    Box(
        modifier = modifier
            .imePadding()
            .fillMaxSize()
    ) {
        if (reminders.isEmpty()) {
            // Empty state
            EmptyPageHintText(
                modifier = Modifier
                    .padding(inputSpacingXLarge)
                    .align(Alignment.Center),
                text = stringResource(id = R.string.no_reminders_hint)
            )
        } else {
            // Reminders list with drag-and-drop
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = reminders,
                    key = { it.id }
                ) { reminder ->
                    ReorderableItem(reorderableLazyListState, key = reminder.id) { isDragging ->
                        Reminder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .longPressDraggableHandle(),
                            isElevated = isDragging,
                            reminderViewData = reminder,
                            onDeleteClick = { onDeleteReminder(reminder) }
                        )
                    }
                }
            }
        }

        // Save changes button
        if (hasChanges) {
            WideButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(inputSpacingLarge),
                text = stringResource(id = R.string.save_changes),
                onClick = onSaveChanges
            )
        }

        // Loading overlay
        if (isLoading) LoadingOverlay()
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersScreenPreview() {
    TnGComposeTheme {
        Column {
            // Empty state preview
            RemindersScreen(
                reminders = emptyList(),
                isLoading = false,
                hasChanges = true,
                onSaveChanges = {},
                onDeleteReminder = {},
                onMoveReminder = { _, _ -> }
            )
        }
    }
}
