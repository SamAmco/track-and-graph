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
@file:OptIn(ExperimentalPagerApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.samco.trackandgraph.adddatapoint

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

@Composable
fun AddDataPointsDialog(viewModel: AddDataPointsViewModel, onDismissRequest: () -> Unit = {}) {

    val hidden by viewModel.hidden.observeAsState(true)

    LaunchedEffect(Unit) { viewModel.dismissEvents.collect { onDismissRequest() } }

    if (!hidden) {
        DialogTheme {
            Dialog(
                onDismissRequest = { onDismissRequest() },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = false
                )
            ) {
                AddDataPointsView(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    viewModel = viewModel
                )
                BackHandler {
                    if (viewModel.showCancelConfirmDialog.value == true) {
                        viewModel.onConfirmCancelDismissed()
                    } else viewModel.onCancelClicked()
                }
            }
        }
    }
}

@Composable
private fun AddDataPointsView(
    modifier: Modifier = Modifier,
    viewModel: AddDataPointsViewModel
) = Surface {
    Column(
        modifier = modifier
            .heightIn(max = 400.dp)
            .background(color = MaterialTheme.tngColors.surface)
            .padding(dimensionResource(id = R.dimen.card_padding))
    ) {
        val showTutorial by viewModel.showTutorial.observeAsState(false)
        if (showTutorial) AddDataPointsTutorial(viewModel.tutorialViewModel)
        else DataPointInputView(viewModel)
    }

    if (viewModel.showCancelConfirmDialog.observeAsState(false).value) {
        ConfirmCancelDialog(
            body = R.string.confirm_cancel_notes_will_be_lost,
            onDismissRequest = viewModel::onConfirmCancelDismissed,
            onConfirm = viewModel::onConfirmCancelConfirmed,
        )
    }
}

@Composable
private fun ColumnScope.DataPointInputView(viewModel: AddDataPointsViewModel) {
    HintHeader(viewModel)

    TrackerPager(Modifier.weight(1f, true), viewModel)

    BottomButtons(viewModel)
}

@Composable
private fun BottomButtons(viewModel: AddDataPointsViewModel) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallTextButton(
            stringRes = R.string.cancel,
            onClick = viewModel::onCancelClicked,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.tngColors.onSurface
            )
        )
        if (viewModel.skipButtonVisible.observeAsState(false).value) {
            SmallTextButton(
                stringRes = R.string.skip,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onSkipClicked()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
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
    val focusManager = LocalFocusManager.current

    HorizontalPager(
        modifier = modifier,
        count = count,
        state = pagerState
    ) { page ->
        viewModel.getViewModel(page).observeAsState().value?.let {
            TrackerPage(
                viewModel = it,
                currentPage = page == pagerState.currentPage
            )
        }
    }

    //Synchronise page between view model and view:

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect {
            focusManager.clearFocus()
            viewModel.updateCurrentPage(it)
        }
    }

    val currentPage by viewModel.currentPageIndex.observeAsState(0)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentPage) {
        if (currentPage != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(currentPage)
            }
        }
    }
}

@Composable
private fun TrackerPage(
    viewModel: AddDataPointViewModel,
    currentPage: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val focusManager = LocalFocusManager.current
        val valueFocusRequester = remember { FocusRequester() }

        TrackerNameHeadline(name = viewModel.name.observeAsState("").value)

        DialogInputSpacing()

        val selectedDateTime by viewModel.timestamp.observeAsState(OffsetDateTime.now())

        DateTimeButtonRow(
            modifier = Modifier.fillMaxWidth(),
            selectedDateTime = selectedDateTime,
            onDateTimeSelected = viewModel::updateTimestamp
        )

        DialogInputSpacing()

        val suggestedValues by viewModel.suggestedValues.observeAsState(emptyList())
        val selectedSuggestedValue by viewModel.currentValueAsSuggestion.observeAsState()

        if (suggestedValues.isNotEmpty()) {
            SuggestedValues(
                suggestedValues,
                selectedSuggestedValue,
                viewModel::onSuggestedValueSelected,
                viewModel::onSuggestedValueLongPress
            )
            DialogInputSpacing()
        }

        LaunchedEffect(valueFocusRequester) {
            viewModel.focusOnValueEvent.collect {
                delay(100)
                valueFocusRequester.requestFocus()
            }
        }

        var labelAdded by rememberSaveable { mutableStateOf(false) }
        var noteAdded by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(viewModel) {
            snapshotFlow { viewModel.label.text }
                .dropWhile { it.isEmpty() }
                .take(1)
                .collect { labelAdded = true }
        }

        LaunchedEffect(viewModel) {
            snapshotFlow { viewModel.note.text }
                .dropWhile { it.isEmpty() }
                .take(1)
                .collect { noteAdded = true }
        }

        when (viewModel) {
            is AddDataPointViewModel.NumericalDataPointViewModel -> {
                ValueInputTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.input_spacing_large)),
                    textFieldValue = viewModel.value,
                    onValueChange = viewModel::setValueText,
                    focusManager = focusManager,
                    focusRequester = valueFocusRequester,
                    onNextOverride = {
                        if (labelAdded || noteAdded) focusManager.moveFocus(FocusDirection.Down)
                        else viewModel.addDataPoint()
                    }
                )
            }

            is AddDataPointViewModel.DurationDataPointViewModel -> {
                DurationInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.input_spacing_large)),
                    viewModel = viewModel,
                    focusManager = focusManager,
                    nextFocusDirection = FocusDirection.Down,
                    focusRequester = valueFocusRequester
                )
            }

            else -> {}
        }

        LaunchedEffect(currentPage) {
            delay(100)
            if (currentPage && suggestedValues.all { it.value == null })
                valueFocusRequester.requestFocus()
        }

        LabelAndNoteInputs(
            viewModel = viewModel,
            scrollState = scrollState,
            labelAdded = labelAdded,
            noteAdded = noteAdded,
            onLabelAdded = { labelAdded = true },
            onNoteAdded = { noteAdded = true }
        )
    }
}

