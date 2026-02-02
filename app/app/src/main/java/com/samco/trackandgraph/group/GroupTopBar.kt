/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.group

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import com.samco.trackandgraph.R
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import kotlinx.coroutines.launch

/**
 * Top app bar content for the Group screen with proper memoization to avoid unnecessary recompositions
 */
@Composable
internal fun GroupTopBarContent(
    navArgs: GroupNavKey,
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    onAddTracker: (Long) -> Unit,
    onAddGraphStat: (Long) -> Unit,
    showFab: MutableState<Boolean>,
    onAddFunction: (Long) -> Unit,
) {
    val topBarController = LocalTopBarController.current

    val isRootGroup = navArgs.groupId == 0L
    val title = if (isRootGroup) stringResource(R.string.app_name) else navArgs.groupName ?: ""

    // Memoize the nested scroll connection to avoid recreating on every recomposition
    val nestedScrollConnection = remember(showFab) {
        createNestedScrollConnection(showFab)
    }

    // Memoize the actions composable to avoid recreating on every recomposition
    val actions = remember(
        groupDialogsViewModel,
        addGroupDialogViewModel,
        groupViewModel,
        onAddTracker,
        onAddGraphStat,
        navArgs.groupId
    ) {
        createTopBarActions(
            groupDialogsViewModel = groupDialogsViewModel,
            addGroupDialogViewModel = addGroupDialogViewModel,
            groupViewModel = groupViewModel,
            onAddTracker = onAddTracker,
            onAddGraphStat = onAddGraphStat,
            onAddFunction = onAddFunction,
            groupId = navArgs.groupId
        )
    }

    topBarController.Set(
        navArgs,
        AppBarConfig(
            title = title,
            backNavigationAction = !isRootGroup,
            nestedScrollConnection = nestedScrollConnection,
            actions = actions
        )
    )
}

/**
 * Creates a nested scroll connection that controls FAB visibility based on scroll direction
 */
private fun createNestedScrollConnection(showFab: MutableState<Boolean>): NestedScrollConnection {
    return object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            when {
                dy < 0 -> showFab.value = false
                dy > 0 -> showFab.value = true
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val vy = available.y
            when {
                vy < 0 -> showFab.value = true
                vy > 0 -> showFab.value = false
            }
            return Velocity.Zero
        }
    }
}

/**
 * Creates the top bar actions composable with proper memoization
 */
private fun createTopBarActions(
    groupDialogsViewModel: GroupDialogsViewModel,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    groupViewModel: GroupViewModel,
    onAddTracker: (Long) -> Unit,
    onAddGraphStat: (Long) -> Unit,
    onAddFunction: (Long) -> Unit,
    groupId: Long
): @Composable RowScope.() -> Unit {
    return {
        // Import CSV action
        IconButton(onClick = { groupDialogsViewModel.showImportDialog() }) {
            Icon(painterResource(R.drawable.import_icon), null)
        }
        // Export CSV action  
        IconButton(onClick = { groupDialogsViewModel.showExportDialog() }) {
            Icon(painterResource(R.drawable.export_icon), null)
        }
        // Add dropdown menu
        GroupAddDropdownMenu(
            groupViewModel = groupViewModel,
            groupDialogsViewModel = groupDialogsViewModel,
            addGroupDialogViewModel = addGroupDialogViewModel,
            onAddTracker = onAddTracker,
            onAddGraphStat = onAddGraphStat,
            onAddFunction = onAddFunction,
            groupId = groupId
        )
    }
}

/**
 * Dropdown menu for adding new items (tracker, graph/stat, group)
 */
@Composable
private fun GroupAddDropdownMenu(
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    onAddTracker: (Long) -> Unit,
    onAddGraphStat: (Long) -> Unit,
    onAddFunction: (Long) -> Unit,
    groupId: Long
) {
    var showAddMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    IconButton(onClick = { showAddMenu = true }) {
        Icon(painterResource(R.drawable.add_icon), stringResource(R.string.add))
        DropdownMenu(
            expanded = showAddMenu,
            onDismissRequest = { showAddMenu = false }
        ) {
            // Add Tracker
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tracker)) },
                onClick = {
                    showAddMenu = false
                    onAddTracker(groupId)
                }
            )
            // Add Graph/Stat
            DropdownMenuItem(
                text = { Text(stringResource(R.string.graph_or_stat)) },
                onClick = {
                    showAddMenu = false
                    scope.launch {
                        // Check if user has trackers before navigating
                        if (groupViewModel.userHasAnyTrackers()) {
                            onAddGraphStat(groupId)
                        } else {
                            groupDialogsViewModel.showNoTrackersDialog()
                        }
                    }
                }
            )
            // Add Group
            DropdownMenuItem(
                text = { Text(stringResource(R.string.group)) },
                onClick = {
                    showAddMenu = false
                    addGroupDialogViewModel.showForCreate(parentGroupId = groupId)
                }
            )
            // Add Function
            DropdownMenuItem(
                text = { Text(stringResource(R.string.function)) },
                onClick = {
                    showAddMenu = false
                    scope.launch {
                        if (groupViewModel.userHasAnyTrackers()) {
                            onAddFunction(groupId)
                        } else {
                            groupDialogsViewModel.showNoTrackersFunctionsDialog()
                        }
                    }
                }
            )
        }
    }
}
