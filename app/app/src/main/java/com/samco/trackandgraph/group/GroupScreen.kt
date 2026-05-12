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
package com.samco.trackandgraph.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsNavigationViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.addgroup.AddGroupDialog
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.graphstatview.ui.GraphStatContextMenuCallbacks
import com.samco.trackandgraph.importexport.ImportExportDialog
import com.samco.trackandgraph.permissions.rememberAlarmAndNotificationPermissionRequester
import com.samco.trackandgraph.permissions.rememberNotificationPermissionRequester
import com.samco.trackandgraph.releasenotes.ReleaseNotesDialog
import com.samco.trackandgraph.releasenotes.ReleaseNotesViewModel
import com.samco.trackandgraph.releasenotes.ReleaseNotesViewModelImpl
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.ContinueDialog
import com.samco.trackandgraph.ui.ui.EmptyPageHintText
import com.samco.trackandgraph.ui.ui.FeatureInfoDialog
import com.samco.trackandgraph.ui.ui.FloatingBarButton
import com.samco.trackandgraph.ui.ui.LoadingOverlay
import com.samco.trackandgraph.ui.ui.cardMarginSmall
import com.samco.trackandgraph.ui.ui.fabEnterTransition
import com.samco.trackandgraph.ui.ui.fabExitTransition
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.ui.inputSpacingXLarge
import com.samco.trackandgraph.ui.utils.plus
import com.samco.trackandgraph.util.performTrackVibrate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.serialization.Serializable
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.util.Locale

@Serializable
data class GroupNavKey(
    val groupId: Long = 0L,
    val groupName: String? = null,
    /**
     * Set by deep-link navigation. When non-null, [GroupViewModel] scrolls the grid to the
     * placement with this id once children load. Consumed once by the ViewModel — subsequent
     * recompositions do not re-trigger the scroll.
     */
    val scrollToGroupItemId: Long? = null,
) : NavKey

