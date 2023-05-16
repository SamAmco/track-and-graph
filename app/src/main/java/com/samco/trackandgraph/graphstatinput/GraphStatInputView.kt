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

@file:OptIn(ExperimentalComposeUiApi::class)

package com.samco.trackandgraph.graphstatinput

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.graphstatinput.configviews.ui.*
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.*
import java.lang.Float.max
import java.lang.Float.min

@Composable
internal fun GraphStatInputView(
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModel: GraphStatInputViewModel,
    graphStatId: Long
) {
    Surface(color = MaterialTheme.colors.background) {
        Box {
            val loading = viewModel.loading.observeAsState(true)
            val demoViewData by viewModel.demoViewData.observeAsState()

            Column {
                var demoYOffset by remember { mutableStateOf(0f) }
                var demoVisible by remember { mutableStateOf(false) }

                Box(modifier = Modifier.weight(1f)) {
                    GraphStatInputViewForm(
                        viewModelStoreOwner = viewModelStoreOwner,
                        viewModel = viewModel,
                        graphStatId = graphStatId
                    )

                    if (demoVisible) demoViewData?.let { DemoOverlay(demoYOffset, it) }
                }

                if (demoViewData != null) {
                    DemoButton(
                        isPressed = {
                            demoVisible = it
                            if (!it) demoYOffset = 0f
                        },
                        onDragOffset = { demoYOffset -= it })
                }

                AddCreateBar(
                    errorText = viewModel.validationException.observeAsState().value?.errorMessageId,
                    onCreateUpdateClicked = viewModel::createGraphOrStat,
                    isUpdateMode = viewModel.updateMode.observeAsState().value ?: false
                )
            }

            if (loading.value) LoadingOverlay()
        }
    }
}

@Composable
private fun DemoOverlay(
    demoYOffset: Float,
    demoViewData: IGraphStatViewData,
) = Box(
    Modifier
        .background(MaterialTheme.tngColors.surface.copy(alpha = 0.8f))
        .fillMaxSize()
) {
    var displayHeight by remember { mutableStateOf(0f) }
    var demoHeight by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                displayHeight = it.size.height.toFloat()
            }
    ) {
        val invisiblePixels = max(0f, demoHeight - displayHeight)
        val offsetRatio = max(0f, min(demoYOffset * 3f, displayHeight) / max(1f, displayHeight))
        val offset = -offsetRatio * invisiblePixels

        GraphStatCardView(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offset.dp)
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .onSizeChanged { demoHeight = it.height.toFloat() },
            graphStatViewData = demoViewData
        )
    }
}


@Composable
private fun DemoButton(
    isPressed: (Boolean) -> Unit,
    onDragOffset: (Float) -> Unit
) {
    var buttonDown by remember { mutableStateOf(false) }

    val background =
        if (buttonDown) MaterialTheme.tngColors.selectorButtonColor
        else MaterialTheme.tngColors.primary

    Box(
        Modifier
            .background(background)
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.card_padding))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = true)
                    isPressed(true)
                    buttonDown = true
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach {
                            if (it.positionChange() != Offset.Zero) {
                                onDragOffset(it.positionChange().y)
                                it.consume()
                            }
                        }
                    } while (event.changes.all { it.pressed })
                    isPressed(false)
                    buttonDown = false
                }
            }
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.hold_to_preview).uppercase(),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.tngColors.onPrimary
        )
    }
}

@Composable
private fun GraphStatInputViewForm(
    modifier: Modifier = Modifier,
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModel: GraphStatInputViewModel,
    graphStatId: Long,
    scrollState: ScrollState = rememberScrollState()
) = Column(
    modifier = modifier
        .padding(
            top = dimensionResource(id = R.dimen.card_padding),
            start = dimensionResource(id = R.dimen.card_padding),
            end = dimensionResource(id = R.dimen.card_padding)
        )
        .verticalScroll(state = scrollState)
) {

    FullWidthTextField(
        textFieldValue = viewModel.graphName,
        onValueChange = { viewModel.setGraphStatName(it) },
        label = stringResource(id = R.string.graph_or_stat_name)
    )

    SpacingSmall()

    val updateMode = viewModel.updateMode.observeAsState(false)
    if (!updateMode.value) {
        val selectedGraphType by viewModel.graphStatType.observeAsState(GraphStatType.LINE_GRAPH)
        GraphStatTypeSelector(
            selectedItem = selectedGraphType,
            setGraphType = { viewModel.setGraphType(it) }
        )
    }

    SpacingSmall()
    Divider()
    SpacingSmall()

    ConfigInputView(
        viewModelStoreOwner = viewModelStoreOwner,
        viewModel = viewModel,
        graphStatId = graphStatId,
        scrollState = scrollState
    )
}

