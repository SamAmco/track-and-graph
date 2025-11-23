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

import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.graphstatview.ui.GraphStatClickListener
import com.samco.trackandgraph.importexport.ExportFeaturesDialog
import com.samco.trackandgraph.importexport.ImportFeaturesDialog
import com.samco.trackandgraph.permissions.rememberAlarmAndNotificationPermissionRequester
import com.samco.trackandgraph.permissions.rememberNotificationPermissionRequester
import com.samco.trackandgraph.releasenotes.ReleaseNotesDialog
import com.samco.trackandgraph.releasenotes.ReleaseNotesViewModel
import com.samco.trackandgraph.releasenotes.ReleaseNotesViewModelImpl
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.ContinueDialog
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.EmptyPageHintText
import com.samco.trackandgraph.ui.compose.ui.FeatureInfoDialog
import com.samco.trackandgraph.ui.compose.ui.FloatingBarButton
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.inputSpacingXLarge
import com.samco.trackandgraph.ui.compose.utils.plus
import com.samco.trackandgraph.util.performTrackVibrate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Serializable
data class GroupNavKey(
    val groupId: Long = 0L,
    val groupName: String? = null
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
    val releaseNotesViewModel: ReleaseNotesViewModel = hiltViewModel<ReleaseNotesViewModelImpl>()

    LaunchedEffect(navArgs.groupId) {
        groupViewModel.setGroup(navArgs.groupId)
    }

    // Local state for FAB visibility based on scroll behavior
    val showFab = remember { mutableStateOf(true) }

    GroupTopBarContent(
        navArgs = navArgs,
        groupViewModel = groupViewModel,
        groupDialogsViewModel = groupDialogsViewModel,
        addGroupDialogViewModel = addGroupDialogViewModel,
        onAddTracker = onAddTracker,
        onAddGraphStat = onAddGraphStat,
        onAddFunction = onAddFunction,
        showFab = showFab,
    )

    GroupScreenContent(
        groupViewModel = groupViewModel,
        groupDialogsViewModel = groupDialogsViewModel,
        addGroupDialogViewModel = addGroupDialogViewModel,
        releaseNotesViewModel = releaseNotesViewModel,
        groupId = navArgs.groupId,
        groupName = navArgs.groupName,
        onTrackerEdit = onTrackerEdit,
        onGraphStatEdit = onGraphStatEdit,
        onGraphStatClick = onGraphStatClick,
        onGroupClick = onGroupClick,
        onTrackerHistory = onTrackerHistory,
        onFunctionEdit = onFunctionEdit,
        onFunctionClick = onFunctionClick,
        showFab = showFab,
    )
}

/**
 * Data classes for click listeners with default empty lambda values
 */
data class TrackerClickListeners(
    val onEdit: (DisplayTracker) -> Unit = {},
    val onDelete: (DisplayTracker) -> Unit = {},
    val onMoveTo: (DisplayTracker) -> Unit = {},
    val onDescription: (DisplayTracker) -> Unit = {},
    val onAdd: (DisplayTracker, Boolean) -> Unit = { _, _ -> },
    val onHistory: (DisplayTracker) -> Unit = {},
    val onPlayTimer: (DisplayTracker) -> Unit = {},
    val onStopTimer: (DisplayTracker) -> Unit = {}
)

data class GraphStatClickListeners(
    val onDelete: (IGraphStatViewData) -> Unit = {},
    val onEdit: (IGraphStatViewData) -> Unit = {},
    val onClick: (IGraphStatViewData) -> Unit = {},
    val onMove: (IGraphStatViewData) -> Unit = {},
    val onDuplicate: (IGraphStatViewData) -> Unit = {}
)

data class GroupClickListeners(
    val onClick: (Group) -> Unit = {},
    val onEdit: (Group) -> Unit = {},
    val onDelete: (Group) -> Unit = {},
    val onMove: (Group) -> Unit = {}
)

data class FunctionClickListeners(
    val onClick: (DisplayFunction) -> Unit = {},
    val onEdit: (DisplayFunction) -> Unit = {},
    val onDelete: (DisplayFunction) -> Unit = {},
    val onMove: (DisplayFunction) -> Unit = {},
    val onDuplicate: (DisplayFunction) -> Unit = {}
)

/**
 * Content component for GroupScreen with navigation callbacks
 */
