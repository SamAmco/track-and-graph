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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.Flow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.permissions.rememberAlarmAndNotificationPermissionRequester
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.EmptyPageHintText
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.WideButton
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.inputSpacingXLarge
import kotlinx.serialization.Serializable
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Serializable
data object RemindersNavKey : NavKey

@Composable
fun RemindersScreen(navArgs: RemindersNavKey) {
    val viewModel: RemindersViewModel = hiltViewModel<RemindersViewModelImpl>()

    val reminders by viewModel.currentReminders.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val hasChanges by viewModel.remindersChanged.collectAsState()

    TopAppBarContent(navArgs)

    RemindersScreen(
        reminders = reminders,
        isLoading = isLoading,
        hasChanges = hasChanges,
        lazyListState = viewModel.lazyListState,
        scrollToNewItem = viewModel.scrollToNewItem,
        onSaveChanges = viewModel::saveChanges,
        onDeleteReminder = viewModel::deleteReminder,
        onMoveReminder = { from, to -> viewModel.moveItem(from, to) },
    )
}

@Composable
private fun TopAppBarContent(navArgs: RemindersNavKey) {
    val topBarController = LocalTopBarController.current
    val title = stringResource(R.string.reminders)
    val defaultReminderName = stringResource(R.string.default_reminder_name)
    val viewModel: RemindersViewModel = hiltViewModel<RemindersViewModelImpl>()
    val permissionRequester = rememberAlarmAndNotificationPermissionRequester()

    val actions: @Composable RowScope.() -> Unit = remember(
        viewModel,
        defaultReminderName,
        permissionRequester
    ) {
        {
            IconButton(
                onClick = {
                    viewModel.addReminder(defaultReminderName)
                    permissionRequester()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    }

    topBarController.Set(
        navArgs,
        AppBarConfig(
            title = title,
            actions = actions
        )
    )
}

@Composable
fun RemindersScreen(
    reminders: List<ReminderViewData>,
    isLoading: Boolean,
    hasChanges: Boolean,
    lazyListState: LazyListState,
    scrollToNewItem: Flow<Int>,
    onSaveChanges: () -> Unit,
    onDeleteReminder: (ReminderViewData) -> Unit,
    onMoveReminder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveReminder(from.index, to.index)
    }

    // Handle scroll to new item
    LaunchedEffect(scrollToNewItem, lazyListState) {
        scrollToNewItem.collect { itemIndex ->
            lazyListState.animateScrollToItem(itemIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                contentPadding = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
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
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Save changes button
        if (hasChanges) {
            WideButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        WindowInsets.safeDrawing
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            .asPaddingValues()
                    )
                    .then(Modifier.padding(inputSpacingLarge)),
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
                lazyListState = LazyListState(),
                scrollToNewItem = kotlinx.coroutines.flow.emptyFlow(),
                onSaveChanges = {},
                onDeleteReminder = {},
                onMoveReminder = { _, _ -> }
            )
        }
    }
}