@Composable
fun GroupScreen(
    navArgs: GroupNavKey,
    onTrackerEdit: (DisplayTracker) -> Unit = {},
    onGraphStatEdit: (IGraphStatViewData) -> Unit = {},
    onGraphStatClick: (IGraphStatViewData) -> Unit = {},
    onGroupClick: (Group) -> Unit = {},
    onTrackerHistory: (DisplayTracker) -> Unit = {},
    onFunctionEdit: (DisplayFunction) -> Unit = {},
    onFunctionClick: (DisplayFunction) -> Unit = {},
    onAddTracker: (Long) -> Unit = {},
    onAddGraphStat: (Long) -> Unit = {},
    onAddFunction: (Long) -> Unit = {},
) {
    val groupViewModel: GroupViewModel = hiltViewModel<GroupViewModelImpl>()
    val groupDialogsViewModel: GroupDialogsViewModel = hiltViewModel()
    val addGroupDialogViewModel: AddGroupDialogViewModelImpl = hiltViewModel()
    val addSymlinkViewModel: AddSymlinkViewModel = hiltViewModel<AddSymlinkViewModelImpl>()
    val releaseNotesViewModel: ReleaseNotesViewModel = hiltViewModel<ReleaseNotesViewModelImpl>()
    val symlinksDialogViewModel: SymlinksDialogViewModel = hiltViewModel()
    val searchViewModel: GroupSearchViewModel = hiltViewModel<GroupSearchViewModelImpl>()
    val addDataPointsDialogViewModel: AddDataPointsNavigationViewModel =
        hiltViewModel<AddDataPointsViewModelImpl>()

    val isSearchVisible by searchViewModel.isSearchVisible.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    LaunchedEffect(navArgs.groupId) {
        groupViewModel.setGroup(navArgs.groupId)
        searchViewModel.setGroupId(navArgs.groupId)
    }

    // Local state for FAB visibility based on scroll behavior
    val showFab = remember { mutableStateOf(true) }

    val onTrackerAdd: (DisplayTracker, Boolean) -> Unit = { tracker, useDefault ->
        if (tracker.hasDefaultValue && useDefault) {
            context.performTrackVibrate()
            groupViewModel.addDefaultTrackerValue(tracker)
        } else {
            addDataPointsDialogViewModel.showAddDataPointDialog(trackerId = tracker.id)
        }
    }
    val onTrackerPlayTimer: (DisplayTracker) -> Unit = { tracker ->
        groupViewModel.playTimer(tracker)
        requestNotificationPermission()
    }
    val onTrackerStopTimer: (DisplayTracker) -> Unit = { groupViewModel.stopTimer(it) }

    if (isSearchVisible) {
        SearchScreen(
            navArgs = navArgs,
            searchViewModel = searchViewModel,
            onBack = { searchViewModel.hideSearch() },
            onTrackerAdd = onTrackerAdd,
            onTrackerPlayTimer = onTrackerPlayTimer,
            onTrackerStopTimer = onTrackerStopTimer,
        )
    } else {
        GroupTopBarContent(
            navArgs = navArgs,
            groupViewModel = groupViewModel,
            groupDialogsViewModel = groupDialogsViewModel,
            addGroupDialogViewModel = addGroupDialogViewModel,
            onAddTracker = onAddTracker,
            onAddGraphStat = onAddGraphStat,
            onAddFunction = onAddFunction,
            onAddSymlink = { addSymlinkViewModel.show(it) },
            showFab = showFab,
            onSearchClick = { searchViewModel.showSearch() },
        )

        GroupScreenContent(
            groupViewModel = groupViewModel,
            groupDialogsViewModel = groupDialogsViewModel,
            addGroupDialogViewModel = addGroupDialogViewModel,
            addSymlinkViewModel = addSymlinkViewModel,
            releaseNotesViewModel = releaseNotesViewModel,
            symlinksDialogViewModel = symlinksDialogViewModel,
            addDataPointsDialogViewModel = addDataPointsDialogViewModel,
            groupId = navArgs.groupId,
            groupName = navArgs.groupName,
            scrollToGroupItemId = navArgs.scrollToGroupItemId,
            onTrackerEdit = onTrackerEdit,
            onGraphStatEdit = onGraphStatEdit,
            onGraphStatClick = onGraphStatClick,
            onGroupClick = onGroupClick,
            onTrackerHistory = onTrackerHistory,
            onFunctionEdit = onFunctionEdit,
            onFunctionClick = onFunctionClick,
            onTrackerAdd = onTrackerAdd,
            onTrackerPlayTimer = onTrackerPlayTimer,
            onTrackerStopTimer = onTrackerStopTimer,
            showFab = showFab,
        )
    }

    // Rendered at the outer level so the dialog persists across search open/close
    // — the underlying ViewModel state drives visibility, but the composable must
    // be in composition in both branches to react to state changes.
    AddDataPointsDialog(
        addDataPointsDialogViewModel,
        onDismissRequest = { addDataPointsDialogViewModel.reset() }
    )

    // Bridge: stopping a timer sets showDurationInputDialog on groupViewModel; we
    // forward that into addDataPointsDialogViewModel so the duration dialog opens.
    // Hoisted out of GroupScreenContent so it stays in composition while search is
    // open — otherwise stop-timer taps from search cards would never open the dialog.
    val showDurationInputDialog =
        groupViewModel.showDurationInputDialog.collectAsStateWithLifecycle().value
    LaunchedEffect(showDurationInputDialog) {
        if (showDurationInputDialog == null) return@LaunchedEffect
        addDataPointsDialogViewModel.showAddDataPointDialog(
            trackerId = showDurationInputDialog.trackerId,
            customInitialValue = showDurationInputDialog.duration.seconds.toDouble()
        )
        groupViewModel.onConsumedShowDurationInputDialog()
    }
}

