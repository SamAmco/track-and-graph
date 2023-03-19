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

import androidx.annotation.ColorRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.android.material.card.MaterialCardView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphFeature
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatYRangeTypeSpinner
import com.samco.trackandgraph.ui.ColorSpinnerAdapter
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.*
import com.samco.trackandgraph.ui.dataVisColorList

@Composable
fun LineGraphConfigView(
    modifier: Modifier = Modifier,
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
        modifier = Modifier,
        yRangeType = viewModel.yRangeType,
        onYRangeTypeSelected = { viewModel.updateYRangeType(it) }
    )

    SpacingSmall()

    Divider()

    SpacingSmall()

    LineGraphFeaturesInputView(viewModel)
}

@Composable
private fun LineGraphFeaturesInputView(
    viewModel: LineGraphConfigViewModel
) = Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    for (index in viewModel.lineGraphFeatures.indices) {
        val lgf = viewModel.lineGraphFeatures[index]
        LineGraphFeatureInputView(
            lgf = lgf,
            //TODO implement feature path map
            emptyMap(),
            onRemove = { viewModel.removeLineGraphFeature(index) },
            onUpdate = { viewModel.updateLineGraphFeature(index, it) }
        )
    }

    AddBarButton(
        onClick = { viewModel.onAddLineGraphFeatureClicked() },
    )
}

@Composable
private fun LineGraphFeatureInputView(
    lgf: LineGraphFeature,
    featureMap: Map<Long, String>,
    onRemove: () -> Unit,
    onUpdate: (LineGraphFeature) -> Unit
) = Card {
    var text by remember { mutableStateOf(TextFieldValue(lgf.name)) }
    Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            //The name input
            FullWidthTextField(
                modifier = Modifier.weight(1f),
                textFieldValue = text,
                onValueChange = { text = it }
            )
            //bin button
            IconButton(onClick = { onRemove() }) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_line_button_content_description)
                )
            }
        }
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                //The color input
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
                //The feature
                TextMapSpinner(
                    strings = featureMap,
                    selectedItem = featureMap.keys.firstOrNull() ?: -1L,
                    onItemSelected = { onUpdate(lgf.copy(featureId = it)) }
                )
                //The averaging mode

                val averagingModeNames =
                    stringArrayResource(id = R.array.line_graph_averaging_mode_names)
                        .mapIndexed { index, name -> index to name }
                        .associate { (index, name) -> LineGraphAveraginModes.values()[index] to name }

                TextMapSpinner(
                    strings = averagingModeNames,
                    selectedItem = lgf.averagingMode,
                    onItemSelected = { onUpdate(lgf.copy(averagingMode = it)) }
                )

                //the plot mode
                val plotModeNames = stringArrayResource(id = R.array.line_graph_plot_mode_names)
                    .mapIndexed { index, name -> index to name }
                    .associate { (index, name) -> LineGraphPlottingModes.values()[index] to name }

                TextMapSpinner(
                    strings = plotModeNames,
                    selectedItem = lgf.plottingMode,
                    onItemSelected = { onUpdate(lgf.copy(plottingMode = it)) }
                )
                Row {
                    //Offset label
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.offset),
                        style = MaterialTheme.typography.body1
                    )

                    //Offset input
                    //TODO get this right
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = lgf.offset.toString(),
                        onValueChange = { onUpdate(lgf.copy(offset = it.toDouble())) },
                    )

                    //Scale label
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.scale),
                        style = MaterialTheme.typography.body1
                    )

                    //Scale input
                    //TODO get this right
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = lgf.scale.toString(),
                        onValueChange = { onUpdate(lgf.copy(scale = it.toDouble())) },
                    )
                }
            }
        }
    }
}