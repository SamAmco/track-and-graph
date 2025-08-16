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
@file:OptIn(ExperimentalFoundationApi::class)

package com.samco.trackandgraph.adddatapoint

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.*
import kotlinx.coroutines.flow.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddDataPointsDialog(viewModel: AddDataPointsViewModel, onDismissRequest: () -> Unit = {}) {

    val hidden by viewModel.hidden.observeAsState(true)

    LaunchedEffect(Unit) { viewModel.dismissEvents.collect { onDismissRequest() } }

    if (!hidden) {
        DialogTheme {
            CustomDialog(
                onDismissRequest = { onDismissRequest() },
                dismissOnClickOutside = false,
                scrollContent = false,
                paddingValues = PaddingValues(
                    vertical = halfDialogInputSpacing,
                    horizontal = inputSpacingLarge,
                )
            ) {
                AddDataPointsScreen(viewModel = viewModel)
                BackHandler {
                    if (viewModel.showCancelConfirmDialog.value == true) {
                        viewModel.onConfirmCancelDismissed()
                    } else viewModel.onCancelClicked()
                }
            }
        }
    }
}

// Data classes for state representation
private data class AddDataPointsViewState(
    val showTutorial: Boolean = false,
    val showCancelConfirmDialog: Boolean = false
)

private data class DataPointInputViewState(
    val indexText: String = "",
    val skipButtonVisible: Boolean = false,
    val updateMode: Boolean = false,
    val dataPointPages: Int = 0,
    val currentPageIndex: Int = 0
)

// Callback interfaces
private interface AddDataPointsCallbacks {
    fun onTutorialButtonPressed()
    fun onCancelClicked()
    fun onConfirmCancelConfirmed()
    fun onConfirmCancelDismissed()
    fun onSkipClicked()
    fun onAddClicked()
    fun onPageChanged(page: Int)
}

@Composable
private fun AddDataPointsScreen(
    viewModel: AddDataPointsViewModel
) {
    val showTutorial by viewModel.showTutorial.observeAsState(false)
    val showCancelConfirmDialog by viewModel.showCancelConfirmDialog.observeAsState(false)
    val indexText by viewModel.indexText.observeAsState("")
    val skipButtonVisible by viewModel.skipButtonVisible.observeAsState(false)
    val updateMode by viewModel.updateMode.observeAsState(false)
    val trackerPages by viewModel.pageViewModels.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.observeAsState(0)

    val callbacks = remember(viewModel) {
        object : AddDataPointsCallbacks {
            override fun onTutorialButtonPressed() = viewModel.onTutorialButtonPressed()
            override fun onCancelClicked() = viewModel.onCancelClicked()
            override fun onConfirmCancelConfirmed() = viewModel.onConfirmCancelConfirmed()
            override fun onConfirmCancelDismissed() = viewModel.onConfirmCancelDismissed()
            override fun onSkipClicked() = viewModel.onSkipClicked()
            override fun onAddClicked() = viewModel.onAddClicked()
            override fun onPageChanged(page: Int) = viewModel.updateCurrentPage(page)
        }
    }

    val state = AddDataPointsViewState(
        showTutorial = showTutorial,
        showCancelConfirmDialog = showCancelConfirmDialog
    )

    val inputState = DataPointInputViewState(
        indexText = indexText,
        skipButtonVisible = skipButtonVisible,
        updateMode = updateMode,
        dataPointPages = trackerPages.size,
        currentPageIndex = currentPageIndex
    )

    if (state.showTutorial) {
        AddDataPointsTutorial(viewModel.tutorialViewModel)
    } else {
        DataPointInputView(
            state = inputState,
            trackerPages = trackerPages,
            callbacks = callbacks
        )
    }

    if (state.showCancelConfirmDialog) {
        ContinueCancelDialog(
            body = R.string.confirm_cancel_notes_will_be_lost,
            onDismissRequest = callbacks::onConfirmCancelDismissed,
            onConfirm = callbacks::onConfirmCancelConfirmed,
        )
    }
}

@Composable
private fun DataPointInputView(
    state: DataPointInputViewState,
    trackerPages: List<AddDataPointViewModel>,
    callbacks: AddDataPointsCallbacks
) = BoxWithConstraints {
    Column(modifier = Modifier.heightIn(max = maxHeight)) {
        HintHeader(
            indexText = state.indexText,
            onTutorialButtonPressed = callbacks::onTutorialButtonPressed
        )

        Box(modifier = Modifier.weight(1f, fill = false)) {
            FadingScrollColumn {
                TrackerPager(
                    currentPageIndex = state.currentPageIndex,
                    trackerPages = trackerPages,
                    onPageChanged = callbacks::onPageChanged
                )
            }
        }

        BottomButtons(
            skipButtonVisible = state.skipButtonVisible,
            updateMode = state.updateMode,
            onCancelClicked = callbacks::onCancelClicked,
            onSkipClicked = callbacks::onSkipClicked,
            onAddClicked = callbacks::onAddClicked
        )
    }
}