@Composable
fun GraphStatTypeSelector(
    selectedItem: GraphStatType,
    setGraphType: (GraphStatType) -> Unit
) {
    LabeledRow(
        label = stringResource(R.string.graph_type_label),
        paddingValues = PaddingValues(
            start = dimensionResource(id = R.dimen.card_padding)
        )
    ) {

        val spinnerItems = mapOf(
            GraphStatType.LINE_GRAPH to stringResource(id = R.string.graph_type_line_graph),
            GraphStatType.PIE_CHART to stringResource(id = R.string.graph_type_pie_chart),
            GraphStatType.AVERAGE_TIME_BETWEEN to stringResource(id = R.string.graph_type_average_time_between),
            GraphStatType.TIME_HISTOGRAM to stringResource(id = R.string.graph_type_time_histogram),
            GraphStatType.LAST_VALUE to stringResource(id = R.string.graph_type_last_value)
        )

        TextMapSpinner(
            strings = spinnerItems,
            selectedItem = selectedItem,
            onItemSelected = { setGraphType(it) },
        )
    }
}

@Composable
fun ConfigInputView(
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModel: GraphStatInputViewModel,
    graphStatId: Long,
    scrollState: ScrollState
) {
    val graphType by viewModel.graphStatType.observeAsState(GraphStatType.LINE_GRAPH)

    val lineGraphConfigViewModel = hiltViewModel<LineGraphConfigViewModel>(viewModelStoreOwner)
    lineGraphConfigViewModel.initFromGraphStatId(graphStatId)

    val pieChartConfigViewModel = hiltViewModel<PieChartConfigViewModel>(viewModelStoreOwner)
    pieChartConfigViewModel.initFromGraphStatId(graphStatId)

    val averageTimeBetweenConfigViewModel =
        hiltViewModel<AverageTimeBetweenConfigViewModel>(viewModelStoreOwner)
    averageTimeBetweenConfigViewModel.initFromGraphStatId(graphStatId)

    val timeHistogramConfigViewModel =
        hiltViewModel<TimeHistogramConfigViewModel>(viewModelStoreOwner)
    timeHistogramConfigViewModel.initFromGraphStatId(graphStatId)

    val lastValueConfigViewModel =
        hiltViewModel<LastValueConfigViewModel>(viewModelStoreOwner)
    lastValueConfigViewModel.initFromGraphStatId(graphStatId)

    val barChartConfigViewModel =
        hiltViewModel<BarChartConfigViewModel>(viewModelStoreOwner)
    barChartConfigViewModel.initFromGraphStatId(graphStatId)

    var currentViewModel: GraphStatConfigViewModelBase<*> = lineGraphConfigViewModel

    when (graphType) {
        GraphStatType.LINE_GRAPH -> {
            currentViewModel = lineGraphConfigViewModel
            LineGraphConfigView(scrollState = scrollState, viewModel = lineGraphConfigViewModel)
        }
        GraphStatType.PIE_CHART -> {
            currentViewModel = pieChartConfigViewModel
            PieChartConfigView(viewModel = pieChartConfigViewModel)
        }
        GraphStatType.AVERAGE_TIME_BETWEEN -> {
            currentViewModel = averageTimeBetweenConfigViewModel
            AverageTimeBetweenConfigView(viewModel = averageTimeBetweenConfigViewModel)
        }
        GraphStatType.TIME_HISTOGRAM -> {
            currentViewModel = timeHistogramConfigViewModel
            TimeHistogramConfigView(viewModel = timeHistogramConfigViewModel)
        }
        GraphStatType.LAST_VALUE -> {
            currentViewModel = lastValueConfigViewModel
            LastValueConfigView(viewModel = lastValueConfigViewModel)
        }
        GraphStatType.BAR_CHART -> {
            currentViewModel = barChartConfigViewModel
            BarChartConfigView(viewModel = barChartConfigViewModel)
        }
    }

    LaunchedEffect(currentViewModel) {
        currentViewModel.getConfigFlow()
            .collect { viewModel.onConfigEvent(it) }
    }
}