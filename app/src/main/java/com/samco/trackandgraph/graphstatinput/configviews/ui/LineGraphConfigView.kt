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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LineGraphConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatYRangeTypeSpinner
import com.samco.trackandgraph.graphstatinput.customviews.YRangeFromToInputs
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.AddBarButton
import com.samco.trackandgraph.ui.compose.ui.CircleSpinner
import com.samco.trackandgraph.ui.compose.ui.ColorSpinner
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration

@Composable
fun LineGraphConfigView(
    scrollState: ScrollState,
    graphStatId: Long,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit
) {
    val viewModel = hiltViewModel<LineGraphConfigViewModel>().apply {
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

    HorizontalDivider()

    DialogInputSpacing()

    LineGraphFeaturesInputView(
        scrollState = scrollState,
        featureUiDataList = viewModel.lineGraphFeatureUiDataList,
        onUpdateFeatureName = viewModel::onUpdateFeatureName,
        onUpdateFeatureOffset = viewModel::onUpdateFeatureOffset,
        onUpdateFeatureScale = viewModel::onUpdateFeatureScale,
        onUpdateFeatureColor = viewModel::onUpdateFeatureColor,
        onUpdateFeaturePointStyle = viewModel::onUpdateFeaturePointStyle,
        onUpdateFeatureAveragingMode = viewModel::onUpdateFeatureAveragingMode,
        onUpdateFeaturePlottingMode = viewModel::onUpdateFeaturePlottingMode,
        onUpdateFeatureDurationPlottingMode = viewModel::onUpdateFeatureDurationPlottingMode,
        onSelectFeature = viewModel::onSelectFeature,
        onRemoveFeature = viewModel::removeLineGraphFeature,
        onAddFeature = viewModel::onAddLineGraphFeatureClicked
    )

    DialogInputSpacing()
}

@Composable
private fun LineGraphFeaturesInputView(
    scrollState: ScrollState,
    featureUiDataList: List<LineGraphConfigViewModel.LineGraphFeatureUiData>,
    onUpdateFeatureName: (Int, TextFieldValue) -> Unit,
    onUpdateFeatureOffset: (Int, TextFieldValue) -> Unit,
    onUpdateFeatureScale: (Int, TextFieldValue) -> Unit,
    onUpdateFeatureColor: (Int, Int) -> Unit,
    onUpdateFeaturePointStyle: (Int, LineGraphPointStyle) -> Unit,
    onUpdateFeatureAveragingMode: (Int, LineGraphAveraginModes) -> Unit,
    onUpdateFeaturePlottingMode: (Int, LineGraphPlottingModes) -> Unit,
    onUpdateFeatureDurationPlottingMode: (Int, DurationPlottingMode) -> Unit,
    onSelectFeature: (Int, Long) -> Unit,
    onRemoveFeature: (Int) -> Unit,
    onAddFeature: () -> Unit
) = Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    featureUiDataList.forEachIndexed { index, featureUiData ->
        LineGraphFeatureInputView(
            featureUiData = featureUiData,
            onUpdateName = { onUpdateFeatureName(index, it) },
            onUpdateOffset = { onUpdateFeatureOffset(index, it) },
            onUpdateScale = { onUpdateFeatureScale(index, it) },
            onUpdateColor = { onUpdateFeatureColor(index, it) },
            onUpdatePointStyle = { onUpdateFeaturePointStyle(index, it) },
            onUpdateAveragingMode = { onUpdateFeatureAveragingMode(index, it) },
            onUpdatePlottingMode = { onUpdateFeaturePlottingMode(index, it) },
            onUpdateDurationPlottingMode = { onUpdateFeatureDurationPlottingMode(index, it) },
            onSelectFeature = { onSelectFeature(index, it) },
            onRemove = { onRemoveFeature(index) }
        )
        DialogInputSpacing()
    }

    val coroutineScope = rememberCoroutineScope()

    AddBarButton(
        onClick = {
            onAddFeature()
            coroutineScope.launch {
                delay(200)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        },
    )
}

@Composable
private fun LineGraphFeatureInputView(
    featureUiData: LineGraphConfigViewModel.LineGraphFeatureUiData,
    onUpdateName: (TextFieldValue) -> Unit,
    onUpdateOffset: (TextFieldValue) -> Unit,
    onUpdateScale: (TextFieldValue) -> Unit,
    onUpdateColor: (Int) -> Unit,
    onUpdatePointStyle: (LineGraphPointStyle) -> Unit,
    onUpdateAveragingMode: (LineGraphAveraginModes) -> Unit,
    onUpdatePlottingMode: (LineGraphPlottingModes) -> Unit,
    onUpdateDurationPlottingMode: (DurationPlottingMode) -> Unit,
    onSelectFeature: (Long) -> Unit,
    onRemove: () -> Unit
) = Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
) {
    Column(
        modifier = Modifier.padding(cardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FullWidthTextField(
                modifier = Modifier.weight(1f),
                textFieldValue = featureUiData.nameTextField,
                onValueChange = onUpdateName
            )
            IconButton(onClick = onRemove) {
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
                    selectedColor = featureUiData.colorIndex,
                    onColorSelected = onUpdateColor
                )

                val pointStyleIcons = listOf(
                    R.drawable.point_style_none_icon,
                    R.drawable.point_style_circles_icon,
                    R.drawable.point_style_circles_and_numbers_icon,
                    R.drawable.point_style_circles_only_icon,
                )

                CircleSpinner(
                    numItems = pointStyleIcons.size,
                    selectedIndex = LineGraphPointStyle.entries.indexOf(featureUiData.pointStyle),
                    onIndexSelected = { onUpdatePointStyle(LineGraphPointStyle.entries[it]) }
                ) {
                    Icon(
                        painter = painterResource(id = pointStyleIcons[it]),
                        contentDescription = null
                    )
                }
            }
            Column {
                // Feature Selection
                var showSelectDialog by rememberSaveable { mutableStateOf(false) }

                SelectorButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = featureUiData.selectedFeatureText,
                    onClick = { showSelectDialog = true }
                )

                if (showSelectDialog) {
                    SelectItemDialog(
                        title = stringResource(R.string.select_a_feature),
                        selectableTypes = setOf(SelectableItemType.FEATURE),
                        onFeatureSelected = { selectedFeatureId ->
                            onSelectFeature(selectedFeatureId)
                            showSelectDialog = false
                        },
                        onDismissRequest = { showSelectDialog = false }
                    )
                }

                // Duration Plotting Mode (only show for duration features)
                if (featureUiData.isDurationFeature) {
                    HalfDialogInputSpacing()
                    
                    val durationModeNames = mapOf(
                        DurationPlottingMode.DURATION_IF_POSSIBLE to stringResource(R.string.duration),
                        DurationPlottingMode.HOURS to stringResource(R.string.hours),
                        DurationPlottingMode.MINUTES to stringResource(R.string.minutes),
                        DurationPlottingMode.SECONDS to stringResource(R.string.seconds)
                    )

                    TextMapSpinner(
                        strings = durationModeNames,
                        selectedItem = featureUiData.durationPlottingMode,
                        onItemSelected = onUpdateDurationPlottingMode
                    )
                }

                // Averaging Mode
                val averagingModeNames =
                    stringArrayResource(id = R.array.line_graph_averaging_mode_names)
                        .mapIndexed { index, name -> index to name }
                        .associate { (index, name) -> LineGraphAveraginModes.entries.toTypedArray()[index] to name }

                TextMapSpinner(
                    strings = averagingModeNames,
                    selectedItem = featureUiData.averagingMode,
                    onItemSelected = onUpdateAveragingMode
                )

                // Plotting Mode
                val plotModeNames = stringArrayResource(id = R.array.line_graph_plot_mode_names)
                    .mapIndexed { index, name -> index to name }
                    .associate { (index, name) -> LineGraphPlottingModes.entries.toTypedArray()[index] to name }

                TextMapSpinner(
                    strings = plotModeNames,
                    selectedItem = featureUiData.plottingMode,
                    onItemSelected = onUpdatePlottingMode
                )

                InputSpacingLarge()

                // Offset and Scale
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResource(id = R.string.offset),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    MiniNumericTextField(
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                        textAlign = TextAlign.Center,
                        textFieldValue = featureUiData.offsetTextField,
                        onValueChange = onUpdateOffset
                    )

                    DialogInputSpacing()

                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResource(id = R.string.scale),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    MiniNumericTextField(
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                        textAlign = TextAlign.Center,
                        textFieldValue = featureUiData.scaleTextField,
                        onValueChange = onUpdateScale
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LineGraphConfigViewPreview() {
    val mockSettings = object : TngSettings {
        override val firstDayOfWeek = DayOfWeek.MONDAY
        override val startTimeOfDay = Duration.ofSeconds(0)
    }
    
    CompositionLocalProvider(LocalSettings provides mockSettings) {
        TnGComposeTheme {
            LineGraphFeaturesInputView(
                scrollState = rememberScrollState(),
                featureUiDataList = listOf(
                    LineGraphConfigViewModel.LineGraphFeatureUiData(
                        nameTextField = TextFieldValue("Steps"),
                        offsetTextField = TextFieldValue("0"),
                        scaleTextField = TextFieldValue("1"),
                        selectedFeatureText = "Daily Steps",
                        colorIndex = 0,
                        averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                        plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                        pointStyle = LineGraphPointStyle.CIRCLES,
                        isDurationFeature = false,
                        durationPlottingMode = DurationPlottingMode.DURATION_IF_POSSIBLE,
                        featureId = 1L
                    ),
                    LineGraphConfigViewModel.LineGraphFeatureUiData(
                        nameTextField = TextFieldValue("Sleep Duration"),
                        offsetTextField = TextFieldValue("0"),
                        scaleTextField = TextFieldValue("1"),
                        selectedFeatureText = "Sleep Time",
                        colorIndex = 1,
                        averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                        plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                        pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                        isDurationFeature = true,
                        durationPlottingMode = DurationPlottingMode.HOURS,
                        featureId = 2L
                    )
                ),
                onUpdateFeatureName = { _, _ -> },
                onUpdateFeatureOffset = { _, _ -> },
                onUpdateFeatureScale = { _, _ -> },
                onUpdateFeatureColor = { _, _ -> },
                onUpdateFeaturePointStyle = { _, _ -> },
                onUpdateFeatureAveragingMode = { _, _ -> },
                onUpdateFeaturePlottingMode = { _, _ -> },
                onUpdateFeatureDurationPlottingMode = { _, _ -> },
                onSelectFeature = { _, _ -> },
                onRemoveFeature = { _ -> },
                onAddFeature = { }
            )
        }
    }
}