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
@file:OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)

package com.samco.trackandgraph.adddatapoint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

@Composable
fun AddDataPointsView(viewModel: AddDataPointsViewModel) = Surface {
    Column(
        modifier = Modifier
            .heightIn(max = 400.dp)
            .fillMaxWidth()
            .background(color = MaterialTheme.tngColors.surface)
            .padding(
                start = dimensionResource(id = R.dimen.card_padding),
                end = dimensionResource(id = R.dimen.card_padding),
                top = dimensionResource(id = R.dimen.input_spacing_large),
                bottom = dimensionResource(id = R.dimen.card_padding)
            )
    ) {
        HintHeader(viewModel)

        SpacingLarge()

        TrackerPager(Modifier.weight(1f, true), viewModel)

        BottomButtons(viewModel)
    }
}

@Composable
private fun BottomButtons(viewModel: AddDataPointsViewModel) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallTextButton(stringRes = R.string.cancel, onClick = viewModel::onCancelClicked)
        if (viewModel.skipButtonVisible.observeAsState(false).value) {
            SmallTextButton(stringRes = R.string.skip, onClick = {
                focusManager.clearFocus()
                viewModel.onSkipClicked()
            })
        }
        val addButtonRes =
            if (viewModel.updateMode.observeAsState(false).value) R.string.update
            else R.string.add
        SmallTextButton(stringRes = addButtonRes, onClick = {
            focusManager.clearFocus()
            viewModel.onAddClicked()
        })

    }
}

@Composable
private fun TrackerPager(modifier: Modifier, viewModel: AddDataPointsViewModel) {
    val count by viewModel.dataPointPages.observeAsState(0)
    val pagerState = rememberPagerState(initialPage = viewModel.currentPageIndex.value ?: 0)

    HorizontalPager(
        modifier = modifier,
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
        }
    }

    val currentPage by viewModel.currentPageIndex.observeAsState(0)
    val scope = rememberCoroutineScope()

    if (currentPage != pagerState.currentPage) {
        LaunchedEffect(currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(currentPage)
            }
        }
    }
}

@Composable
private fun TrackerPage(viewModel: AddDataPointViewModel) =
    FadingScrollColumn(
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val focusManager = LocalFocusManager.current

        SpacingLarge()

        TrackerNameHeadline(name = viewModel.name.observeAsState("").value)

        SpacingSmall()

        val selectedDateTime by viewModel.timestamp.observeAsState(OffsetDateTime.now())

        DateTimeButtonRow(
            modifier = Modifier.fillMaxWidth(),
            selectedDateTime = selectedDateTime,
            onDateTimeSelected = viewModel::updateTimestamp
        )

        SpacingLarge()

        SuggestedValues(viewModel)

        SpacingLarge()

        when (viewModel) {
            is AddDataPointViewModel.NumericalDataPointViewModel -> {

                val value by viewModel.value.observeAsState("")

                LabeledRow(label = stringResource(id = R.string.value_colon)) {
                    ValueInputTextField(
                        value = value ?: "",
                        onValueChanged = viewModel::setValue,
                        focusManager = focusManager
                    )
                }
            }
            is AddDataPointViewModel.DurationDataPointViewModel -> {
                LabeledRow(label = stringResource(id = R.string.value_colon)) {
                    DurationInput(
                        viewModel = viewModel,
                        focusManager = focusManager,
                        nextFocusDirection = FocusDirection.Down
                    )
                }
            }
        }

        SpacingSmall()

        val label by viewModel.label.observeAsState("")

        LabeledRow(label = stringResource(id = R.string.label_colon)) {
            LabelInputTextField(
                value = label,
                onValueChanged = viewModel::updateLabel,
                focusManager = focusManager
            )
        }

        SpacingSmall()

        NoteInput(viewModel)
    }

@Composable
private fun SuggestedValues(viewModel: AddDataPointViewModel) {
    val focusManager = LocalFocusManager.current
    val list by viewModel.suggestedValues.observeAsState(emptyList())
    val selectedItem by viewModel.selectedSuggestedValue.observeAsState()
    FadingLazyRow(
        size = with(LocalDensity.current) { 24.dp.toPx() },
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.dialog_input_spacing))
    ) {
        items(count = list.size, itemContent = { index ->
            val suggestedValue = list[index]
            TextChip(
                text = "${suggestedValue.valueStr} : ${suggestedValue.label}",
                isSelected = suggestedValue == selectedItem,
                onSelectionChanged = {
                    focusManager.clearFocus()
                    viewModel.onSuggestedValueSelected(suggestedValue)
                }
            )
        })
    }
}

@Composable
private fun NoteInput(viewModel: AddDataPointViewModel) =
    Row(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.Center
    ) {
        val note by viewModel.note.observeAsState("")
        var showNoteBox by rememberSaveable { mutableStateOf(false) }

        if (note.isNotEmpty() || showNoteBox) {
            FullWidthTextField(
                modifier = Modifier.heightIn(max = 200.dp),
                value = note,
                onValueChange = { viewModel.updateNote(it) },
                label = stringResource(id = R.string.note_input_hint),
                singleLine = false
            )
        } else {
            TextButton(
                onClick = { showNoteBox = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = stringResource(id = R.string.add_a_note)
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.add_a_note),
                    fontSize = MaterialTheme.typography.body1.fontSize,
                    fontWeight = MaterialTheme.typography.body1.fontWeight,
                )
            }
        }
    }

@Composable
private fun HintHeader(viewModel: AddDataPointsViewModel) =
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.add_data_point_hint),
            fontSize = MaterialTheme.typography.body1.fontSize,
            fontWeight = MaterialTheme.typography.body1.fontWeight,
        )
        Text(
            text = viewModel.indexText.observeAsState("").value,
            fontSize = MaterialTheme.typography.body1.fontSize,
            fontWeight = MaterialTheme.typography.body1.fontWeight,
        )
    }

