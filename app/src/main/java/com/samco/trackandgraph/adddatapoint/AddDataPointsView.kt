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
package com.samco.trackandgraph.adddatapoint

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.google.accompanist.pager.HorizontalPager
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge

@Composable
fun AddDataPointsView(viewModel: AddDataPointsViewModel) =
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.card_padding),
                end = dimensionResource(id = R.dimen.card_padding),
                top = dimensionResource(id = R.dimen.input_spacing_large),
                bottom = dimensionResource(id = R.dimen.card_padding)
            )
    ) {
        Column {
            HintHeader(viewModel)

            SpacingLarge()

            TrackerPager(viewModel)

            SpacingLarge()

            BottomButtons(viewModel)
        }
    }

@Composable
private fun BottomButtons(viewModel: AddDataPointsViewModel) =
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallTextButton(stringRes = R.string.cancel, onClick = viewModel::onCancelClicked)
        if (viewModel.skipButtonVisible.observeAsState(false).value) {
            SmallTextButton(stringRes = R.string.skip, onClick = viewModel::onSkipClicked)
        }
        val addButtonRes =
            if (viewModel.updateMode.observeAsState(false).value) R.string.update
            else R.string.add
        SmallTextButton(stringRes = addButtonRes, onClick = viewModel::onAddClicked)
    }

@Composable
private fun TrackerPager(viewModel: AddDataPointsViewModel) {
/*
    HorizontalPager(count = ) {
        
    }
*/
}

@Composable
private fun HintHeader(viewModel: AddDataPointsViewModel) =
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.add_data_point_hint),
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
        )
        Text(
            text = viewModel.indexText.observeAsState("").value,
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
        )
    }

