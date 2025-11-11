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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngTypography
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

/**
 * Lightweight view layer representation of a function for UI display
 */
data class DisplayFunction(
    val id: Long,
    val featureId: Long,
    val name: String,
    val description: String
)

/**
 * Composable that displays a function item card with context menu and click handling.
 */
@Composable
fun Function(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    displayFunction: DisplayFunction,
    onEdit: (DisplayFunction) -> Unit,
    onDelete: (DisplayFunction) -> Unit,
    onMoveTo: (DisplayFunction) -> Unit,
    onDuplicate: (DisplayFunction) -> Unit,
    onClick: (DisplayFunction) -> Unit,
) = Box(modifier = modifier.fillMaxWidth()) {
    var showContextMenu by remember { mutableStateOf(false) }

    val functionFont = MaterialTheme.tngTypography.code.copy(
        fontSize = MaterialTheme.typography.labelLarge.fontSize,
    )
    val fontSizeDp = with(LocalDensity.current) {
        functionFont.fontSize.toDp()
    }
    val peekDistance = fontSizeDp + (dialogInputSpacing * 1.15f)

    Card(
        modifier = Modifier
            .testTag("functionCard")
            .fillMaxWidth()
            .padding(cardMarginSmall)
            .clickable { onClick(displayFunction) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isElevated) cardElevation * 3 else cardElevation),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minTrackerCardHeight)
        ) {
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(bottom = peekDistance)
                    .heightIn(min = (minTrackerCardHeight - peekDistance)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Menu button in top end
                    FunctionMenuButton(
                        modifier = Modifier.align(Alignment.End),
                        showContextMenu = showContextMenu,
                        onShowContextMenu = { showContextMenu = it },
                        displayFunction = displayFunction,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onMoveTo = onMoveTo,
                        onDuplicate = onDuplicate
                    )
                    // Function name
                    FunctionNameText(functionName = displayFunction.name)
                    InputSpacingLarge()
                }
            }
            Text(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(halfDialogInputSpacing),
                text = stringResource(R.string.function).lowercase(),
                style = functionFont
            )
        }
    }
}

@Composable
private fun FunctionMenuButton(
    modifier: Modifier = Modifier,
    showContextMenu: Boolean,
    onShowContextMenu: (Boolean) -> Unit,
    displayFunction: DisplayFunction,
    onEdit: (DisplayFunction) -> Unit,
    onDelete: (DisplayFunction) -> Unit,
    onMoveTo: (DisplayFunction) -> Unit,
    onDuplicate: (DisplayFunction) -> Unit
) {
    Box(modifier = modifier) {
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
                    onEdit(displayFunction)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    onShowContextMenu(false)
                    onDelete(displayFunction)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move_to)) },
                onClick = {
                    onShowContextMenu(false)
                    onMoveTo(displayFunction)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                onClick = {
                    onShowContextMenu(false)
                    onDuplicate(displayFunction)
                }
            )
        }
    }
}

@Composable
private fun FunctionNameText(
    functionName: String
) {
    Text(
        text = functionName,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inputSpacingLarge),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        maxLines = 10,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview(showBackground = true)
@Composable
fun FunctionPreview() {
    TnGComposeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Function(
                displayFunction = DisplayFunction(
                    id = 1,
                    featureId = 101,
                    name = "Calculate Average",
                    description = "Calculates the average of selected data points"
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDuplicate = {},
                onClick = {}
            )

            Function(
                displayFunction = DisplayFunction(
                    id = 2,
                    featureId = 102,
                    name = "Weekly Summary Report",
                    description = "Generates a comprehensive weekly summary with trends and insights"
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDuplicate = {},
                onClick = {}
            )
        }
    }
}