@Composable
private fun LabelAndNoteInputs(
    viewModel: AddDataPointViewModel,
    scrollState: ScrollState,
    labelAdded: Boolean,
    noteAdded: Boolean,
    onLabelAdded: () -> Unit,
    onNoteAdded: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var noteBoxHeight by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(noteBoxHeight) {
        if (noteBoxHeight != null) scrollState.scrollTo(scrollState.maxValue)
    }

    val coroutineScope = rememberCoroutineScope()
    val noteInputFocusRequester = remember { FocusRequester() }
    val labelInputFocusRequester = remember { FocusRequester() }

    DialogInputSpacing()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    if (labelAdded) {
        LabelInputTextField(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(horizontal = dimensionResource(id = R.dimen.input_spacing_large))
                .onFocusEvent { state ->
                    if (state.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
            textFieldValue = viewModel.label,
            onValueChange = viewModel::updateLabel,
            focusManager = focusManager,
            focusRequester = labelInputFocusRequester,
            onNextOverride = {
                if (noteAdded) focusManager.moveFocus(FocusDirection.Down)
                else viewModel.addDataPoint()
            }
        )
    }

    if (noteAdded) {
        if (labelAdded) DialogInputSpacing()
        FullWidthTextField(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .padding(horizontal = dimensionResource(id = R.dimen.input_spacing_large))
                .onGloballyPositioned {
                    if (noteBoxHeight != it.size.height) noteBoxHeight = it.size.height
                },
            textFieldValue = viewModel.note,
            onValueChange = { viewModel.updateNote(it) },
            focusRequester = noteInputFocusRequester,
            label = stringResource(id = R.string.note_input_hint),
            singleLine = false
        )
    }

    if (labelAdded xor noteAdded) DialogInputSpacing()

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.input_spacing_large)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (!labelAdded) {
            AddChipButton(text = stringResource(id = R.string.add_a_label)) {
                onLabelAdded()
                coroutineScope.launch {
                    delay(50)
                    labelInputFocusRequester.requestFocus()
                }
            }
        }
        if (!noteAdded) {
            AddChipButton(text = stringResource(id = R.string.add_a_note)) {
                onNoteAdded()
                coroutineScope.launch {
                    delay(50)
                    noteInputFocusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
private fun HintHeader(viewModel: AddDataPointsViewModel) =
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = viewModel.indexText.observeAsState("").value,
            fontSize = MaterialTheme.typography.body1.fontSize,
            fontWeight = MaterialTheme.typography.body1.fontWeight,
        )
        //Faq vecotor icon as a button
        IconButton(onClick = { viewModel.onTutorialButtonPressed() }) {
            Icon(
                painter = painterResource(id = R.drawable.faq_icon),
                contentDescription = stringResource(id = R.string.help),
                tint = MaterialTheme.colors.onSurface
            )
        }
    }

@Composable
private fun SuggestedValues(
    list: List<SuggestedValueViewData>,
    selectedItem: SuggestedValueViewData?,
    onSuggestedValueSelected: (SuggestedValueViewData) -> Unit,
    onSuggestedValueLongPress: (SuggestedValueViewData) -> Unit
) {
    val focusManager = LocalFocusManager.current
    FadingLazyRow(
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.dialog_input_spacing),
            Alignment.CenterHorizontally
        )
    ) {
        items(count = list.size, itemContent = { index ->
            val suggestedValue = list[index]

            //We should not be passed values with null for everything
            val text: String =
                if (suggestedValue.label.isNullOrBlank() && suggestedValue.valueStr != null) suggestedValue.valueStr
                else if (suggestedValue.valueStr.isNullOrBlank() && suggestedValue.label != null) suggestedValue.label
                else "${suggestedValue.valueStr} : ${suggestedValue.label}"

            TextChip(
                text = text,
                isSelected = suggestedValue == selectedItem,
                onClick = {
                    focusManager.clearFocus()
                    onSuggestedValueSelected(suggestedValue)
                },
                onLongPress = {
                    onSuggestedValueLongPress(suggestedValue)
                }
            )
        })
    }
}