/** Data classes for click listeners with default empty lambda values */
data class TrackerClickListeners(
    val onEdit: (DisplayTracker) -> Unit = {},
    val onDescription: (DisplayTracker) -> Unit = {},
    val onSymlinks: (DisplayTracker) -> Unit = {},
    val onAdd: (DisplayTracker, Boolean) -> Unit = { _, _ -> },
    val onHistory: (DisplayTracker) -> Unit = {},
    val onPlayTimer: (DisplayTracker) -> Unit = {},
    val onStopTimer: (DisplayTracker) -> Unit = {}
)

data class GraphStatClickListeners(
    val onEdit: (IGraphStatViewData) -> Unit = {},
    val onClick: (IGraphStatViewData) -> Unit = {},
    val onSymlinks: (IGraphStatViewData) -> Unit = {},
)

data class GroupClickListeners(
    val onClick: (Group) -> Unit = {},
    val onEdit: (Group) -> Unit = {},
    val onSymlinks: (Group) -> Unit = {},
)

data class FunctionClickListeners(
    val onClick: (DisplayFunction) -> Unit = {},
    val onEdit: (DisplayFunction) -> Unit = {},
    val onDescription: (DisplayFunction) -> Unit = {},
    val onSymlinks: (DisplayFunction) -> Unit = {},
)

