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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.smallIconSize
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.alpha

@Composable
fun SelectItemDialog(
    title: String,
    selectableTypes: Set<SelectableItemType>,
    hiddenItems: Set<HiddenItem> = emptySet(),
    onGroupSelected: ((Long) -> Unit)? = null,
    onTrackerSelected: ((Long) -> Unit)? = null,
    onGraphSelected: ((Long) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val viewModel: SelectItemDialogViewModel = hiltViewModel<SelectItemDialogViewModelImpl>()

    LaunchedEffect(selectableTypes, hiddenItems) {
        viewModel.init(selectableTypes, hiddenItems)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupTree by viewModel.groupTree.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.selectionEvents) {
        for (event in viewModel.selectionEvents) {
            when (event) {
                is SelectionEvent.TrackerSelected -> onTrackerSelected?.invoke(event.trackerId)
                is SelectionEvent.GraphSelected -> onGraphSelected?.invoke(event.graphId)
            }
        }
    }

    SelectItemDialogContent(
        state = state,
        title = title,
        groupTree = groupTree,
        selectedGroupId = selectedGroupId,
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        onItemSelected = viewModel::onItemClicked,
        onContinue = {
            selectedGroupId?.let { onGroupSelected?.invoke(it) }
            viewModel.reset()
            onDismissRequest()
        },
        continueEnabled = selectedGroupId != null,
    )
}

@Composable
private fun SelectItemDialogContent(
    state: SelectItemDialogState,
    title: String,
    groupTree: GraphNode?,
    selectedGroupId: Long?,
    onItemSelected: (GraphNode) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onContinue: () -> Unit = {},
    continueEnabled: Boolean,
) = CustomContinueCancelDialog(
    scrollContent = false,
    continueEnabled = continueEnabled,
    onDismissRequest = onDismissRequest,
    onConfirm = onContinue,
) {
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
                if (groupTree == null) {
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
                    SelectItemList(
                        rootNode = groupTree,
                        selectedGroupId = selectedGroupId,
                        onItemSelected = onItemSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectItemList(
    rootNode: GraphNode,
    selectedGroupId: Long?,
    onItemSelected: (GraphNode) -> Unit
) = BoxWithConstraints {
    // Use dialog width as the default minimum width
    val dialogWidth = maxWidth

    // Track max width for consistent sizing, starting with dialog width
    var maxWidth by remember { mutableStateOf(dialogWidth) }

    // Buffer for collecting width measurements
    val widthBuffer = remember { mutableListOf<Dp>() }

    // Trigger for debouncing - increment when new measurements are added
    var bufferUpdateTrigger by remember { mutableIntStateOf(0) }

    // Debounce width updates to avoid layout thrashing
    LaunchedEffect(bufferUpdateTrigger) {
        if (bufferUpdateTrigger > 0) {
            delay(100) // Wait 100ms after last measurement

            if (widthBuffer.isNotEmpty()) {
                val newMaxWidth = widthBuffer.maxOrNull() ?: dialogWidth
                if (newMaxWidth > maxWidth) {
                    maxWidth = newMaxWidth
                }
                widthBuffer.clear()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        graphNodeItem(
            node = rootNode,
            indentLevel = 0,
            minWidth = maxWidth,
            selectedGroupId = selectedGroupId,
            onWidthMeasured = { measuredWidth ->
                // Add to buffer and trigger debounce
                widthBuffer.add(measuredWidth)
                bufferUpdateTrigger++
            },
            onItemSelected = onItemSelected
        )
    }
}

private fun LazyListScope.graphNodeItem(
    node: GraphNode,
    indentLevel: Int = 0,
    minWidth: Dp = 0.dp,
    selectedGroupId: Long? = null,
    onWidthMeasured: (Dp) -> Unit = {},
    onItemSelected: (GraphNode) -> Unit
) {
    item {
        SelectableItemRow(
            modifier = Modifier.widthIn(min = minWidth),
            item = node,
            indentLevel = indentLevel,
            selectedGroupId = selectedGroupId,
            onWidthMeasured = onWidthMeasured,
            onClick = { onItemSelected(node) }
        )
    }

    // Recursively add children if this is an expanded group
    if (node is GraphNode.Group && node.expanded.value) {
        node.children.forEach { child ->
            graphNodeItem(
                node = child,
                indentLevel = indentLevel + 1,
                minWidth = minWidth,
                selectedGroupId = selectedGroupId,
                onWidthMeasured = onWidthMeasured,
                onItemSelected = onItemSelected
            )
        }
    }
}

@Composable
private fun SelectableItemRow(
    modifier: Modifier = Modifier,
    item: GraphNode,
    indentLevel: Int = 0,
    selectedGroupId: Long? = null,
    onWidthMeasured: (Dp) -> Unit = {},
    onClick: () -> Unit
) {
    when (item) {
        is GraphNode.Group -> GroupItemRow(
            modifier = modifier,
            item = item,
            indentLevel = indentLevel,
            selectedGroupId = selectedGroupId,
            onWidthMeasured = onWidthMeasured,
            onClick = onClick
        )

        is GraphNode.Tracker -> IconItemRow(
            modifier = modifier,
            name = item.name,
            icon = R.drawable.add_box,
            indentLevel = indentLevel,
            onWidthMeasured = onWidthMeasured,
            onClick = onClick
        )

        is GraphNode.Graph -> IconItemRow(
            modifier = modifier,
            name = item.name,
            icon = R.drawable.chart_data,
            indentLevel = indentLevel,
            onWidthMeasured = onWidthMeasured,
            onClick = onClick
        )
    }
}

@Composable
private fun IconItemRow(
    modifier: Modifier = Modifier,
    name: String,
    @DrawableRes icon: Int,
    indentLevel: Int = 0,
    onWidthMeasured: (Dp) -> Unit = {},
    onClick: () -> Unit,
) {
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .clickable { onClick() }
            .onSizeChanged { size ->
                val width = with(density) { size.width.toDp() }
                onWidthMeasured(width)
            }
            .padding(vertical = cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(indentLevel * smallIconSize))
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(smallIconSize)
        )
        HalfDialogInputSpacing()
        Text(
            text = name,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
        )
    }
}

@Composable
private fun GroupItemRow(
    modifier: Modifier = Modifier,
    item: GraphNode.Group,
    indentLevel: Int = 0,
    selectedGroupId: Long? = null,
    onWidthMeasured: (Dp) -> Unit = {},
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val isSelected = selectedGroupId == item.id

    Row(
        modifier = modifier
            .clickable { onClick() }
            .onSizeChanged { size ->
                val width = with(density) { size.width.toDp() }
                onWidthMeasured(width)
            }
            .let {
                if (isSelected) {
                    it.border(
                        width = 2.dp,
                        color = MaterialTheme.colors.primary,
                        shape = MaterialTheme.shapes.small
                    )
                } else it
            }
            .padding(vertical = cardPadding, horizontal = halfDialogInputSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(indentLevel * smallIconSize))
        Icon(
            painter = painterResource(id = R.drawable.down_arrow),
            contentDescription = null,
            modifier = Modifier
                .size(smallIconSize)
                .alpha(if (item.children.isEmpty()) 0f else 1f)
                .rotate(if (item.expanded.value) 0f else -90f),
            tint = MaterialTheme.colors.onSurface
        )
        HalfDialogInputSpacing()
        Text(
            text = item.name,
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
            groupTree = null,
            selectedGroupId = null,
            continueEnabled = false
        )
    }
}

@Preview
@Composable
private fun SelectItemDialogContentPreview() {
    TnGComposeTheme {
        SelectItemDialogContent(
            state = SelectItemDialogState.READY,
            title = "Move To",
            selectedGroupId = 1L,
            continueEnabled = true,
            groupTree = GraphNode.Group(
                id = 1L,
                name = "Health & Wellness Tracking",
                colorIndex = 0,
                expanded = remember { mutableStateOf(true) },
                children = listOf(
                    GraphNode.Group(
                        id = 2L,
                        name = "Physical Fitness & Exercise Routines",
                        colorIndex = 1,
                        expanded = remember { mutableStateOf(false) },
                        children = listOf(
                            GraphNode.Tracker(
                                id = 10L,
                                name = "Daily Weight Measurements & Body Composition"
                            ),
                            GraphNode.Graph(
                                id = 20L,
                                name = "Weight Loss Progress Over Time Chart"
                            ),
                            GraphNode.Tracker(
                                id = 11L,
                                name = "Workout Duration & Intensity Levels"
                            )
                        )
                    ),
                    GraphNode.Group(
                        id = 3L,
                        name = "Mental Health & Mood",
                        colorIndex = 2,
                        expanded = remember { mutableStateOf(true) },
                        children = listOf(
                            GraphNode.Tracker(
                                id = 12L,
                                name = "Daily Mood Rating Scale (1-10)"
                            ),
                            GraphNode.Graph(
                                id = 21L,
                                name = "Stress Level Trends Analysis"
                            )
                        )
                    ),
                    GraphNode.Tracker(
                        id = 13L,
                        name = "Blood Pressure & Heart Rate Monitoring"
                    ),
                    GraphNode.Graph(
                        id = 22L,
                        name = "Overall Health Dashboard Summary"
                    )
                )
            )
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
            groupTree = null,
            selectedGroupId = null,
            continueEnabled = false
        )
    }
}
