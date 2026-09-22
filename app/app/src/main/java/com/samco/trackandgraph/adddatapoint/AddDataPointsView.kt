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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.theming.DialogTheme
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.ui.FadingScrollColumn
import com.samco.trackandgraph.ui.ui.SmallTextButton
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.ui.smallIconSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun AddDataPointsDialog(
    viewModel: AddDataPointsViewModel,
    onDismissRequest: () -> Unit = {},
    dialogWidth: Dp? = null,
    scrollInputContent: Boolean = true,
) {

    val hidden by viewModel.hidden.observeAsState(true)
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) { viewModel.dismissEvents.collect { onDismissRequest() } }
    LaunchedEffect(viewModel) {
        viewModel.dataPointAddedEvent.collect {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    if (!hidden) {
        // Basically everywhere we should use CustomDialog for consistency,
        // but here we need custom behaviour for layout height animation
        // performance. Otherwise the entire window animates in height and
        // we drop a lot of frames.
        AddDataPointsDialogContent(
            onDismissRequest = onDismissRequest,
            dialogWidth = dialogWidth,
            onBack = {
                if (viewModel.showCancelConfirmDialog.value == true) {
                    viewModel.onConfirmCancelDismissed()
                } else viewModel.onCancelClicked()
            }
        ) {
            AddDataPointsScreen(
                viewModel = viewModel,
                scrollInputContent = scrollInputContent,
            )
        }
    }
}

@Composable
internal fun AddDataPointsDialogContent(
    onDismissRequest: () -> Unit = {},
    dialogWidth: Dp? = null,
    onBack: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    DialogTheme {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = true,
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentHeight()
                        .let { if (dialogWidth != null) it.width(dialogWidth) else it }
                        .systemBarsPadding()
                        .imePadding(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(
                            paddingValues = PaddingValues(
                                vertical = halfDialogInputSpacing,
                                horizontal = inputSpacingLarge,
                            )
                        )
                    ) {
                        content()
                    }
                }
            }
            BackHandler(onBack = onBack)
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
    val currentPageIndex: Int = 0,
    val trackedCount: Int = 0,
    val anyFieldLocked: Boolean = false
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
    viewModel: AddDataPointsViewModel,
    scrollInputContent: Boolean = true,
) {
    val showTutorial by viewModel.showTutorial.observeAsState(false)
    val showCancelConfirmDialog by viewModel.showCancelConfirmDialog.observeAsState(false)
    val indexText by viewModel.indexText.observeAsState("")
    val skipButtonVisible by viewModel.skipButtonVisible.observeAsState(false)
    val updateMode by viewModel.updateMode.observeAsState(false)
    val trackerPages by viewModel.pageViewModels.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.observeAsState(0)
    val trackedCount by viewModel.trackedCount.collectAsStateWithLifecycle()

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

    val currentPageLockState = trackerPages.getOrNull(currentPageIndex)?.lockState
    val inputState = DataPointInputViewState(
        indexText = indexText,
        skipButtonVisible = skipButtonVisible,
        updateMode = updateMode,
        dataPointPages = trackerPages.size,
        currentPageIndex = currentPageIndex,
        trackedCount = trackedCount,
        anyFieldLocked = currentPageLockState?.anyLocked == true
    )

    if (state.showTutorial) {
        AddDataPointsTutorial(viewModel.tutorialViewModel)
    } else {
        DataPointInputView(
            state = inputState,
            trackerPages = trackerPages,
            callbacks = callbacks,
            scrollInputContent = scrollInputContent,
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
    callbacks: AddDataPointsCallbacks,
    scrollInputContent: Boolean = true,
) = BoxWithConstraints {
    Column(modifier = Modifier.heightIn(max = maxHeight)) {
        HintHeader(
            indexText = state.indexText,
            trackedCount = state.trackedCount,
            anyFieldLocked = state.anyFieldLocked,
            onTutorialButtonPressed = callbacks::onTutorialButtonPressed
        )

        Box(modifier = Modifier.weight(1f, fill = false)) {
            if (scrollInputContent) FadingScrollColumn {
                TrackerPager(
                    currentPageIndex = state.currentPageIndex,
                    trackerPages = trackerPages,
                    onPageChanged = callbacks::onPageChanged
                )
            } else {
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
                onClick = onSkipClicked,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
        }
        val addButtonRes = if (updateMode) R.string.update else R.string.add
        SmallTextButton(
            stringRes = addButtonRes,
            onClick = onAddClicked
        )
    }
}

/**
 * Blocks child (descendant) scrollables from handing off any leftover
 * drag/fling to ancestors. The parent itself remains scrollable when the
 * gesture starts on it directly.
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

/**
 * Makes every composed pager page report the target page's height. The child is
 * still measured at its natural height so adjacent pages can be cached without
 * allowing them to determine the pager's cross-axis size.
 */
private fun Modifier.pagerTargetHeight(
    isTargetPage: Boolean,
    targetHeight: Int?,
    onNaturalHeightChanged: (Int) -> Unit,
) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val reportedHeight = when {
        isTargetPage -> placeable.height
        targetHeight != null -> targetHeight
        else -> 0
    }

    layout(placeable.width, reportedHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
        placeable.placeRelative(0, 0)
    }
}.onSizeChanged { onNaturalHeightChanged(it.height) }

@Composable
private fun TrackerPager(
    modifier: Modifier = Modifier,
    currentPageIndex: Int,
    trackerPages: List<AddDataPointViewModel>,
    onPageChanged: (Int) -> Unit
) {
    if (trackerPages.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = currentPageIndex,
        pageCount = trackerPages::size,
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val firmSnap = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }
    val heavyFling = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        decayAnimationSpec = exponentialDecay(frictionMultiplier = 4.5f),
        snapAnimationSpec = firmSnap,
    )

    val pageFocusRequesters = remember(trackerPages) {
        List(trackerPages.size) {
            TrackerPageFocusRequesters(
                value = FocusRequester(),
                label = FocusRequester(),
                note = FocusRequester(),
            )
        }
    }
    var availableFocusTargets by remember {
        mutableStateOf(emptyMap<Int, Set<TrackerPageFocusTarget>>())
    }
    var activeField by remember { mutableStateOf(TrackerPageFocusTarget.Value) }
    val naturalPageHeights = remember(trackerPages) { mutableStateMapOf<Int, Int>() }

    val heightTargetPage = when {
        currentPageIndex != pagerState.settledPage -> currentPageIndex
        pagerState.isScrollInProgress -> pagerState.targetPage
        else -> pagerState.currentPage
    }
    val targetPageHeight = naturalPageHeights[heightTargetPage]

    // Suggested values are loaded lazily. Observe the current page and its
    // neighbours here so their keyboard policy is known before a swipe starts.
    val keyboardIntents = mutableMapOf<Int, KeyboardIntent>()
    val preloadRange = (currentPageIndex - 1..currentPageIndex + 1)
        .filter { it in trackerPages.indices }
    preloadRange.forEach { index ->
        key(trackerPages[index]) {
            val suggestedValuesState by trackerPages[index].suggestedValues.observeAsState(
                SuggestedValuesViewState()
            )
            keyboardIntents[index] = suggestedValuesState.keyboardIntent
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clipToBounds(),
        flingBehavior = heavyFling,
        beyondViewportPageCount = 1,
        key = { it },
    ) { index ->
        val viewModel = trackerPages[index]
        val suggestedValuesState by viewModel.suggestedValues.observeAsState(
            SuggestedValuesViewState()
        )
        TrackerPage(
            modifier = Modifier
                .blockDescendantHandoff()
                .fillMaxWidth()
                .pagerTargetHeight(
                    isTargetPage = index == heightTargetPage,
                    targetHeight = targetPageHeight,
                    onNaturalHeightChanged = { height ->
                        naturalPageHeights[index] = height
                    },
            ),
            viewModel = viewModel,
            suggestedValuesState = suggestedValuesState,
            focusRequesters = pageFocusRequesters[index],
            onFocusTargetChanged = { activeField = it },
            onAvailableFocusTargetsChanged = { targets ->
                availableFocusTargets = availableFocusTargets + (index to targets)
            },
        )
    }

    var lastAppliedKeyboardIntent by remember { mutableStateOf<KeyboardIntent?>(null) }
    var focusOwnerPage by remember { mutableIntStateOf(-1) }
    var destinationPage by remember { mutableIntStateOf(currentPageIndex) }

    // targetPage lets the IME react to a swipe destination before the pager settles.
    LaunchedEffect(pagerState) {
        snapshotFlow {
            if (pagerState.isScrollInProgress) pagerState.targetPage
            else pagerState.settledPage
        }
            .distinctUntilChanged()
            .collect { destinationPage = it }
    }
    val destinationKeyboardIntent =
        keyboardIntents[destinationPage] ?: KeyboardIntent.Unknown

    LaunchedEffect(
        destinationPage,
        destinationKeyboardIntent,
        pagerState.isScrollInProgress,
        availableFocusTargets,
    ) {
        when (val action = planPagerInputAction(
            destinationPage = destinationPage,
            destinationKeyboardIntent = destinationKeyboardIntent,
            isScrollInProgress = pagerState.isScrollInProgress,
            availableTargets = availableFocusTargets[destinationPage],
            activeField = activeField,
            lastAppliedKeyboardIntent = lastAppliedKeyboardIntent,
            focusOwnerPage = focusOwnerPage,
        )) {
            PagerInputAction.None -> Unit

            is PagerInputAction.FocusDestination -> {
                pageFocusRequesters[action.page]
                    .forTarget(action.field)
                    .requestFocus()
                if (action.showKeyboard) keyboardController?.show()
                lastAppliedKeyboardIntent = KeyboardIntent.Show
                focusOwnerPage = action.page
            }

            is PagerInputAction.Hide -> {
                if (action.hideKeyboard) {
                    keyboardController?.hide()
                    lastAppliedKeyboardIntent = KeyboardIntent.Hide
                }
                if (action.clearFocus) {
                    focusManager.clearFocus()
                    focusOwnerPage = destinationPage
                }
            }
        }
    }

    // User-driven pager position -> ViewModel. settledPage changes only after
    // the snap completes, so it cannot cancel an in-flight programmatic move.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onPageChanged(page)
            }
    }

    // Button-driven ViewModel position -> pager.
    LaunchedEffect(currentPageIndex) {
        // Announce the destination before animateScrollToPage starts so the
        // keyboard transition begins in the same frame as a button press.
        destinationPage = currentPageIndex
        if (currentPageIndex != pagerState.settledPage) {
            pagerState.animateScrollToPage(
                page = currentPageIndex,
                animationSpec = firmSnap,
            )
        }
    }
}

