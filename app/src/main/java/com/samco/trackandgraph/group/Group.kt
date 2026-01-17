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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.dataVisColorList

private val cornerImageSize = 140.dp to 70.dp
private val minHeight = 80.dp

/**
 * Composable that displays a group item card with context menu
 * and click handling for navigation.
 */
@Composable
fun Group(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    group: Group,
    onEdit: (Group) -> Unit,
    onDelete: (Group) -> Unit,
    onMoveTo: (Group) -> Unit,
    onClick: (Group) -> Unit,
) = Box(modifier = modifier.fillMaxWidth()) {
    var showContextMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(cardMarginSmall),
        shadowElevation = if (isElevated) cardElevation * 3 else cardElevation,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .testTag("groupCard")
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clickable { onClick(group) }
        ) {
            // Corner decoration image
            Image(
                painter = painterResource(R.drawable.group_tab_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(cornerImageSize.first, cornerImageSize.second),
                colorFilter = ColorFilter.tint(
                    dataVisColorList[group.colorIndex]
                )
            )

            // Menu button
            GroupMenuButton(
                modifier = Modifier.align(Alignment.TopEnd),
                showContextMenu = showContextMenu,
                onShowContextMenu = { showContextMenu = it },
                group = group,
                onEdit = onEdit,
                onDelete = onDelete,
                onMoveTo = onMoveTo
            )

            // Group name text
            Text(
                text = group.name,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = cardPadding, vertical = inputSpacingLarge),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GroupMenuButton(
    modifier: Modifier = Modifier,
    showContextMenu: Boolean,
    onShowContextMenu: (Boolean) -> Unit,
    group: Group,
    onEdit: (Group) -> Unit,
    onDelete: (Group) -> Unit,
    onMoveTo: (Group) -> Unit
) {
    Box(
        modifier = modifier.size(buttonSize)
    ) {
        IconButton(
            modifier = Modifier.size(buttonSize),
            onClick = { onShowContextMenu(true) },
        ) {
            Icon(
                painterResource(R.drawable.list_menu_icon),
                contentDescription = stringResource(R.string.tracked_data_menu_button_content_description),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { onShowContextMenu(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    onShowContextMenu(false)
                    onEdit(group)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    onShowContextMenu(false)
                    onDelete(group)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move_to)) },
                onClick = {
                    onShowContextMenu(false)
                    onMoveTo(group)
                }
            )
        }
    }
}

@Preview
@Composable
private fun GroupPreview() {
    TnGComposeTheme {
        Group(
            group = Group(
                id = 1,
                name = "Health & Fitness",
                colorIndex = 2,
                displayIndex = 0,
                parentGroupId = null
            ),
            onEdit = {},
            onDelete = {},
            onMoveTo = {},
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun GroupElevatedPreview() {
    TnGComposeTheme {
        Group(
            isElevated = true,
            group = Group(
                id = 2,
                name = "Work & Productivity Goals",
                colorIndex = 5,
                displayIndex = 0,
                parentGroupId = null
            ),
            onEdit = {},
            onDelete = {},
            onMoveTo = {},
            onClick = {}
        )
    }
}
