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
package com.samco.trackandgraph.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import androidx.compose.ui.res.stringResource

/**
 * Dialog wrapper for creating a symlink. Shows a SelectItemDialog pre-filtered to exclude
 * the current group and its ancestors (to prevent cycles in the group hierarchy).
 */
@Composable
fun AddSymlinkDialog(
    viewModel: AddSymlinkViewModel,
    onDismissRequest: () -> Unit,
) {
    val groupId by viewModel.showDialogForGroupId.collectAsStateWithLifecycle()
    if (groupId == null) return

    val disabledItems by viewModel.disabledItems.collectAsStateWithLifecycle()

    SelectItemDialog(
        title = stringResource(R.string.symlink),
        selectableTypes = setOf(
            SelectableItemType.GROUP,
            SelectableItemType.TRACKER,
            SelectableItemType.GRAPH,
            SelectableItemType.FUNCTION,
        ),
        disabledItems = disabledItems,
        onGroupSelected = { childId ->
            viewModel.createSymlink(groupId!!, childId, GroupChildType.GROUP)
        },
        onTrackerSelected = { trackerId ->
            viewModel.createSymlink(groupId!!, trackerId, GroupChildType.TRACKER)
        },
        onGraphSelected = { graphId ->
            viewModel.createSymlink(groupId!!, graphId, GroupChildType.GRAPH)
        },
        onFunctionSelected = { functionId ->
            viewModel.createSymlink(groupId!!, functionId, GroupChildType.FUNCTION)
        },
        onDismissRequest = onDismissRequest,
        resetOnClose = true,
    )
}
