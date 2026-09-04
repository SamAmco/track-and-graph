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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Top app bar content for the Group screen with proper memoization to avoid unnecessary recompositions
 */
@Composable
internal fun GroupTopBarContent(
    navArgs: GroupNavKey,
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    onAddComponent: () -> Unit,
    showFab: MutableState<Boolean>,
    onSearchClick: () -> Unit,
) {
    val topBarController = LocalTopBarController.current

    val isRootGroup = navArgs.groupId == 0L
    val vmGroupName by groupViewModel.groupName.collectAsStateWithLifecycle()
    val title = when {
        isRootGroup -> stringResource(R.string.app_name)
        else -> navArgs.groupName ?: vmGroupName ?: ""
    }

    // Memoize the nested scroll connection to avoid recreating on every recomposition
    val nestedScrollConnection = remember(showFab) {
        createNestedScrollConnection(showFab)
    }

    // Memoize the actions composable to avoid recreating on every recomposition
    val actions = remember(
        groupDialogsViewModel,
        onAddComponent,
        onSearchClick,
    ) {
        createTopBarActions(
            groupDialogsViewModel = groupDialogsViewModel,
            onAddComponent = onAddComponent,
            onSearchClick = onSearchClick,
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
    onAddComponent: () -> Unit,
    onSearchClick: () -> Unit,
): @Composable RowScope.() -> Unit {
    return {
        // Import/Export action
        IconButton(onClick = { groupDialogsViewModel.showImportExportDialog() }) {
            Icon(painterResource(R.drawable.swap_vert_icon), stringResource(R.string.import_export))
        }
        // Search action
        IconButton(onClick = onSearchClick) {
            Icon(painterResource(R.drawable.search_icon), stringResource(R.string.search))
        }
        IconButton(onClick = onAddComponent) {
            Icon(painterResource(R.drawable.add_icon), stringResource(R.string.add))
        }
    }
}
