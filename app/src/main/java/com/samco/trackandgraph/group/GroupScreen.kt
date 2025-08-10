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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.addgroup.AddGroupDialog
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.importexport.ExportFeaturesDialog
import com.samco.trackandgraph.importexport.ImportFeaturesDialog
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItem
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.ContinueDialog
import com.samco.trackandgraph.ui.compose.ui.EmptyPageHintText
import com.samco.trackandgraph.ui.compose.ui.FeatureInfoDialog
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.inputSpacingXLarge

/**
 * Compose screen for GroupFragment containing FAB, RecyclerView (in AndroidView),
 * empty state text, loading overlay, and dialogs.
 */
@Composable
fun GroupScreen(
    recyclerView: RecyclerView,
    groupViewModel: GroupViewModel,
    groupDialogsViewModel: GroupDialogsViewModel,
    moveItemViewModel: MoveItemViewModel,
    addDataPointsDialogViewModel: AddDataPointsViewModelImpl,
    addGroupDialogViewModel: AddGroupDialogViewModelImpl,
    groupId: Long,
    groupName: String?,
    showFab: Boolean,
    onQueueAddAllClicked: () -> Unit
) {
    val isLoading = groupViewModel.loading.collectAsStateWithLifecycle().value
    val showEmptyText = groupViewModel.showEmptyGroupText.collectAsStateWithLifecycle().value
    val hasTrackers = groupViewModel.hasTrackers.collectAsStateWithLifecycle().value

    GroupScreenView(
        recyclerView = recyclerView,
        isLoading = isLoading,
        showEmptyText = showEmptyText,
        showFab = showFab && hasTrackers, // Only show FAB if we have trackers AND not hidden by scroll
        onQueueAddAllClicked = onQueueAddAllClicked
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
            onItemSelected = {
                val groupItem = it as? SelectableItem.Group ?: return@SelectItemDialog
                moveItemViewModel.moveItemToGroup(groupItem.id)
            },
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
        }

        ContinueCancelDialog(
            body = bodyRes,
            onDismissRequest = { groupDialogsViewModel.hideDeleteDialog() },
            onConfirm = {
                when (itemForDeletion.type) {
                    DeleteType.GROUP -> groupViewModel.onDeleteGroup(itemForDeletion.id)
                    DeleteType.GRAPH_STAT -> groupViewModel.onDeleteGraphStat(itemForDeletion.id)
                    DeleteType.TRACKER -> groupViewModel.onDeleteFeature(itemForDeletion.id)
                }
                groupDialogsViewModel.hideDeleteDialog()
            },
            continueText = R.string.delete,
            dismissText = R.string.cancel
        )
    }

    if (groupDialogsViewModel.showNoTrackersDialog.collectAsStateWithLifecycle().value) {
        ContinueDialog(
            onConfirm = { groupDialogsViewModel.hideNoTrackersDialog() },
            onDismissRequest = { groupDialogsViewModel.hideNoTrackersDialog() },
            continueText = R.string.ok
        ) {
            Text(
                text = stringResource(id = R.string.no_trackers_graph_stats_hint),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

/**
 * Pure UI component for GroupScreen that can be previewed without ViewModel dependencies.
 */
@Composable
private fun GroupScreenView(
    recyclerView: RecyclerView?,
    isLoading: Boolean,
    showEmptyText: Boolean,
    showFab: Boolean,
    onQueueAddAllClicked: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // RecyclerView wrapped in AndroidView (only if provided)
        recyclerView?.let { rv ->
            AndroidView(
                factory = { rv },
                modifier = Modifier.fillMaxSize()
            )
        }

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
            enter = scaleIn(
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            FloatingActionButton(
                onClick = onQueueAddAllClicked,
                backgroundColor = MaterialTheme.colors.primary,
                modifier = Modifier.padding(inputSpacingLarge)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.queue_add_box),
                    contentDescription = stringResource(id = R.string.track_all)
                )
            }
        }

        // Loading overlay
        if (isLoading) LoadingOverlay()
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupScreenViewEmptyPreview() {
    TnGComposeTheme {
        GroupScreenView(
            recyclerView = null,
            isLoading = false,
            showEmptyText = true,
            showFab = true,
            onQueueAddAllClicked = { }
        )
    }
}
