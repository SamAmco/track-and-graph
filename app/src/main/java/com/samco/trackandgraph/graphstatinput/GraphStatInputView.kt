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

package com.samco.trackandgraph.graphstatinput

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.graphstatinput.configviews.ui.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
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
                var demoYOffset by remember { mutableFloatStateOf(0f) }
                var demoVisible by remember { mutableStateOf(false) }

                val selectedGraphType = viewModel.graphStatType.observeAsState(GraphStatType.LINE_GRAPH)

                Box(modifier = Modifier.weight(1f)) {
                    GraphStatInputViewForm(
                        graphName = viewModel.graphName,
                        selectedGraphType = selectedGraphType,
                        setGraphType = viewModel::setGraphType,
                        setGraphStatName = viewModel::setGraphStatName,
                        updateMode = viewModel.updateMode.observeAsState(false),
                        configInputView = { scrollState ->
                            ConfigInputView(
                                viewModelStoreOwner = viewModelStoreOwner,
                                onConfigEvent = viewModel::onConfigEvent,
                                graphType = selectedGraphType,
                                graphStatId = graphStatId,
                                scrollState = scrollState
                            )
                        },
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
                    isUpdateMode = viewModel.updateMode.observeAsState().value == true
                )
            }

            if (loading.value) LoadingOverlay()

            val showLuaFirstTimeUserDialog = viewModel.showLuaFirstTimeUserDialog.observeAsState(false)
            if (showLuaFirstTimeUserDialog.value) {
                LuaFirstTimeUserDialog(
                    onDismiss = viewModel::onLuaFirstTimeUserDialogDismiss,
                    onOpenLuaTutorialPath = viewModel::onOpenLuaTutorialPath
                )
            }
        }
    }
}

@Composable
private fun GraphStatInputViewForm(
    modifier: Modifier = Modifier,
    graphName: TextFieldValue,
    selectedGraphType: State<GraphStatType>,
    setGraphType: (GraphStatType) -> Unit,
    setGraphStatName: (TextFieldValue) -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    updateMode: State<Boolean>,
    configInputView: @Composable (ScrollState) -> Unit = {},
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
        textFieldValue = graphName,
        onValueChange = setGraphStatName,
        label = stringResource(id = R.string.graph_or_stat_name)
    )

    DialogInputSpacing()

    if (!updateMode.value) {
        GraphStatTypeSelector(
            selectedItem = selectedGraphType,
            setGraphType = setGraphType
        )
    }

    DialogInputSpacing()
    Divider()
    DialogInputSpacing()

    configInputView(scrollState)
}

@Preview
@Composable
private fun GraphStatInputViewFormPreview() = TnGComposeTheme {
    Surface {
        GraphStatInputViewForm(
            graphName = TextFieldValue("Graph Stat Name"),
            selectedGraphType = remember { mutableStateOf(GraphStatType.LINE_GRAPH) },
            setGraphType = {},
            updateMode = remember { mutableStateOf(false) },
            configInputView = {}
        )
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
private fun LuaFirstTimeUserDialog(
    onDismiss: () -> Unit,
    onOpenLuaTutorialPath: () -> Unit,
) {
    ConfirmDialog(
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        content = {
            Column {
                Text(
                    stringResource(id = R.string.lua_graph_first_time_user_title),
                    style = MaterialTheme.typography.h5,
                )
                InputSpacingLarge()
                Text(
                    stringResource(id = R.string.lua_graph_first_time_user_message),
                    style = MaterialTheme.typography.body1,
                )
                InputSpacingLarge()
                TextLink(
                    stringResource(id = R.string.learn_more),
                ) {
                    onOpenLuaTutorialPath()
                    onDismiss()
                }
            }
        },
        continueText = R.string.ok,
    )
}

@Preview(showBackground = true)
@Composable
private fun LuaFirstTimeUserDialogPreview() {
    LuaFirstTimeUserDialog(onDismiss = {}, onOpenLuaTutorialPath = {})
}

@Composable
fun GraphStatTypeSelector(
    selectedItem: State<GraphStatType>,
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
            GraphStatType.BAR_CHART to stringResource(id = R.string.graph_type_bar_chart),
            GraphStatType.PIE_CHART to stringResource(id = R.string.graph_type_pie_chart),
            GraphStatType.AVERAGE_TIME_BETWEEN to stringResource(id = R.string.graph_type_average_time_between),
            GraphStatType.TIME_HISTOGRAM to stringResource(id = R.string.graph_type_time_histogram),
            GraphStatType.LAST_VALUE to stringResource(id = R.string.graph_type_last_value),
            GraphStatType.LUA_SCRIPT to stringResource(id = R.string.graph_stat_type_lua_graph),
        )

        TextMapSpinner(
            strings = spinnerItems,
            selectedItem = selectedItem.value,
            textAlign = TextAlign.End,
            onItemSelected = { setGraphType(it) },
        )
    }
}

@Composable
fun ConfigInputView(
    viewModelStoreOwner: ViewModelStoreOwner,
    graphType: State<GraphStatType>,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit,
    graphStatId: Long,
    scrollState: ScrollState
) {
    when (graphType.value) {
        GraphStatType.LINE_GRAPH -> LineGraphConfigView(
            scrollState = scrollState,
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.PIE_CHART -> PieChartConfigView(
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.AVERAGE_TIME_BETWEEN -> AverageTimeBetweenConfigView(
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.TIME_HISTOGRAM -> TimeHistogramConfigView(
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.LAST_VALUE -> LastValueConfigView(
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.BAR_CHART -> BarChartConfigView(
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.LUA_SCRIPT -> LuaGraphConfigView(
            scrollState = scrollState,
            viewModelStoreOwner = viewModelStoreOwner,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )
    }
}