@Composable
private fun BottomButtons(
    skipButtonVisible: Boolean,
    updateMode: Boolean,
    onCancelClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    onAddClicked: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallTextButton(
            stringRes = R.string.cancel,
            onClick = onCancelClicked,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.tngColors.onSurface
            )
        )
        if (skipButtonVisible) {
            SmallTextButton(
                stringRes = R.string.skip,
                onClick = {
                    focusManager.clearFocus()
                    onSkipClicked()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
        }
        val addButtonRes = if (updateMode) R.string.update else R.string.add
        SmallTextButton(
            stringRes = addButtonRes,
            onClick = {
                focusManager.clearFocus()
                onAddClicked()
            }
        )
    }
}

/**
 * Blocks child (descendant) scrollables from handing off any leftover
 * drag/fling to ancestors. The parent itself remains scrollable when
 * the gesture starts on it directly.
 */
fun Modifier.blockDescendantHandoff(): Modifier = composed {
    val connection = remember {
        object : NestedScrollConnection {
            // Don’t pre-consume (so the parent can still scroll if the gesture starts on it)
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

            // Consume all leftover scroll from descendants so it won’t bubble up
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = available

            // Consume all leftover fling from descendants so momentum doesn’t transfer
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = available
        }
    }
    this.then(Modifier.nestedScroll(connection))
}

@Composable
private fun TrackerPager(
    modifier: Modifier = Modifier,
    currentPageIndex: Int,
    trackerPages: List<AddDataPointViewModel>,
    onPageChanged: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPageIndex)
    val focusManager = LocalFocusManager.current

    // 1) Snap layout info
    val snapInfo = remember(listState) {
        SnapLayoutInfoProvider(
            lazyListState = listState,
            snapPosition = SnapPosition.Center
        )
    }

    // 2) “Heavier” physics:
    //    - Higher friction in the decay spec -> the fling dies out sooner (less page skipping)
    //    - Stiffer, well-damped spring for the final snap -> firm settle
    val heavyFling = remember(snapInfo) {
        val heavyDecay = exponentialDecay<Float>(
            frictionMultiplier = 4.5f
        )
        val firmSnap = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )

        snapFlingBehavior(
            snapLayoutInfoProvider = snapInfo,
            decayAnimationSpec = heavyDecay,
            snapAnimationSpec = firmSnap
        )
    }

    val pageFocusRequester = remember { FocusRequester() }
    val pageAvailable = remember { mutableStateOf(false) }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(), // Smooth height transitions
        flingBehavior = heavyFling,
    ) {
        itemsIndexed(trackerPages, key = { idx, _ -> idx }) { index, viewModel ->
            val suggestedValues by viewModel.suggestedValues.observeAsState(null)
            println("samsam $index $suggestedValues")
            TrackerPage(
                modifier = Modifier
                    .blockDescendantHandoff()
                    .fillParentMaxWidth(),
                viewModel = viewModel,
                currentPage = index == currentPageIndex,
                suggestedValues = suggestedValues,
                valueFocusRequester = if (index == currentPageIndex) pageFocusRequester else null
            )
            // The page will ignore the focus request if the suggested
            // values are null because it's still waiting to know if it should use
            // quick track buttons with value and label or just labels etc
            LaunchedEffect(suggestedValues) {
                if (suggestedValues != null) {
                    pageAvailable.value = true
                }
            }
        }
    }

    //Don't bother requesting focus until the first page is fully available
    // or it might be ignored
    LaunchedEffect(listState.isScrollInProgress, pageAvailable.value) {
        if (pageAvailable.value && !listState.isScrollInProgress) {
            pageFocusRequester.requestFocus()
        }
    }

    // Bidirectional synchronization between ViewModel and LazyRow

    // 1) LazyRow scroll position -> ViewModel
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                focusManager.clearFocus()
                onPageChanged(page)
            }
    }

    // 2) ViewModel currentPageIndex -> LazyRow scroll position
    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(currentPageIndex)
        }
    }
}

@Composable
private fun HintHeader(
    indexText: String,
    onTutorialButtonPressed: () -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text(
        text = indexText,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
    )
    //Faq vector icon as a button
    IconButton(onClick = onTutorialButtonPressed) {
        Icon(
            painter = painterResource(id = R.drawable.faq_icon),
            contentDescription = stringResource(id = R.string.help),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Preview composables
@Preview
@Composable
fun AddDataPointsViewPreview() {
    TnGComposeTheme {
        val sampleInputState = DataPointInputViewState(
            indexText = "1 of 3",
            skipButtonVisible = true,
            updateMode = false,
            dataPointPages = 3,
            currentPageIndex = 0
        )

        val sampleCallbacks = object : AddDataPointsCallbacks {
            override fun onTutorialButtonPressed() {}
            override fun onCancelClicked() {}
            override fun onConfirmCancelConfirmed() {}
            override fun onConfirmCancelDismissed() {}
            override fun onSkipClicked() {}
            override fun onAddClicked() {}
            override fun onPageChanged(page: Int) {}
        }

        // Note: This preview is simplified since we can't easily mock the tutorial ViewModel
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 400.dp)
                    .background(color = MaterialTheme.tngColors.surface)
                    .padding(
                        vertical = halfDialogInputSpacing,
                        horizontal = inputSpacingLarge,
                    )
            ) {
                DataPointInputView(
                    state = sampleInputState,
                    trackerPages = emptyList(),
                    callbacks = sampleCallbacks
                )
            }
        }
    }
}

