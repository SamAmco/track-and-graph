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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton

@Composable
internal fun GroupDeleteDialog(
    groupDialogsViewModel: GroupDialogsViewModel,
    groupViewModel: GroupViewModel,
) {
    val itemForDeletion = groupDialogsViewModel.itemForDeletion.collectAsStateWithLifecycle().value
        ?: return

    val onDismiss = { groupDialogsViewModel.hideDeleteDialog() }

    GroupDeleteDialogContent(
        itemForDeletion = itemForDeletion,
        onDismiss = onDismiss,
        onDeleteEverywhere = {
            when (itemForDeletion.type) {
                DeleteType.GROUP -> groupViewModel.onDeleteGroup(itemForDeletion.id)
                DeleteType.GRAPH_STAT -> groupViewModel.onDeleteGraphStat(itemForDeletion.id)
                DeleteType.TRACKER -> groupViewModel.onDeleteTracker(itemForDeletion.id)
                DeleteType.FUNCTION -> groupViewModel.onDeleteFunction(itemForDeletion.id)
            }
            onDismiss()
        },
        onRemoveFromGroup = {
            when (itemForDeletion.type) {
                DeleteType.TRACKER -> groupViewModel.onDeleteTracker(
                    itemForDeletion.id,
                    itemForDeletion.groupId,
                )
                else -> Unit
            }
            onDismiss()
        },
    )
}

@Composable
private fun GroupDeleteDialogContent(
    itemForDeletion: DeleteItemDto,
    onDismiss: () -> Unit,
    onDeleteEverywhere: () -> Unit,
    onRemoveFromGroup: () -> Unit,
) {
    if (itemForDeletion.unique) {
        val bodyRes = when (itemForDeletion.type) {
            DeleteType.GROUP -> R.string.ru_sure_del_group
            DeleteType.GRAPH_STAT -> R.string.ru_sure_del_graph
            DeleteType.TRACKER -> R.string.ru_sure_del_feature
            DeleteType.FUNCTION -> R.string.ru_sure_del_function
        }
        ContinueCancelDialog(
            body = bodyRes,
            onDismissRequest = onDismiss,
            onConfirm = onDeleteEverywhere,
            continueText = R.string.delete,
            cancelText = R.string.cancel,
        )
    } else {
        CustomDialog(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(R.string.item_in_multiple_groups),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DialogInputSpacing()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                SmallTextButton(
                    stringRes = R.string.delete_everywhere,
                    onClick = onDeleteEverywhere,
                )
                SmallTextButton(
                    stringRes = R.string.remove_from_this_group,
                    onClick = onRemoveFromGroup,
                )
                SmallTextButton(
                    stringRes = R.string.cancel,
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun GroupDeleteDialogUniquePreview() {
    TnGComposeTheme {
        GroupDeleteDialogContent(
            itemForDeletion = DeleteItemDto(
                id = 1L,
                type = DeleteType.TRACKER,
                unique = true,
            ),
            onDismiss = {},
            onDeleteEverywhere = {},
            onRemoveFromGroup = {},
        )
    }
}

@Preview
@Composable
private fun GroupDeleteDialogMultiGroupPreview() {
    TnGComposeTheme {
        GroupDeleteDialogContent(
            itemForDeletion = DeleteItemDto(
                id = 1L,
                type = DeleteType.TRACKER,
                unique = false,
                groupId = 2L,
            ),
            onDismiss = {},
            onDeleteEverywhere = {},
            onRemoveFromGroup = {},
        )
    }
}