/** Content component for GroupScreen with navigation callbacks */
@Composable
private fun GroupScreenContent(
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    addSymlinkViewModel: AddSymlinkViewModel,
    releaseNotesViewModel: ReleaseNotesViewModel,
    symlinksDialogViewModel: SymlinksDialogViewModel,
    addDataPointsDialogViewModel: AddDataPointsNavigationViewModel,
    groupId: Long,
    groupName: String?,
    scrollToGroupItemId: Long? = null,
    onTrackerEdit: (DisplayTracker) -> Unit = {},
    onGraphStatEdit: (IGraphStatViewData) -> Unit = {},
    onGraphStatClick: (IGraphStatViewData) -> Unit = {},
    onGroupClick: (Group) -> Unit = {},
    onTrackerHistory: (DisplayTracker) -> Unit = {},
    onFunctionEdit: (DisplayFunction) -> Unit = {},
    onFunctionClick: (DisplayFunction) -> Unit = {},
    onTrackerAdd: (DisplayTracker, Boolean) -> Unit,
    onTrackerPlayTimer: (DisplayTracker) -> Unit,
    onTrackerStopTimer: (DisplayTracker) -> Unit,
    showFab: State<Boolean>,
) {
    val isLoading = groupViewModel.loading.collectAsStateWithLifecycle().value
    val showEmptyText = groupViewModel.showEmptyGroupText.collectAsStateWithLifecycle().value
    val groupHasTrackers = groupViewModel.groupHasAnyTrackers.collectAsStateWithLifecycle().value
    val allChildren = groupViewModel.currentChildren.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        groupViewModel.scrollToTopEvents.receiveAsFlow().collect {
            groupViewModel.lazyGridState.animateScrollToItem(0)
        }
    }

    ScrollToGroupItemEffect(
        targetGroupItemId = scrollToGroupItemId,
        lazyGridState = groupViewModel.lazyGridState,
        children = groupViewModel.currentChildren,
    )

    val moveItemViewModel: MoveItemViewModel = hiltViewModel()

    // Permission handling
    val requestAlarmAndNotificationPermission = rememberAlarmAndNotificationPermissionRequester()
    LaunchedEffect(groupViewModel.hasAnyReminders) {
        groupViewModel.hasAnyReminders
            .filter { it }
            .take(1)
            .collect { requestAlarmAndNotificationPermission() }
    }

    val showReleaseNotesButton =
        releaseNotesViewModel.showReleaseNotesButton.collectAsStateWithLifecycle().value
    val showReleaseNotesDialog =
        releaseNotesViewModel.showReleaseNotesDialog.collectAsStateWithLifecycle().value
    val releaseNotes = releaseNotesViewModel.releaseNotes.collectAsStateWithLifecycle().value

    GroupScreenView(
        lazyGridState = groupViewModel.lazyGridState,
        isLoading = isLoading,
        showEmptyText = showEmptyText,
        // Only show FAB if scroll allows it AND we have trackers
        showFab = showFab.value && groupHasTrackers,
        showReleaseNotesButton = showFab.value && showReleaseNotesButton,
        onQueueAddAllClicked = {
            groupViewModel.getTrackersInGroup().let { trackers ->
                addDataPointsDialogViewModel.showAddDataPointsDialog(trackerIds = trackers.map { it.id })
            }
        },
        onReleaseNotesClicked = releaseNotesViewModel::onClickReleaseNotesButton,
        allChildren = allChildren,
        trackerClickListeners = TrackerClickListeners(
            onEdit = onTrackerEdit,
            onDescription = { groupDialogsViewModel.showFeatureDescriptionDialog(it) },
            onSymlinks = {
                symlinksDialogViewModel.showSymlinks(
                    it.id,
                    GroupChildType.TRACKER,
                    it.name
                )
            },
            onAdd = onTrackerAdd,
            onHistory = onTrackerHistory,
            onPlayTimer = onTrackerPlayTimer,
            onStopTimer = onTrackerStopTimer,
        ),
        graphStatClickListeners = GraphStatClickListeners(
            onEdit = onGraphStatEdit,
            onClick = onGraphStatClick,
            onSymlinks = {
                symlinksDialogViewModel.showSymlinks(
                    it.graphOrStat.id,
                    GroupChildType.GRAPH,
                    it.graphOrStat.name
                )
            },
        ),
        groupClickListeners = GroupClickListeners(
            onClick = onGroupClick,
            onEdit = { group ->
                addGroupDialogViewModel.showForEdit(groupId = group.id)
            },
            onSymlinks = {
                symlinksDialogViewModel.showSymlinks(
                    it.id,
                    GroupChildType.GROUP,
                    it.name
                )
            },
        ),
        functionClickListeners = FunctionClickListeners(
            onClick = onFunctionClick,
            onEdit = onFunctionEdit,
            onDescription = { groupDialogsViewModel.showFunctionDescriptionDialog(it) },
            onSymlinks = {
                symlinksDialogViewModel.showSymlinks(
                    it.id,
                    GroupChildType.FUNCTION,
                    it.name
                )
            },
        ),
        onDeleteItem = { groupItemId, type, unique ->
            groupDialogsViewModel.showDeleteDialog(groupItemId, type, unique)
        },
        onMoveItem = { groupItemId, hiddenItems ->
            moveItemViewModel.showMoveDialog(groupItemId, hiddenItems)
        },
        onDuplicateGraph = groupViewModel::onDuplicateGraphOrStat,
        onDuplicateFunction = groupViewModel::onDuplicateFunction,
        onDragStart = groupViewModel::onDragStart,
        onDragSwap = groupViewModel::onDragSwap,
        onDragEnd = groupViewModel::onDragEnd,
    )

    // Dialogs
    AddGroupDialog(
        viewModel = addGroupDialogViewModel,
        onDismissRequest = { addGroupDialogViewModel.hide() }
    )

    AddSymlinkDialog(
        viewModel = addSymlinkViewModel,
        onDismissRequest = { addSymlinkViewModel.hide() }
    )

    if (groupDialogsViewModel.showImportExportDialog.collectAsStateWithLifecycle().value) {
        val vmGroupName = groupViewModel.groupName.collectAsStateWithLifecycle().value
        ImportExportDialog(
            trackGroupId = groupId,
            trackGroupName = groupName ?: vmGroupName,
            onDismissRequest = { groupDialogsViewModel.hideImportExportDialog() }
        )
    }

    val displayTracker =
        groupDialogsViewModel.featureForDescriptionDialog.collectAsStateWithLifecycle().value
    if (displayTracker != null) {
        FeatureInfoDialog(
            featureName = displayTracker.name,
            featureDescription = displayTracker.description,
            onDismissRequest = { groupDialogsViewModel.hideFeatureDescriptionDialog() }
        )
    }

    val displayFunction =
        groupDialogsViewModel.functionForDescriptionDialog.collectAsStateWithLifecycle().value
    if (displayFunction != null) {
        FeatureInfoDialog(
            featureName = displayFunction.name,
            featureDescription = displayFunction.description,
            onDismissRequest = { groupDialogsViewModel.hideFunctionDescriptionDialog() }
        )
    }

    // Move dialog
    val moveDialogConfig = moveItemViewModel.moveDialogConfig.collectAsStateWithLifecycle().value

    if (moveDialogConfig != null) {
        SelectItemDialog(
            title = stringResource(R.string.move_to),
            selectableTypes = setOf(SelectableItemType.GROUP),
            hiddenItems = moveDialogConfig.hiddenItems,
            onGroupSelected = moveItemViewModel::moveItemToGroup,
            onDismissRequest = { moveItemViewModel.dismissMoveDialog() }
        )
    }

    // Symlinks dialog
    SymlinksDialog(viewModel = symlinksDialogViewModel)

    // Confirmation dialogs
    GroupDeleteDialog(groupDialogsViewModel, groupViewModel)

    if (groupDialogsViewModel.showNoTrackersDialog.collectAsStateWithLifecycle().value) {
        ContinueDialog(
            body = R.string.no_trackers_graph_stats_hint,
            onConfirm = { groupDialogsViewModel.hideNoTrackersDialog() },
            onDismissRequest = { groupDialogsViewModel.hideNoTrackersDialog() },
            continueText = R.string.ok
        )
    }

    if (groupDialogsViewModel.showNoTrackersFunctionsDialog.collectAsStateWithLifecycle().value) {
        ContinueDialog(
            body = R.string.no_trackers_functions_hint,
            onConfirm = { groupDialogsViewModel.hideNoTrackersFunctionsDialog() },
            onDismissRequest = { groupDialogsViewModel.hideNoTrackersFunctionsDialog() },
            continueText = R.string.ok
        )
    }

    // Release notes dialog
    if (showReleaseNotesDialog) {
        ReleaseNotesDialog(
            releaseNotes = releaseNotes,
            onDismissRequest = releaseNotesViewModel::onDismissReleaseNotesDialog,
            onDonateClicked = releaseNotesViewModel::onDonateClicked,
            onSkipDonationClicked = releaseNotesViewModel::onDismissReleaseNotesDialog
        )
    }
}

