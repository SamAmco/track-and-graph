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

package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LineGraphConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatYRangeTypeSpinner
import com.samco.trackandgraph.ui.compose.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LineGraphConfigView(
    scrollState: ScrollState,
    viewModel: LineGraphConfigViewModel
) {
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

    if (viewModel.yRangeType == YRangeType.FIXED) {
        YRangeFromToInputs(viewModel)
    }

    SpacingSmall()

    Divider()

    SpacingSmall()

    LineGraphFeaturesInputView(scrollState, viewModel)

    SpacingSmall()
}

@Composable
private fun YRangeFromToInputs(viewModel: LineGraphConfigViewModel) = Row(
    modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Text(
        modifier = Modifier.alignByBaseline(),
        text = stringResource(id = R.string.from),
        style = MaterialTheme.typography.subtitle2
    )

    MiniNumericTextField(
        modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        textAlign = TextAlign.Center,
        textFieldValue = viewModel.yRangeFrom,
        onValueChange = { viewModel.updateYRangeFrom(it) }
    )

    Text(
        modifier = Modifier.alignByBaseline(),
        text = stringResource(id = R.string.to),
        style = MaterialTheme.typography.subtitle2
    )

    MiniNumericTextField(
        modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        textAlign = TextAlign.Center,
        textFieldValue = viewModel.yRangeTo,
        onValueChange = { viewModel.updateYRangeTo(it) }
    )
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
            featureMap = viewModel.featureNameMap,
            textFields = viewModel.getTextFieldsFor(index),
            onRemove = { viewModel.removeLineGraphFeature(index) },
            onUpdate = { viewModel.updateLineGraphFeature(index, it) }
        )
        SpacingSmall()
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
    featureMap: Map<Long, String>,
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
        SpacingExtraSmall()
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
                TextMapSpinner(
                    strings = featureMap,
                    selectedItem = lgf.featureId,
                    onItemSelected = { onUpdate(lgf.copy(featureId = it)) }
                )

                val averagingModeNames =
                    stringArrayResource(id = R.array.line_graph_averaging_mode_names)
                        .mapIndexed { index, name -> index to name }
                        .associate { (index, name) -> LineGraphAveraginModes.values()[index] to name }

                TextMapSpinner(
                    strings = averagingModeNames,
                    selectedItem = lgf.averagingMode,
                    onItemSelected = { onUpdate(lgf.copy(averagingMode = it)) }
                )

                val plotModeNames = stringArrayResource(id = R.array.line_graph_plot_mode_names)
                    .mapIndexed { index, name -> index to name }
                    .associate { (index, name) -> LineGraphPlottingModes.values()[index] to name }

                TextMapSpinner(
                    strings = plotModeNames,
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

                    SpacingSmall()

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