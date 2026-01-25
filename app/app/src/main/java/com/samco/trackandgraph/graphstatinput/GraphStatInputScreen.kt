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
package com.samco.trackandgraph.graphstatinput

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatinput.configviews.ui.AverageTimeBetweenConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.BarChartConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.LastValueConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.LineGraphConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.LuaGraphConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.PieChartConfigView
import com.samco.trackandgraph.graphstatinput.configviews.ui.TimeHistogramConfigView
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.AddCreateBar
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.TextLink
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import java.lang.Float.max
import java.lang.Float.min

@Serializable
data class GraphStatInputNavKey(
    val groupId: Long,
    val graphStatId: Long = -1L
) : NavKey

@Composable
fun GraphStatInputScreen(
    navArgs: GraphStatInputNavKey,
    urlNavigator: UrlNavigator,
    onPopBack: () -> Unit
) {
    val viewModel: GraphStatInputViewModel = hiltViewModel<GraphStatInputViewModelImpl>()

    // Initialize ViewModel with arguments
    LaunchedEffect(navArgs.groupId, navArgs.graphStatId) {
        viewModel.initViewModel(navArgs.groupId, navArgs.graphStatId)
    }

    // Handle navigation back when complete
    LaunchedEffect(Unit) {
        viewModel.complete.receiveAsFlow().collect {
            onPopBack()
        }
    }

    // App bar configuration
    TopAppBarContent(navArgs, urlNavigator)

    // Screen content
    GraphStatInputView(
        viewModel = viewModel,
        graphStatId = navArgs.graphStatId
    )
}

@Composable
private fun TopAppBarContent(navArgs: GraphStatInputNavKey, urlNavigator: UrlNavigator) {
    val topBarController = LocalTopBarController.current
    val context = LocalContext.current
    val title = stringResource(R.string.add_a_graph_or_stat)

    val actions: @Composable RowScope.() -> Unit = remember(urlNavigator, context) {
        {
            IconButton(
                onClick = {
                    urlNavigator.triggerNavigation(context, UrlNavigator.Location.TUTORIAL_GRAPHS)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.about_icon),
                    contentDescription = stringResource(R.string.info)
                )
            }
        }
    }

    topBarController.Set(
        navArgs,
        AppBarConfig(
            title = title,
            backNavigationAction = true,
            actions = actions
        )
    )
}

@Composable
internal fun GraphStatInputView(
    viewModel: GraphStatInputViewModel,
    graphStatId: Long
) = Box(
    modifier = Modifier.fillMaxSize()
) {
    val loading = viewModel.loading.observeAsState(true)
    val demoViewData by viewModel.demoViewData.observeAsState()

    Column(modifier = Modifier.fillMaxHeight()) {
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
        .verticalScroll(state = scrollState)
        .padding(
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal)
                .asPaddingValues()
        )
        .then(
            Modifier.padding(
                top = cardPadding,
                start = cardPadding,
                end = cardPadding
            )
        )
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
        .padding(WindowInsets.safeDrawing.asPaddingValues())
        .fillMaxSize()
) {
    var displayHeight by remember { mutableFloatStateOf(0f) }
    var demoHeight by remember { mutableFloatStateOf(0f) }
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
            .padding(vertical = cardPadding)
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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.tngColors.onPrimary
        )
    }
}

@Composable
private fun LuaFirstTimeUserDialog(
    onDismiss: () -> Unit,
    onOpenLuaTutorialPath: () -> Unit,
) {
    CustomContinueCancelDialog(
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        cancelVisible = false,
        content = {
            Column {
                Text(
                    stringResource(id = R.string.lua_graph_first_time_user_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                InputSpacingLarge()
                Text(
                    stringResource(id = R.string.lua_graph_first_time_user_message),
                    style = MaterialTheme.typography.bodyLarge,
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
            start = cardPadding
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
    graphType: State<GraphStatType>,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit,
    graphStatId: Long,
    scrollState: ScrollState
) {
    when (graphType.value) {
        GraphStatType.LINE_GRAPH -> LineGraphConfigView(
            scrollState = scrollState,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.PIE_CHART -> PieChartConfigView(
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.AVERAGE_TIME_BETWEEN -> AverageTimeBetweenConfigView(
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.TIME_HISTOGRAM -> TimeHistogramConfigView(
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.LAST_VALUE -> LastValueConfigView(
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.BAR_CHART -> BarChartConfigView(
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )

        GraphStatType.LUA_SCRIPT -> LuaGraphConfigView(
            scrollState = scrollState,
            graphStatId = graphStatId,
            onConfigEvent = onConfigEvent,
        )
    }
}
