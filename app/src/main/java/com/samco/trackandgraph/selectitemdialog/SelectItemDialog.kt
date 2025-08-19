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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
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
    onFeatureSelected: ((Long) -> Unit)? = null,
    onGraphSelected: ((Long) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    resetOnClose: Boolean = false,
) {
    val viewModel: SelectItemDialogViewModel = hiltViewModel<SelectItemDialogViewModelImpl>()

    LaunchedEffect(selectableTypes, hiddenItems) {
        viewModel.init(selectableTypes, hiddenItems)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupTree by viewModel.groupTree.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()


    SelectItemDialogContent(
        state = state,
        title = title,
        groupTree = groupTree,
        selectedItem = selectedItem,
        lazyListState = viewModel.lazyListState,
        horizontalScrollState = viewModel.horizontalScrollState,
        onDismissRequest = {
            onDismissRequest()
            if (resetOnClose) viewModel.reset()
        },
        onItemSelected = viewModel::onItemClicked,
        onContinue = {
            selectedItem?.let { item ->
                when (item) {
                    is GraphNode.Group -> onGroupSelected?.invoke(item.id)
                    is GraphNode.Tracker -> {
                        onTrackerSelected?.invoke(item.trackerId)
                        onFeatureSelected?.invoke(item.featureId)
                    }

                    is GraphNode.Graph -> onGraphSelected?.invoke(item.id)
                }
                onDismissRequest()
                if (resetOnClose) viewModel.reset()
            }
        },
        continueEnabled = selectedItem != null,
    )
}

@Composable
private fun SelectItemDialogContent(
    state: SelectItemDialogState,
    title: String,
    groupTree: GraphNode?,
    selectedItem: GraphNode?,
    lazyListState: LazyListState = LazyListState(),
    horizontalScrollState: ScrollState = ScrollState(0),
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
            style = MaterialTheme.typography.titleSmall,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    SelectItemList(
                        rootNode = groupTree,
                        selectedItem = selectedItem,
                        lazyListState = lazyListState,
                        horizontalScrollState = horizontalScrollState,
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
    selectedItem: GraphNode?,
    lazyListState: LazyListState = LazyListState(),
    horizontalScrollState: ScrollState = ScrollState(0),
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
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState),
    ) {
        graphNodeItem(
            node = rootNode,
            indentLevel = 0,
            minWidth = maxWidth,
            selectedItem = selectedItem,
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
    selectedItem: GraphNode? = null,
    onWidthMeasured: (Dp) -> Unit = {},
    onItemSelected: (GraphNode) -> Unit
) {
    item {
        SelectableItemRow(
            modifier = Modifier.widthIn(min = minWidth),
            item = node,
            indentLevel = indentLevel,
            selectedItem = selectedItem,
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
                selectedItem = selectedItem,
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
    selectedItem: GraphNode? = null,
    onWidthMeasured: (Dp) -> Unit = {},
    onClick: () -> Unit
) {
    when (item) {
        is GraphNode.Group -> GroupItemRow(
            modifier = modifier,
            item = item,
            indentLevel = indentLevel,
            isSelected = selectedItem == item,
            onWidthMeasured = onWidthMeasured,
            onClick = onClick
        )

        is GraphNode.Tracker -> IconItemRow(
            modifier = modifier,
            name = item.name,
            icon = R.drawable.add_box,
            indentLevel = indentLevel,
            isSelected = selectedItem == item,
            onWidthMeasured = onWidthMeasured,
            onClick = onClick
        )

        is GraphNode.Graph -> IconItemRow(
            modifier = modifier,
            name = item.name,
            icon = R.drawable.chart_data,
            indentLevel = indentLevel,
            isSelected = selectedItem == item,
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
    isSelected: Boolean = false,
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
            .let {
                if (isSelected) {
                    it.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
                } else it
            }
            .padding(vertical = cardPadding, horizontal = halfDialogInputSpacing),
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
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun GroupItemRow(
    modifier: Modifier = Modifier,
    item: GraphNode.Group,
    indentLevel: Int = 0,
    isSelected: Boolean = false,
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
            .let {
                if (isSelected) {
                    it.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
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
            tint = MaterialTheme.colorScheme.onSurface
        )
        HalfDialogInputSpacing()
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
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
            selectedItem = null,
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
            selectedItem = GraphNode.Graph(
                id = 21L,
                name = "Stress Level Trends Analysis"
            ),
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
                                trackerId = 10L,
                                featureId = 10L,
                                name = "Daily Weight Measurements & Body Composition"
                            ),
                            GraphNode.Graph(
                                id = 20L,
                                name = "Weight Loss Progress Over Time Chart"
                            ),
                            GraphNode.Tracker(
                                trackerId = 11L,
                                featureId = 11L,
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
                                trackerId = 12L,
                                featureId = 12L,
                                name = "Daily Mood Rating Scale (1-10)"
                            ),
                            GraphNode.Graph(
                                id = 21L,
                                name = "Stress Level Trends Analysis"
                            )
                        )
                    ),
                    GraphNode.Tracker(
                        trackerId = 13L,
                        featureId = 13L,
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
            selectedItem = null,
            continueEnabled = false
        )
    }
}
