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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.BarChartBarPeriod
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.BarChartConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatYRangeTypeSpinner
import com.samco.trackandgraph.graphstatinput.customviews.YRangeFromToInputs
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner

@Composable
fun BarChartConfigView(
    viewModel: BarChartConfigViewModel
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

    if (viewModel.yRangeType == YRangeType.FIXED) YRangeFromToInputs(viewModel)

    SpacingSmall()

    Divider()

    SpacingSmall()

    Text(
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.card_padding)),
        text = stringResource(id = R.string.select_a_feature),
        style = MaterialTheme.typography.subtitle2
    )

    val featureId = viewModel.featureId
    val featureMap = viewModel.featureMap

    if (featureId != null && featureMap != null) {
        TextMapSpinner(
            strings = featureMap,
            selectedItem = featureId,
            onItemSelected = { viewModel.updateFeatureId(it) }
        )
    }

    //SpacingSmall()

    val strings = stringArrayResource(id = R.array.time_histogram_windows)
    val barIntervalNames = remember {
        mapOf(
            BarChartBarPeriod.HOUR to strings[0],
            BarChartBarPeriod.DAY to strings[1],
            BarChartBarPeriod.WEEK to strings[2],
            BarChartBarPeriod.MONTH to strings[3],
            BarChartBarPeriod.THREE_MONTHS to strings[4],
            BarChartBarPeriod.SIX_MONTHS to strings[5],
            BarChartBarPeriod.YEAR to strings[6]
        )
    }

    LabeledRow(
        label = stringResource(id = R.string.bar_interval),
        paddingValues = PaddingValues(start = dimensionResource(id = R.dimen.card_padding))
    ) {
        TextMapSpinner(
            strings = barIntervalNames,
            selectedItem = viewModel.selectedBarPeriod,
            onItemSelected = viewModel::updateBarPeriod
        )
    }

    SpacingSmall()

    //TODO add spinner for bar period

    RowCheckbox(
        checked = viewModel.sumByCount,
        onCheckedChange = { viewModel.updateSumByCount(it) },
        text = stringResource(id = R.string.sum_by_count_checkbox_label)
    )

    SpacingSmall()
}
