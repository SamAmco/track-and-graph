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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LastValueConfigViewModel
import com.samco.trackandgraph.graphstatinput.customviews.FilterByLabelSection
import com.samco.trackandgraph.graphstatinput.customviews.FilterByValueSection
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding

@Composable
fun LastValueConfigView(
    graphStatId: Long?,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit
) {
    val viewModel = hiltViewModel<LastValueConfigViewModel>().apply {
        init(graphStatId)
    }

    LaunchedEffect(viewModel) {
        viewModel.getConfigFlow().collect { onConfigEvent(it) }
    }

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

    InputSpacingLarge()

    FilterByLabelSection(viewModel)

    InputSpacingLarge()

    FilterByValueSection(viewModel)

    DialogInputSpacing()
}