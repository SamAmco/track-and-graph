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
package com.samco.trackandgraph.graphstatinput.configviews.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.TimeHistogramConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.cardPadding

@Composable
fun TimeHistogramConfigView(
    graphStatId: Long,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit
) = Column {
    val viewModel = hiltViewModel<TimeHistogramConfigViewModel>().apply {
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

    DialogInputSpacing()

    HorizontalDivider()

    InputSpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = cardPadding),
        text = stringResource(id = R.string.select_a_feature),
        style = MaterialTheme.typography.titleSmall
    )

    DialogInputSpacing()

    var showSelectDialog by rememberSaveable { mutableStateOf(false) }

    SelectorButton(
        modifier = Modifier.fillMaxWidth(),
        text = viewModel.selectedFeatureText,
        onClick = { showSelectDialog = true }
    )

    if (showSelectDialog) {
        SelectItemDialog(
            title = stringResource(R.string.select_a_feature),
            selectableTypes = setOf(SelectableItemType.FEATURE),
            onFeatureSelected = { selectedFeatureId ->
                viewModel.updateFeatureId(selectedFeatureId)
                showSelectDialog = false
            },
            onDismissRequest = { showSelectDialog = false }
        )
    }

    DialogInputSpacing()

    LabeledRow(
        label = stringResource(id = R.string.time_window_size),
        paddingValues = PaddingValues(start = cardPadding)
    ) {
        val stringArray = stringArrayResource(id = R.array.time_histogram_windows)
        val timeWindows = mapOf(
            TimeHistogramWindow.HOUR to stringArray[0],
            TimeHistogramWindow.DAY to stringArray[1],
            TimeHistogramWindow.WEEK to stringArray[2],
            TimeHistogramWindow.MONTH to stringArray[3],
            TimeHistogramWindow.THREE_MONTHS to stringArray[4],
            TimeHistogramWindow.SIX_MONTHS to stringArray[5],
            TimeHistogramWindow.YEAR to stringArray[6]
        )

        TextMapSpinner(
            strings = timeWindows,
            selectedItem = viewModel.selectedWindow,
            textAlign = TextAlign.End,
            onItemSelected = { viewModel.updateWindow(it) }
        )
    }

    DialogInputSpacing()

    RowCheckbox(
        checked = viewModel.sumByCount,
        onCheckedChange = { viewModel.updateSumByCount(it) },
        text = stringResource(id = R.string.sum_by_count_checkbox_label)
    )

    DialogInputSpacing()
}