internal enum class KeyboardIntent { Unknown, Show, Hide }

internal sealed interface PagerInputAction {
    data object None : PagerInputAction

    data class FocusDestination(
        val page: Int,
        val field: TrackerPageFocusTarget,
        val showKeyboard: Boolean,
    ) : PagerInputAction

    data class Hide(
        val hideKeyboard: Boolean,
        val clearFocus: Boolean,
    ) : PagerInputAction
}

internal fun planPagerInputAction(
    destinationPage: Int,
    destinationKeyboardIntent: KeyboardIntent,
    isScrollInProgress: Boolean,
    availableTargets: Set<TrackerPageFocusTarget>?,
    activeField: TrackerPageFocusTarget,
    lastAppliedKeyboardIntent: KeyboardIntent?,
    focusOwnerPage: Int,
): PagerInputAction = when (destinationKeyboardIntent) {
    KeyboardIntent.Unknown -> PagerInputAction.None

    KeyboardIntent.Show -> {
        val focusBelongsToAnotherPage = destinationPage != focusOwnerPage
        val keyboardNeedsShowing = lastAppliedKeyboardIntent != KeyboardIntent.Show
        val shouldReconcileAfterSettling =
            !isScrollInProgress && focusBelongsToAnotherPage
        val shouldTransferSupplementalFocus =
            isScrollInProgress &&
                focusBelongsToAnotherPage &&
                activeField != TrackerPageFocusTarget.Value
        val shouldFocusDestination =
            keyboardNeedsShowing ||
                shouldReconcileAfterSettling ||
                shouldTransferSupplementalFocus

        if (availableTargets == null || !shouldFocusDestination) {
            PagerInputAction.None
        } else {
            PagerInputAction.FocusDestination(
                page = destinationPage,
                field = activeField.takeIf { it in availableTargets }
                    ?: TrackerPageFocusTarget.Value,
                showKeyboard = keyboardNeedsShowing,
            )
        }
    }

    KeyboardIntent.Hide -> {
        val keyboardNeedsHiding = lastAppliedKeyboardIntent != KeyboardIntent.Hide
        val shouldClearFocus =
            !isScrollInProgress && destinationPage != focusOwnerPage

        if (!keyboardNeedsHiding && !shouldClearFocus) {
            PagerInputAction.None
        } else {
            PagerInputAction.Hide(
                hideKeyboard = keyboardNeedsHiding,
                clearFocus = shouldClearFocus,
            )
        }
    }
}

