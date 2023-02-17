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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.ui.compose.ui.*

@Composable
internal fun GraphStatInputView(viewModel: GraphStatInputViewModel) {
    Surface(color = MaterialTheme.colors.background) {
        Box {
            val loading = viewModel.loading.observeAsState(true)

            GraphStatInputViewForm(viewModel)

            if (loading.value) LoadingOverlay()

            //TODO add demo button and overlay
        }
    }
}

@Composable
private fun GraphStatInputViewForm(viewModel: GraphStatInputViewModel) = Column(
    modifier = Modifier
        .padding(dimensionResource(id = R.dimen.card_padding))
        .verticalScroll(state = rememberScrollState())
) {

    FullWidthTextField(
        textFieldValue = viewModel.graphName,
        onValueChange = { viewModel.setGraphName(it.text) },
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

    Divider()

}

@Composable
fun GraphStatTypeSelector(
    selectedItem: GraphStatType,
    setGraphType: (GraphStatType) -> Unit
) {
    LabeledRow(label = stringResource(R.string.graph_type_label)) {

        val spinnerItems = mapOf(
            GraphStatType.LINE_GRAPH to stringResource(id = R.string.graph_type_line_graph),
            GraphStatType.PIE_CHART to stringResource(id = R.string.graph_type_pie_chart),
            GraphStatType.AVERAGE_TIME_BETWEEN to stringResource(id = R.string.graph_type_average_time_between),
            GraphStatType.TIME_SINCE to stringResource(id = R.string.graph_type_time_since),
            GraphStatType.TIME_HISTOGRAM to stringResource(id = R.string.graph_type_time_histogram)
        )

        TextMapSpinner(
            strings = spinnerItems,
            selectedItem = selectedItem,
            onItemSelected = { setGraphType(it) },
        )
    }
}
