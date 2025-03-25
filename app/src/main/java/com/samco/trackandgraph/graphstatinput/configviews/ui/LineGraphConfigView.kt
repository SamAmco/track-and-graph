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

package com.samco.trackandgraph.graphstatinput.configviews.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphFeature
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LineGraphConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatYRangeTypeSpinner
import com.samco.trackandgraph.graphstatinput.customviews.YRangeFromToInputs
import com.samco.trackandgraph.ui.compose.ui.AddBarButton
import com.samco.trackandgraph.ui.compose.ui.CircleSpinner
import com.samco.trackandgraph.ui.compose.ui.ColorSpinner
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LineGraphConfigView(
    scrollState: ScrollState,
    viewModelStoreOwner: ViewModelStoreOwner,
    graphStatId: Long,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit
) {
    val viewModel = hiltViewModel<LineGraphConfigViewModel>(viewModelStoreOwner).apply {
        initFromGraphStatId(graphStatId)
    }

    LaunchedEffect(viewModel) {
        viewModel.getConfigFlow().collect { onConfigEvent(it) }
    }

    GraphStatDurationSpinner(
        modifier = Modifier,
        selectedDuration = viewModel.selectedDuration,
        onDurationSelected = { viewModel.updateDuration(it) }
    )

    GraphStatEndingAtSpinner(
        modifier = Modifier,
        sampleEndingAt = viewModel.sampleEndingAt
    ) { viewModel.updateSampleEndingAt(it) }

    GraphStatYRangeTypeSpinner(
        yRangeType = viewModel.yRangeType,
        onYRangeTypeSelected = { viewModel.updateYRangeType(it) }
    )

    if (viewModel.yRangeType == YRangeType.FIXED) YRangeFromToInputs(viewModel)

    DialogInputSpacing()

    Divider()

    DialogInputSpacing()

    LineGraphFeaturesInputView(scrollState, viewModel)

    DialogInputSpacing()
}

@Composable
private fun LineGraphFeaturesInputView(
    scrollState: ScrollState,
    viewModel: LineGraphConfigViewModel
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .animateContentSize(),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    for (index in viewModel.lineGraphFeatures.indices) {
        val lgf = viewModel.lineGraphFeatures[index]
        LineGraphFeatureInputView(
            lgf = lgf,
            features = viewModel.featurePaths,
            textFields = viewModel.getTextFieldsFor(index),
            onRemove = { viewModel.removeLineGraphFeature(index) },
            onUpdate = { viewModel.updateLineGraphFeature(index, it) }
        )
        DialogInputSpacing()
    }

    val coroutineScope = rememberCoroutineScope()

    AddBarButton(
        onClick = {
            viewModel.onAddLineGraphFeatureClicked()
            coroutineScope.launch {
                delay(200)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        },
    )
}

@Composable
private fun LineGraphFeatureInputView(
    lgf: LineGraphFeature,
    features: List<LineGraphConfigViewModel.FeaturePathViewData>,
    textFields: LineGraphConfigViewModel.FeatureTextFields,
    onRemove: () -> Unit,
    onUpdate: (LineGraphFeature) -> Unit
) = Card {
    Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FullWidthTextField(
                modifier = Modifier.weight(1f),
                textFieldValue = textFields.name,
                onValueChange = { textFields.updateName(it) }
            )
            IconButton(onClick = { onRemove() }) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_line_button_content_description)
                )
            }
        }
        HalfDialogInputSpacing()
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorSpinner(
                    selectedColor = lgf.colorIndex,
                    onColorSelected = { onUpdate(lgf.copy(colorIndex = it)) }
                )

                val pointStyleIcons = listOf(
                    R.drawable.point_style_none_icon,
                    R.drawable.point_style_circles_icon,
                    R.drawable.point_style_circles_and_numbers_icon
                )

                CircleSpinner(
                    numItems = pointStyleIcons.size,
                    selectedIndex = LineGraphPointStyle.values().indexOf(lgf.pointStyle),
                    onIndexSelected = { onUpdate(lgf.copy(pointStyle = LineGraphPointStyle.values()[it])) }
                ) {
                    Icon(
                        painter = painterResource(id = pointStyleIcons[it]),
                        contentDescription = null
                    )
                }
            }
            Column {
                val hoursStr = stringResource(id = R.string.hours)
                val minutesStr = stringResource(id = R.string.minutes)
                val secondsStr = stringResource(id = R.string.seconds)

                val featureNames = remember(features) {
                    features.associateBy(
                        keySelector = { it.id },
                        valueTransform = {
                            when (it.id.durationPlottingMode) {
                                DurationPlottingMode.NONE, DurationPlottingMode.DURATION_IF_POSSIBLE -> it.path
                                DurationPlottingMode.HOURS -> "${it.path} ($hoursStr)"
                                DurationPlottingMode.MINUTES -> "${it.path} ($minutesStr)"
                                DurationPlottingMode.SECONDS -> "${it.path} ($secondsStr)"
                            }
                        }
                    )
                }

                val selectedItem = remember(lgf) {
                    LineGraphConfigViewModel.FeatureSelectionIdentifier(
                        featureId = lgf.featureId,
                        durationPlottingMode = lgf.durationPlottingMode
                    )
                }

                TextMapSpinner(
                    strings = featureNames,
                    selectedItem = selectedItem,
                    paddingValues = PaddingValues(start = dimensionResource(id = R.dimen.card_padding)),
                    onItemSelected = {
                        onUpdate(
                            lgf.copy(
                                featureId = it.featureId,
                                durationPlottingMode = it.durationPlottingMode
                            )
                        )
                    }
                )

                val averagingModeNames =
                    stringArrayResource(id = R.array.line_graph_averaging_mode_names)
                        .mapIndexed { index, name -> index to name }
                        .associate { (index, name) -> LineGraphAveraginModes.values()[index] to name }

                TextMapSpinner(
                    strings = averagingModeNames,
                    paddingValues = PaddingValues(start = dimensionResource(id = R.dimen.card_padding)),
                    selectedItem = lgf.averagingMode,
                    onItemSelected = { onUpdate(lgf.copy(averagingMode = it)) }
                )

                val plotModeNames = stringArrayResource(id = R.array.line_graph_plot_mode_names)
                    .mapIndexed { index, name -> index to name }
                    .associate { (index, name) -> LineGraphPlottingModes.values()[index] to name }

                TextMapSpinner(
                    strings = plotModeNames,
                    paddingValues = PaddingValues(start = dimensionResource(id = R.dimen.card_padding)),
                    selectedItem = lgf.plottingMode,
                    onItemSelected = { onUpdate(lgf.copy(plottingMode = it)) }
                )
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResource(id = R.string.offset),
                        style = MaterialTheme.typography.body1
                    )

                    MiniNumericTextField(
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                        textAlign = TextAlign.Center,
                        textFieldValue = textFields.offset,
                        onValueChange = { textFields.updateOffset(it) }
                    )

                    DialogInputSpacing()

                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResource(id = R.string.scale),
                        style = MaterialTheme.typography.body1
                    )

                    MiniNumericTextField(
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                        textAlign = TextAlign.Center,
                        textFieldValue = textFields.scale,
                        onValueChange = { textFields.updateScale(it) }
                    )
                }
            }
        }
    }
}