@Composable
fun GroupScreenView(
    lazyGridState: LazyGridState,
    isLoading: Boolean,
    showEmptyText: Boolean,
    showFab: Boolean,
    showReleaseNotesButton: Boolean,
    allChildren: List<GroupChild>,
    onQueueAddAllClicked: () -> Unit = {},
    onReleaseNotesClicked: () -> Unit = {},
    trackerClickListeners: TrackerClickListeners? = null,
    graphStatClickListeners: GraphStatClickListeners? = null,
    groupClickListeners: GroupClickListeners? = null,
    functionClickListeners: FunctionClickListeners? = null,
    onDeleteItem: (groupItemId: Long, type: DeleteType, unique: Boolean) -> Unit = { _, _, _ -> },
    onMoveItem: (groupItemId: Long, hiddenItems: Set<HiddenItem>) -> Unit = { _, _ -> },
    onDuplicateGraph: (groupItemId: Long) -> Unit = {},
    onDuplicateFunction: (groupItemId: Long, newName: String) -> Unit = { _, _ -> },
    onDragStart: () -> Unit = {},
    onDragSwap: (Int, Int) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    fabInsetPaddingOverride: PaddingValues? = null,
) {
    val resolvedFabInsetPadding = fabInsetPaddingOverride
        ?: WindowInsets.navigationBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        // Group grid with items
        GroupGrid(
            modifier = Modifier
                .testTag(if (isSystemInDarkTheme()) "darkTheme" else "lightTheme")
                .fillMaxSize(),
            lazyGridState = lazyGridState,
            allChildren = allChildren,
            trackerClickListeners = trackerClickListeners ?: TrackerClickListeners(),
            graphStatClickListeners = graphStatClickListeners ?: GraphStatClickListeners(),
            groupClickListeners = groupClickListeners ?: GroupClickListeners(),
            functionClickListeners = functionClickListeners ?: FunctionClickListeners(),
            onDeleteItem = onDeleteItem,
            onMoveItem = onMoveItem,
            onDuplicateGraph = onDuplicateGraph,
            onDuplicateFunction = onDuplicateFunction,
            onDragStart = onDragStart,
            onDragSwap = onDragSwap,
            onDragEnd = onDragEnd,
        )

        // Empty group text
        if (showEmptyText) {
            EmptyPageHintText(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(inputSpacingXLarge),
                text = stringResource(id = R.string.empty_group_hint),
            )
        }

        // FAB positioned manually at bottom end
        AnimatedVisibility(
            visible = showFab,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fabEnterTransition,
            exit = fabExitTransition
        ) {
            FloatingActionButton(
                onClick = onQueueAddAllClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("trackAllFab")
                    .padding(resolvedFabInsetPadding)
                    .then(Modifier.padding(inputSpacingLarge))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.queue_add_box),
                    contentDescription = stringResource(id = R.string.track_all)
                )
            }
        }

        // Release notes button positioned at bottom center
        AnimatedVisibility(
            visible = showReleaseNotesButton,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fabEnterTransition,
            exit = fabExitTransition
        ) {
            FloatingBarButton(
                onClick = onReleaseNotesClicked,
                text = stringResource(id = R.string.see_whats_new),
                icon = R.drawable.deployed_code_update_24px,
                modifier = Modifier
                    .testTag("releaseNotesButton")
                    .padding(resolvedFabInsetPadding)
                    .widthIn(max = 300.dp)
                    .then(Modifier.padding(inputSpacingLarge))
            )
        }

        // Loading overlay
        if (isLoading) LoadingOverlay()
    }
}