private val SuggestedValuesViewState.keyboardIntent: KeyboardIntent
    get() = when {
        !isLoaded -> KeyboardIntent.Unknown
        values?.all { it.value == null } == true -> KeyboardIntent.Show
        else -> KeyboardIntent.Hide
    }

@Composable
private fun HintHeader(
    indexText: String,
    trackedCount: Int,
    anyFieldLocked: Boolean,
    onTutorialButtonPressed: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxWidth()
) {
    // Left-aligned index text
    Text(
        text = indexText,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
        modifier = Modifier.align(Alignment.CenterStart)
    )

    // Center-aligned tracked count indicator with animation
    if (trackedCount > 0 && anyFieldLocked) {
        TrackedCountIndicator(
            modifier = Modifier.align(Alignment.Center),
            count = trackedCount
        )
    }

    // Right-aligned FAQ button
    IconButton(
        onClick = onTutorialButtonPressed,
        modifier = Modifier.align(Alignment.CenterEnd)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.faq_icon),
            contentDescription = stringResource(id = R.string.help),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TrackedCountIndicator(
    modifier: Modifier = Modifier,
    count: Int
) {
    // Animation state for the expanding/fading icon
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }

    // Track previous count to detect increases
    val previousCount = remember { mutableIntStateOf(0) }

    // Track text width to position icon dynamically
    var textWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Trigger animation when count increases (handles first addition too)
    LaunchedEffect(count) {
        if (count > previousCount.intValue) {
            // Reset and start the animation
            launch {
                scale.snapTo(1f)
                scale.animateTo(
                    targetValue = 3.0f,
                    animationSpec = tween(durationMillis = 400)
                )
            }
            launch {
                alpha.snapTo(1f)
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 400)
                )
            }
        }
        previousCount.intValue = count
    }

    // Box where only the text affects the size, icon is positioned absolutely
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                textWidth = coordinates.size.width
            }
        )

        // Icon positioned to the right of the text based on measured width
        Icon(
            painter = painterResource(id = R.drawable.ic_add_record),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .offset {
                    // Position from center: text width + small gap
                    IntOffset(
                        x = textWidth + with(density) { 8.dp.roundToPx() },
                        y = 0
                    )
                }
                .size(smallIconSize)
                .scale(scale.value)
                .alpha(alpha.value)
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
            currentPageIndex = 0,
            trackedCount = 3
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
