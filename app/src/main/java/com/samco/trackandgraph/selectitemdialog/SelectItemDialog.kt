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
package com.samco.trackandgraph.selectitemdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.smallIconSize
import com.samco.trackandgraph.ui.dataVisColorList

enum class HiddenItemType { TRACKER, GROUP, GRAPH }

data class HiddenItem(
    val type: HiddenItemType,
    val id: Long
)

@Composable
fun SelectItemDialog(
    title: String,
    selectableTypes: Set<SelectableItemType>,
    hiddenItems: Set<HiddenItem> = emptySet(),
    onItemSelected: (SelectableItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val viewModel: SelectItemDialogViewModel = hiltViewModel()

    LaunchedEffect(selectableTypes, hiddenItems) {
        viewModel.init(selectableTypes, hiddenItems)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    SelectItemDialogContent(
        state = state,
        title = title,
        items = items,
        onItemSelected = onItemSelected,
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        }
    )
}

@Composable
private fun SelectItemDialogContent(
    state: SelectItemDialogState,
    title: String,
    items: List<SelectableItem>,
    onItemSelected: (SelectableItem) -> Unit,
    onDismissRequest: () -> Unit
) = CustomDialog(
    scrollContent = false,
    onDismissRequest = onDismissRequest
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val dialogWidth = maxWidth

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )

            InputSpacingLarge()

            when (state) {
                SelectItemDialogState.LOADING -> {
                    LoadingOverlay(modifier = Modifier.height(200.dp))
                }

                SelectItemDialogState.READY -> {
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_data),
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            items(items) { item ->
                                SelectableItemRow(
                                    modifier = Modifier.widthIn(min = dialogWidth),
                                    item = item,
                                    onClick = { onItemSelected(item) }
                                )
                            }
                        }
                    }
                }
            }

            DialogInputSpacing()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun SelectableItemRow(
    modifier: Modifier = Modifier,
    item: SelectableItem,
    onClick: () -> Unit
) {
    when (item) {
        is SelectableItem.Group -> GroupItemRow(
            modifier = modifier,
            item = item,
            onClick = onClick
        )
    }
}

@Composable
private fun GroupItemRow(
    modifier: Modifier = Modifier,
    item: SelectableItem.Group,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(smallIconSize)
                .background(
                    color = colorResource(id = dataVisColorList[item.colorIndex]),
                    shape = MaterialTheme.shapes.small
                )
        )
        HalfDialogInputSpacing()
        Text(
            text = item.path,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SelectItemDialogContentLoadingPreview() {
    TnGComposeTheme {
        SelectItemDialogContent(
            state = SelectItemDialogState.LOADING,
            title = "Select Group",
            items = emptyList(),
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}

@Preview
@Composable
private fun SelectItemDialogContentReadyPreview() {
    TnGComposeTheme {
        SelectItemDialogContent(
            state = SelectItemDialogState.READY,
            title = "Select Group",
            items = listOf(
                SelectableItem.Group(
                    id = 1L,
                    path = "Health",
                    colorIndex = 0,
                ),
                SelectableItem.Group(
                    id = 2L,
                    path = "Health/Fitness",
                    colorIndex = 1,
                ),
                SelectableItem.Group(
                    id = 3L,
                    path = "Health/Fitness/Weight Tracker",
                    colorIndex = 2,
                ),
                SelectableItem.Group(
                    id = 4L,
                    path = "Health/Fitness/Weight Graph",
                    colorIndex = 3,
                ),
                SelectableItem.Group(
                    id = 5L,
                    path = "Some/Really/Deeply/Nested/Tracker/That/Forces/Us/To/Scroll",
                    colorIndex = 4,
                ),
            ),
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}

@Preview
@Composable
private fun SelectItemDialogContentEmptyPreview() {
    TnGComposeTheme {
        SelectItemDialogContent(
            state = SelectItemDialogState.READY,
            title = "Select Group",
            items = emptyList(),
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}