@Composable
private fun GroupScreenContent(
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    releaseNotesViewModel: ReleaseNotesViewModel,
    groupId: Long,
    groupName: String?,
    onTrackerEdit: (DisplayTracker) -> Unit = {},
    onGraphStatEdit: (IGraphStatViewData) -> Unit = {},
    onGraphStatClick: (IGraphStatViewData) -> Unit = {},
    onGroupClick: (Group) -> Unit = {},
    onTrackerHistory: (DisplayTracker) -> Unit = {},
    onFunctionEdit: (DisplayFunction) -> Unit = {},
    onFunctionClick: (DisplayFunction) -> Unit = {},
    showFab: State<Boolean>,
) {
    val isLoading = groupViewModel.loading.collectAsStateWithLifecycle().value
    val showEmptyText = groupViewModel.showEmptyGroupText.collectAsStateWithLifecycle().value
    val groupHasTrackers = groupViewModel.groupHasAnyTrackers.collectAsStateWithLifecycle().value
    val allChildren = groupViewModel.currentChildren.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    val addDataPointsDialogViewModel: AddDataPointsNavigationViewModel = hiltViewModel<AddDataPointsViewModelImpl>()
    val moveItemViewModel: MoveItemViewModel = hiltViewModel()

    // Permission handling
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val requestAlarmAndNotificationPermission = rememberAlarmAndNotificationPermissionRequester()
    LaunchedEffect(groupViewModel.hasAnyReminders) {
        groupViewModel.hasAnyReminders
            .filter { it }
            .take(1)
            .collect { requestAlarmAndNotificationPermission() }
    }

    val showReleaseNotesButton = releaseNotesViewModel.showReleaseNotesButton.collectAsStateWithLifecycle().value
    val showReleaseNotesDialog = releaseNotesViewModel.showReleaseNotesDialog.collectAsStateWithLifecycle().value
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
            onDelete = { groupDialogsViewModel.showDeleteTrackerDialog(it) },
            onMoveTo = { moveItemViewModel.showMoveTrackerDialog(it) },
            onDescription = { groupDialogsViewModel.showFeatureDescriptionDialog(it) },
            onAdd = { tracker, useDefault ->
                if (tracker.hasDefaultValue && useDefault) {
                    context.performTrackVibrate()
                    groupViewModel.addDefaultTrackerValue(tracker)
                } else {
                    addDataPointsDialogViewModel.showAddDataPointDialog(trackerId = tracker.id)
                }
            },
            onHistory = onTrackerHistory,
            onPlayTimer = { tracker ->
                groupViewModel.playTimer(tracker)
                requestNotificationPermission()
            },
            onStopTimer = { groupViewModel.stopTimer(it) }
        ),
        graphStatClickListeners = GraphStatClickListeners(
            onDelete = { groupDialogsViewModel.showDeleteGraphStatDialog(it) },
            onEdit = onGraphStatEdit,
            onClick = onGraphStatClick,
            onMove = { moveItemViewModel.showMoveGraphDialog(it) },
            onDuplicate = { groupViewModel.duplicateGraphOrStat(it) }
        ),
        groupClickListeners = GroupClickListeners(
            onClick = onGroupClick,
            onEdit = { group ->
                addGroupDialogViewModel.show(
                    parentGroupId = group.parentGroupId,
                    groupId = group.id
                )
            },
            onDelete = { groupDialogsViewModel.showDeleteGroupDialog(it) },
            onMove = { moveItemViewModel.showMoveGroupDialog(it) }
        ),
        functionClickListeners = FunctionClickListeners(
            onClick = onFunctionClick,
            onEdit = onFunctionEdit,
            onDelete = { groupDialogsViewModel.showDeleteFunctionDialog(it) },
            onMove = { moveItemViewModel.showMoveFunctionDialog(it) },
            onDuplicate = { groupViewModel.duplicateFunction(it) }
        ),
        onDragStart = groupViewModel::onDragStart,
        onDragSwap = groupViewModel::onDragSwap,
        onDragEnd = groupViewModel::onDragEnd,
    )

    // Dialogs
    AddDataPointsDialog(
        addDataPointsDialogViewModel,
        onDismissRequest = { addDataPointsDialogViewModel.reset() }
    )

    AddGroupDialog(
        viewModel = addGroupDialogViewModel,
        onDismissRequest = { addGroupDialogViewModel.hide() }
    )

    if (groupDialogsViewModel.showImportDialog.collectAsStateWithLifecycle().value) {
        ImportFeaturesDialog(
            trackGroupId = groupId,
            onDismissRequest = { groupDialogsViewModel.hideImportDialog() }
        )
    }

    if (groupDialogsViewModel.showExportDialog.collectAsStateWithLifecycle().value) {
        ExportFeaturesDialog(
            trackGroupId = groupId,
            trackGroupName = groupName,
            onDismissRequest = { groupDialogsViewModel.hideExportDialog() }
        )
    }

    val displayTracker = groupDialogsViewModel.featureForDescriptionDialog.collectAsStateWithLifecycle().value
    if (displayTracker != null) {
        FeatureInfoDialog(
            featureName = displayTracker.name,
            featureDescription = displayTracker.description,
            onDismissRequest = { groupDialogsViewModel.hideFeatureDescriptionDialog() }
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

    // Confirmation dialogs
    val itemForDeletion = groupDialogsViewModel.itemForDeletion.collectAsStateWithLifecycle().value
    if (itemForDeletion != null) {
        val bodyRes = when (itemForDeletion.type) {
            DeleteType.GROUP -> R.string.ru_sure_del_group
            DeleteType.GRAPH_STAT -> R.string.ru_sure_del_graph
            DeleteType.TRACKER -> R.string.ru_sure_del_feature
            DeleteType.FUNCTION -> R.string.ru_sure_del_function
        }

        ContinueCancelDialog(
            body = bodyRes,
            onDismissRequest = { groupDialogsViewModel.hideDeleteDialog() },
            onConfirm = {
                when (itemForDeletion.type) {
                    DeleteType.GROUP -> groupViewModel.onDeleteGroup(itemForDeletion.id)
                    DeleteType.GRAPH_STAT -> groupViewModel.onDeleteGraphStat(itemForDeletion.id)
                    DeleteType.TRACKER -> groupViewModel.onDeleteFeature(itemForDeletion.id)
                    DeleteType.FUNCTION -> groupViewModel.onDeleteFunction(itemForDeletion.id)
                }
                groupDialogsViewModel.hideDeleteDialog()
            },
            continueText = R.string.delete,
            cancelText = R.string.cancel
        )
    }

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

    val showDurationInputDialog = groupViewModel.showDurationInputDialog.collectAsStateWithLifecycle().value
    LaunchedEffect(showDurationInputDialog) {
        if (showDurationInputDialog == null) return@LaunchedEffect

        addDataPointsDialogViewModel.showAddDataPointDialog(
            trackerId = showDurationInputDialog.trackerId,
            customInitialValue = showDurationInputDialog.duration.seconds.toDouble()
        )
        groupViewModel.onConsumedShowDurationInputDialog()
    }

    // Release notes dialog
    if (showReleaseNotesDialog) {
        ReleaseNotesDialog(
            releaseNotes = releaseNotes,
            onDismissRequest = releaseNotesViewModel::onDismissReleaseNotesButton,
            onDonateClicked = {
                releaseNotesViewModel.onDonateClicked()
                releaseNotesViewModel.onDismissReleaseNotesButton()
            },
            onSkipDonationClicked = releaseNotesViewModel::onDismissReleaseNotesButton
        )
    }
}

/**
 * Pure UI component for GroupScreen that can be previewed without ViewModel dependencies.
 */
@Composable
private fun GroupScreenView(
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
    onDragStart: () -> Unit = {},
    onDragSwap: (Int, Int) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Group grid with items
        GroupGrid(
            modifier = Modifier.fillMaxSize(),
            lazyGridState = lazyGridState,
            allChildren = allChildren,
            trackerClickListeners = trackerClickListeners ?: TrackerClickListeners(),
            graphStatClickListeners = graphStatClickListeners ?: GraphStatClickListeners(),
            groupClickListeners = groupClickListeners ?: GroupClickListeners(),
            functionClickListeners = functionClickListeners ?: FunctionClickListeners(),
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

        // Extract animation specs for reuse
        val fabEnterAnimation = scaleIn(
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
        val fabExitAnimation = scaleOut(
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))

        // FAB positioned manually at bottom end
        AnimatedVisibility(
            visible = showFab,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fabEnterAnimation,
            exit = fabExitAnimation
        ) {
            FloatingActionButton(
                onClick = onQueueAddAllClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("trackAllFab")
                    .padding(WindowInsets.navigationBars.asPaddingValues())
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
            enter = fabEnterAnimation,
            exit = fabExitAnimation
        ) {
            FloatingBarButton(
                onClick = onReleaseNotesClicked,
                text = stringResource(id = R.string.see_whats_new),
                icon = R.drawable.deployed_code_update_24px,
                modifier = Modifier
                    .testTag("releaseNotesButton")
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .widthIn(max = 300.dp)
                    .then(Modifier.padding(inputSpacingLarge))
            )
        }

        // Loading overlay
        if (isLoading) LoadingOverlay()
    }
}

private sealed class GroupChildKey {
    @Parcelize
    data class Group(val groupId: Long) : GroupChildKey(), Parcelable

    @Parcelize
    data class GraphStat(val graphStatId: Long) : GroupChildKey(), Parcelable

    @Parcelize
    data class Tracker(val trackerId: Long) : GroupChildKey(), Parcelable

    @Parcelize
    data class Function(val functionId: Long) : GroupChildKey(), Parcelable
}

private fun GroupChild.toGroupChildKey(): GroupChildKey = when (this) {
    is GroupChild.ChildGraph -> GroupChildKey.GraphStat(id)
    is GroupChild.ChildGroup -> GroupChildKey.Group(id)
    is GroupChild.ChildTracker -> GroupChildKey.Tracker(id)
    is GroupChild.ChildFunction -> GroupChildKey.Function(id)
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
    onDragStart: () -> Unit,
    onDragSwap: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    // Calculate column count based on maxWidth with minimum 100.dp per cell
    val columnCount = (maxWidth / 180.dp).toInt().coerceAtLeast(2)

    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onDragSwap(from.index, to.index)
    }

    LaunchedEffect(reorderableLazyGridState.isAnyItemDragging) {
        if (reorderableLazyGridState.isAnyItemDragging) onDragStart() else onDragEnd()
    }

    var lastSize by remember { mutableIntStateOf(allChildren.size) }
    LaunchedEffect(allChildren.size) {
        if (allChildren.size > lastSize) {
            lazyGridState.animateScrollToItem(0)
        }
        lastSize = allChildren.size
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
            key = { it.toGroupChildKey() },
            span = { item ->
                when (item) {
                    is GroupChild.ChildTracker -> GridItemSpan(1)
                    is GroupChild.ChildFunction -> GridItemSpan(1)
                    is GroupChild.ChildGroup -> GridItemSpan(2)
                    is GroupChild.ChildGraph -> GridItemSpan(columnCount)
                }
            }
        ) { item ->
            ReorderableItem(
                reorderableLazyGridState,
                key = item.toGroupChildKey()
            ) { isDragging ->
                when (item) {
                    is GroupChild.ChildTracker -> {
                        TrackerItem(
                            tracker = item.displayTracker,
                            clickListeners = trackerClickListeners,
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildFunction -> {
                        FunctionItem(
                            displayFunction = item.displayFunction,
                            clickListeners = functionClickListeners,
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildGroup -> {
                        GroupItem(
                            group = item.group,
                            clickListeners = groupClickListeners,
                            isElevated = isDragging,
                        )
                    }

                    is GroupChild.ChildGraph -> {
                        GraphStatItem(
                            graphStat = item.graph.viewData,
                            clickListeners = graphStatClickListeners,
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
    isElevated: Boolean,
) = Tracker(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    tracker = tracker,
    onEdit = { clickListeners.onEdit(it) },
    onDelete = { clickListeners.onDelete(it) },
    onMoveTo = { clickListeners.onMoveTo(it) },
    onDescription = { clickListeners.onDescription(it) },
    onAdd = { t, useDefault -> clickListeners.onAdd(t, useDefault) },
    onHistory = { clickListeners.onHistory(it) },
    onPlayTimer = { clickListeners.onPlayTimer(it) },
    onStopTimer = { clickListeners.onStopTimer(it) }
)

@Composable
private fun ReorderableCollectionItemScope.GroupItem(
    group: Group,
    clickListeners: GroupClickListeners,
    isElevated: Boolean,
) = Group(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    group = group,
    onEdit = { clickListeners.onEdit(it) },
    onDelete = { clickListeners.onDelete(it) },
    onMoveTo = { clickListeners.onMove(it) },
    onClick = { clickListeners.onClick(it) }
)

@Composable
private fun ReorderableCollectionItemScope.GraphStatItem(
    graphStat: IGraphStatViewData,
    clickListeners: GraphStatClickListeners,
    isElevated: Boolean,
) = GraphStatCardView(
    modifier = Modifier.longPressDraggableHandle(),
    isElevated = isElevated,
    graphStatViewData = graphStat,
    clickListener = GraphStatClickListener(
        onEdit = { clickListeners.onEdit(it) },
        onDelete = { clickListeners.onDelete(it) },
        onClick = { clickListeners.onClick(it) },
        onMove = { clickListeners.onMove(it) },
        onDuplicate = { clickListeners.onDuplicate(it) },
    )
)

@Composable
private fun ReorderableCollectionItemScope.FunctionItem(
    displayFunction: DisplayFunction,
    clickListeners: FunctionClickListeners,
    isElevated: Boolean,
) = Function(
    modifier = Modifier.longPressDraggableHandle(),
    displayFunction = displayFunction,
    isElevated = isElevated,
    onClick = { clickListeners.onClick(displayFunction) },
    onEdit = { clickListeners.onEdit(displayFunction) },
    onDelete = { clickListeners.onDelete(displayFunction) },
    onMoveTo = { clickListeners.onMove(displayFunction) },
    onDuplicate = { clickListeners.onDuplicate(displayFunction) }
)

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