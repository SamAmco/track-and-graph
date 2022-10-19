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
@file:OptIn(ExperimentalPagerApi::class)

package com.samco.trackandgraph.adddatapoint

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.compose.ui.TrackerNameHeadline
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val count by viewModel.dataPointPages.observeAsState(0)
    val pagerState = rememberPagerState(initialPage = viewModel.currentPageIndex.value ?: 0)

    HorizontalPager(
        count = count,
        state = pagerState
    ) { page ->
        viewModel.getViewModel(page).observeAsState().value?.let {
            TrackerPage(viewModel = it)
        }
    }

    //Synchronise page between view model and view:

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect {
            viewModel.updateCurrentPage(it)
            println("samsam: updated page to: $it")
        }
    }

    val currentPage by viewModel.currentPageIndex.observeAsState(0)

    if (currentPage != pagerState.currentPage) {
        println("samsam: $currentPage != ${pagerState.currentPage}")
        LaunchedEffect(currentPage) {
            pagerState.animateScrollToPage(currentPage)
            println("samsam: animatedScrollTo $currentPage")
        }
    }
}

@Composable
private fun TrackerPage(viewModel: AddDataPointViewModel) =
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SpacingLarge()

        TrackerNameHeadline(name = viewModel.name.observeAsState("").value)

        SpacingSmall()

        DateTimeButtonRow()


        SpacingLarge()
    }

@Composable
fun DateTimeButtonRow(
    modifier: Modifier = Modifier
) = Row(
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    DateButton(dateString = "18/10/22")
    //TimeButton()
}

@Composable
fun DateButton(
    modifier: Modifier = Modifier,
    dateString: String
) = Button(
    onClick = {},
    shape = MaterialTheme.shapes.small,
    contentPadding = PaddingValues(8.dp)
) {
    Text(
        text = dateString,
        fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
        fontSize = MaterialTheme.typography.labelMedium.fontSize,
    )
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