@Composable
private fun GroupGrid(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState,
    allChildren: List<GroupChild>,
    trackerClickListeners: TrackerClickListeners,
    graphStatClickListeners: GraphStatClickListeners,
    groupClickListeners: GroupClickListeners,
    functionClickListeners: FunctionClickListeners,
    onDeleteItem: (groupItemId: Long, type: DeleteType, unique: Boolean) -> Unit,
    onMoveItem: (groupItemId: Long, hiddenItems: Set<HiddenItem>) -> Unit,
    onDuplicateGraph: (groupItemId: Long) -> Unit,
    onDuplicateFunction: (groupItemId: Long, newName: String) -> Unit,
    onDragStart: () -> Unit,
    onDragSwap: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    val columnCount = (maxWidth / minColumnWidth).toInt().coerceAtLeast(2)
    val duplicateNameFormat = stringResource(R.string.duplicate_name_format)
    val functionNames = remember(allChildren) {
        allChildren
            .filterIsInstance<GroupChild.ChildFunction>()
            .map { it.displayFunction.name }
            .toSet()
    }

    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onDragSwap(from.index, to.index)
    }

    LaunchedEffect(reorderableLazyGridState.isAnyItemDragging) {
        if (reorderableLazyGridState.isAnyItemDragging) onDragStart() else onDragEnd()
    }

    LazyVerticalGrid(
        modifier = Modifier
            .testTag("groupGrid")
            .fillMaxSize(),
        state = lazyGridState,
        columns = GridCells.Fixed(columnCount),
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues() + PaddingValues(vertical = cardMarginSmall),
    ) {
        items(
            items = allChildren,
            key = { it.groupItemId },
            span = { item ->
                when (item) {
                    is GroupChild.ChildTracker -> GridItemSpan(1)
                    is GroupChild.ChildTrackerLoading -> GridItemSpan(1)
                    is GroupChild.ChildFunction -> GridItemSpan(1)
                    is GroupChild.ChildGroup -> GridItemSpan(2)
                    is GroupChild.ChildGraph -> GridItemSpan(columnCount)
                }
            }
        ) { item ->
            ReorderableItem(
                reorderableLazyGridState,
                key = item.groupItemId
            ) { isDragging ->
                when (item) {
                    is GroupChild.ChildTracker -> {
                        TrackerItem(
                            tracker = item.displayTracker,
                            clickListeners = trackerClickListeners,
                            onDelete = {
                                onDeleteItem(
                                    item.groupItemId,
                                    DeleteType.TRACKER,
                                    it.unique
                                )
                            },
                            onMoveTo = { onMoveItem(item.groupItemId, emptySet()) },
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildTrackerLoading -> {
                        // Group screen doesn't lazy-load trackers; this branch only exists
                        // for exhaustiveness (the loading variant is search-only).
                        LoadingTracker(name = item.name)
                    }

                    is GroupChild.ChildFunction -> {
                        val duplicateName = remember(
                            item.displayFunction.name,
                            functionNames,
                            duplicateNameFormat,
                        ) {
                            nextDuplicateName(
                                originalName = item.displayFunction.name,
                                existingNames = functionNames,
                                formatName = { baseName, suffix ->
                                    String.format(
                                        Locale.ROOT,
                                        duplicateNameFormat,
                                        baseName,
                                        suffix,
                                    )
                                },
                            )
                        }
                        FunctionItem(
                            displayFunction = item.displayFunction,
                            clickListeners = functionClickListeners,
                            onDelete = {
                                onDeleteItem(
                                    item.groupItemId,
                                    DeleteType.FUNCTION,
                                    it.unique
                                )
                            },
                            onMove = { onMoveItem(item.groupItemId, emptySet()) },
                            onDuplicate = {
                                onDuplicateFunction(item.groupItemId, duplicateName)
                            },
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildGroup -> {
                        GroupItem(
                            group = item.group,
                            clickListeners = groupClickListeners,
                            onDelete = {
                                onDeleteItem(
                                    item.groupItemId,
                                    DeleteType.GROUP,
                                    it.unique
                                )
                            },
                            onMove = {
                                onMoveItem(
                                    item.groupItemId,
                                    setOf(HiddenItem(SelectableItemType.GROUP, it.id))
                                )
                            },
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildGraph -> {
                        GraphStatItem(
                            graphStat = item.graph.viewData,
                            unique = item.graph.unique,
                            clickListeners = graphStatClickListeners,
                            onDelete = {
                                onDeleteItem(
                                    item.groupItemId,
                                    DeleteType.GRAPH_STAT,
                                    item.graph.unique
                                )
                            },
                            onMove = { onMoveItem(item.groupItemId, emptySet()) },
                            onDuplicate = {
                                onDuplicateGraph(item.groupItemId)
                            },
                            isElevated = isDragging,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.TrackerItem(
    tracker: DisplayTracker,
    clickListeners: TrackerClickListeners,
    onDelete: (DisplayTracker) -> Unit,
    onMoveTo: (DisplayTracker) -> Unit,
    isElevated: Boolean,
) = Tracker(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    tracker = tracker,
    onClick = { clickListeners.onHistory(it) },
    onAdd = { t, useDefault -> clickListeners.onAdd(t, useDefault) },
    onPlayTimer = { clickListeners.onPlayTimer(it) },
    onStopTimer = { clickListeners.onStopTimer(it) },
    contextMenuCallbacks = TrackerContextMenuCallbacks(
        onEdit = { clickListeners.onEdit(it) },
        onDelete = onDelete,
        onMoveTo = onMoveTo,
        onDescription = { clickListeners.onDescription(it) },
        onSymlinks = { clickListeners.onSymlinks(it) },
    ),
)

@Composable
private fun ReorderableCollectionItemScope.GroupItem(
    group: Group,
    clickListeners: GroupClickListeners,
    onDelete: (Group) -> Unit,
    onMove: (Group) -> Unit,
    isElevated: Boolean,
) = Group(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    group = group,
    onClick = { clickListeners.onClick(it) },
    contextMenuCallbacks = GroupContextMenuCallbacks(
        onEdit = { clickListeners.onEdit(it) },
        onDelete = onDelete,
        onMoveTo = onMove,
        onSymlinks = { clickListeners.onSymlinks(it) },
    ),
)

@Composable
private fun ReorderableCollectionItemScope.GraphStatItem(
    graphStat: IGraphStatViewData,
    unique: Boolean,
    clickListeners: GraphStatClickListeners,
    onDelete: (IGraphStatViewData) -> Unit,
    onMove: (IGraphStatViewData) -> Unit,
    onDuplicate: (IGraphStatViewData) -> Unit,
    isElevated: Boolean,
) = GraphStatCardView(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    graphStatViewData = graphStat,
    unique = unique,
    onClick = { clickListeners.onClick(it) },
    contextMenuCallbacks = GraphStatContextMenuCallbacks(
        onEdit = { clickListeners.onEdit(it) },
        onDelete = onDelete,
        onMove = onMove,
        onDuplicate = onDuplicate,
        onSymlinks = { clickListeners.onSymlinks(it) },
    ),
)

@Composable
private fun ReorderableCollectionItemScope.FunctionItem(
    displayFunction: DisplayFunction,
    clickListeners: FunctionClickListeners,
    onDelete: (DisplayFunction) -> Unit,
    onMove: (DisplayFunction) -> Unit,
    onDuplicate: (DisplayFunction) -> Unit,
    isElevated: Boolean,
) = Function(
    modifier = Modifier.longPressDraggableHandle(),
    displayFunction = displayFunction,
    isElevated = isElevated,
    onClick = { clickListeners.onClick(displayFunction) },
    contextMenuCallbacks = FunctionContextMenuCallbacks(
        onEdit = { clickListeners.onEdit(displayFunction) },
        onDelete = { onDelete(displayFunction) },
        onMoveTo = { onMove(displayFunction) },
        onDuplicate = { onDuplicate(displayFunction) },
        onDescription = { clickListeners.onDescription(displayFunction) },
        onSymlinks = { clickListeners.onSymlinks(displayFunction) },
    ),
)

/**
 * Deep-link scroll: when [targetGroupItemId] is non-null, wait for the corresponding placement
 * to appear in [children] and for the grid to lay out, then animate-scroll to it once.
 *
 * The consumed flag is `rememberSaveable`-keyed on the target id so recomposition and
 * process-death restore do not re-scroll after the user has moved on; a new target id
 * (from a subsequent deep link) starts fresh.
 */
@Composable
private fun ScrollToGroupItemEffect(
    targetGroupItemId: Long?,
    lazyGridState: LazyGridState,
    children: kotlinx.coroutines.flow.StateFlow<List<GroupChild>>,
) {
    if (targetGroupItemId == null) return
    var scrollConsumed by rememberSaveable(targetGroupItemId) { mutableStateOf(false) }
    LaunchedEffect(targetGroupItemId, scrollConsumed) {
        if (scrollConsumed) return@LaunchedEffect
        val list = children.first { it.any { c -> c.groupItemId == targetGroupItemId } }
        val index = list.indexOfFirst { it.groupItemId == targetGroupItemId }
        if (index < 0) return@LaunchedEffect
        // Wait for the grid to actually lay out enough items before scrolling.
        snapshotFlow { lazyGridState.layoutInfo.totalItemsCount }.first { it >= index + 1 }
        // Let the grid settle — without this the first frames of the animation are dropped
        // and the scroll looks instant.
        delay(200)
        lazyGridState.animateScrollToItem(index)
        scrollConsumed = true
    }
}

internal val minColumnWidth = 180.dp

@Preview(showBackground = true)
@Composable
private fun GroupScreenViewEmptyPreview() {
    TnGComposeTheme {
        GroupScreenView(
            lazyGridState = rememberLazyGridState(),
            allChildren = listOf(),
            isLoading = false,
            showEmptyText = true,
            showFab = true,
            showReleaseNotesButton = true,
            trackerClickListeners = TrackerClickListeners(),
            graphStatClickListeners = GraphStatClickListeners(),
            groupClickListeners = GroupClickListeners(),
            functionClickListeners = FunctionClickListeners(),
        )
    }
}
