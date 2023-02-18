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
package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import org.threeten.bp.Duration

enum class GraphStatDurations(val duration: Duration?) {
    ALL_DATA(null),
    A_DAY(Duration.ofDays(1)),
    A_WEEK(Duration.ofDays(7)),
    A_MONTH(Duration.ofDays(30)),
    THREE_MONTHS(Duration.ofDays(90)),
    SIX_MONTHS(Duration.ofDays(180)),
    A_YEAR(Duration.ofDays(365)),
}

@Composable
fun LineGraphConfigView(
    modifier: Modifier = Modifier,
    viewModel: LineGraphConfigViewModel
) {
    LabeledRow(label = stringResource(id = R.string.sample_size)) {
        val spinnerItems = mapOf(
            GraphStatDurations.ALL_DATA to stringResource(id = R.string.graph_time_durations_all_data),
            GraphStatDurations.A_DAY to stringResource(id = R.string.graph_time_durations_a_day),
            GraphStatDurations.A_WEEK to stringResource(id = R.string.graph_time_durations_a_week),
            GraphStatDurations.A_MONTH to stringResource(id = R.string.graph_time_durations_a_month),
            GraphStatDurations.THREE_MONTHS to stringResource(id = R.string.graph_time_durations_three_months),
            GraphStatDurations.SIX_MONTHS to stringResource(id = R.string.graph_time_durations_six_months),
            GraphStatDurations.A_YEAR to stringResource(id = R.string.graph_time_durations_a_year)
        )

        TextMapSpinner(
            strings = spinnerItems,
            selectedItem = viewModel.selectedDuration,
            onItemSelected = { viewModel.setDuration(it) },
        )
    }
}
