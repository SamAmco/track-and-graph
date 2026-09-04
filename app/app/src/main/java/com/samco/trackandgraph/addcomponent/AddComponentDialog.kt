/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.addcomponent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.samco.trackandgraph.R
import com.samco.trackandgraph.addgroup.AddGroupDialogContent
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.reminders.ui.AddReminderDialogContent
import com.samco.trackandgraph.reminders.ui.AddReminderViewModelImpl
import com.samco.trackandgraph.selectitemdialog.SelectItemDialogContent
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.animation.popTransitionSpec
import com.samco.trackandgraph.ui.compose.animation.predictivePopTransitionSpec
import com.samco.trackandgraph.ui.compose.animation.navSizeTransform
import com.samco.trackandgraph.ui.compose.animation.transitionSpec
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.ui.ContinueDialog
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.HeroCardButton
import com.samco.trackandgraph.ui.ui.cardElevation
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import kotlinx.serialization.Serializable

private sealed class AddComponentDialogNavKey : NavKey {
    @Serializable data object ComponentTypeSelection : AddComponentDialogNavKey()
    @Serializable data object AddGroup : AddComponentDialogNavKey()
    @Serializable data object AddReminder : AddComponentDialogNavKey()
    @Serializable data object AddSymlink : AddComponentDialogNavKey()
}

@Composable
fun AddComponentDialog(
    visible: Boolean,
    groupId: Long,
    onDismiss: () -> Unit,
    onAddTracker: (Long) -> Unit,
    onAddGraphOrStat: (Long) -> Unit,
    onAddFunction: (Long) -> Unit,
) {
    val navBackStack = rememberNavBackStack()

    LaunchedEffect(visible) {
        when {
            visible && navBackStack.isEmpty() -> {
                navBackStack.add(AddComponentDialogNavKey.ComponentTypeSelection)
            }
            !visible && navBackStack.isNotEmpty() -> navBackStack.clear()
        }
    }

    val dismiss: () -> Unit = {
        navBackStack.clear()
        onDismiss()
    }
    val navigateBack: () -> Unit = {
        if (navBackStack.size > 1) {
            navBackStack.removeLastOrNull()
        } else {
            dismiss()
        }
    }
    fun finishAdding(onAdd: (Long) -> Unit): (Long) -> Unit = { targetGroupId ->
        dismiss()
        onAdd(targetGroupId)
    }
    val destinationActions = AddComponentDestinationActions(
        onAddTracker = finishAdding(onAddTracker),
        onAddGraphOrStat = finishAdding(onAddGraphOrStat),
        onAddGroup = { navBackStack.add(AddComponentDialogNavKey.AddGroup) },
        onAddFunction = finishAdding(onAddFunction),
        onAddReminder = { navBackStack.add(AddComponentDialogNavKey.AddReminder) },
        onAddSymlink = { navBackStack.add(AddComponentDialogNavKey.AddSymlink) },
        onDismiss = dismiss,
        onNavigateBack = navigateBack,
    )

    // Keep decorated entries composed even while the Dialog window is hidden. Clearing the
    // back stack can then invoke each decorator's onPop and release its destination ViewModels.
    val entries = rememberDecoratedNavEntries(
        backStack = navBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { navKey ->
            addComponentDialogEntry(navKey, groupId, destinationActions)
        },
    )

    if (visible && entries.isNotEmpty()) {
        CustomDialog(
            onDismissRequest = dismiss,
            scrollContent = false,
            supportSmoothHeightAnimation = true,
            paddingValues = PaddingValues(
                start = inputSpacingLarge,
                end = inputSpacingLarge,
                bottom = halfDialogInputSpacing,
                top = inputSpacingLarge,
            )
        ) {
            NavDisplay(
                entries = entries,
                contentAlignment = Alignment.Center,
                onBack = navigateBack,
                sizeTransform = navSizeTransform(),
                transitionSpec = transitionSpec(),
                popTransitionSpec = popTransitionSpec(),
                predictivePopTransitionSpec = predictivePopTransitionSpec(),
            )
        }
    }
}

private data class AddComponentDestinationActions(
    val onAddTracker: (Long) -> Unit,
    val onAddGraphOrStat: (Long) -> Unit,
    val onAddGroup: () -> Unit,
    val onAddFunction: (Long) -> Unit,
    val onAddReminder: () -> Unit,
    val onAddSymlink: () -> Unit,
    val onDismiss: () -> Unit,
    val onNavigateBack: () -> Unit,
)

private fun addComponentDialogEntry(
    navKey: NavKey,
    groupId: Long,
    actions: AddComponentDestinationActions,
): NavEntry<NavKey> = when (navKey) {
    AddComponentDialogNavKey.ComponentTypeSelection -> NavEntry(navKey) {
        ComponentTypeSelectionDestination(
            groupId = groupId,
            onAddTracker = actions.onAddTracker,
            onAddGraphOrStat = actions.onAddGraphOrStat,
            onAddGroup = actions.onAddGroup,
            onAddFunction = actions.onAddFunction,
            onAddReminder = actions.onAddReminder,
            onAddSymlink = actions.onAddSymlink,
            onDismiss = actions.onDismiss,
        )
    }
    AddComponentDialogNavKey.AddGroup -> NavEntry(navKey) {
        AddGroupDestination(groupId = groupId, onDismiss = actions.onDismiss)
    }
    AddComponentDialogNavKey.AddReminder -> NavEntry(navKey) {
        AddReminderDestination(
            groupId = groupId,
            onDismiss = actions.onDismiss,
            onNavigateBackFromTypeSelection = actions.onNavigateBack,
        )
    }
    AddComponentDialogNavKey.AddSymlink -> NavEntry(navKey) {
        AddSymlinkDestination(groupId = groupId, onDismiss = actions.onDismiss)
    }
    else -> error("Unknown navKey: $navKey")
}

@Composable
private fun ComponentTypeSelectionDestination(
    groupId: Long,
    onAddTracker: (Long) -> Unit,
    onAddGraphOrStat: (Long) -> Unit,
    onAddGroup: () -> Unit,
    onAddFunction: (Long) -> Unit,
    onAddReminder: () -> Unit,
    onAddSymlink: () -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<AddComponentDialogViewModelImpl>()
    val warning by viewModel.warning.collectAsStateWithLifecycle()

    ComponentTypeSelectionScreen(
        onAddTracker = { onAddTracker(groupId) },
        onAddGraphOrStat = { viewModel.addGraphOrStat(groupId, onAddGraphOrStat) },
        onAddGroup = onAddGroup,
        onAddFunction = { viewModel.addFunction(groupId, onAddFunction) },
        onAddReminder = onAddReminder,
        onAddSymlink = onAddSymlink,
        onDismiss = onDismiss,
    )

    warning?.let {
        ContinueDialog(
            body = when (it) {
                AddComponentWarning.GRAPH_REQUIRES_TRACKER -> R.string.no_trackers_graph_stats_hint
                AddComponentWarning.FUNCTION_REQUIRES_TRACKER -> R.string.no_trackers_functions_hint
            },
            onConfirm = viewModel::dismissWarning,
            onDismissRequest = viewModel::dismissWarning,
            continueText = R.string.ok,
        )
    }
}

@Composable
private fun AddGroupDestination(
    groupId: Long,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<AddGroupDialogViewModelImpl>()
    LaunchedEffect(groupId) { viewModel.initializeForCreate(groupId) }

    AddGroupDialogContent(
        viewModel = viewModel,
        onDismissRequest = onDismiss,
        onConfirm = { viewModel.addOrUpdateGroup(onComplete = onDismiss) },
    )
}

@Composable
private fun AddReminderDestination(
    groupId: Long,
    onDismiss: () -> Unit,
    onNavigateBackFromTypeSelection: () -> Unit,
) {
    val viewModel = hiltViewModel<AddReminderViewModelImpl>()
    LaunchedEffect(groupId) { viewModel.loadStateForReminder(null, groupId) }
    LaunchedEffect(viewModel.onComplete) {
        for (event in viewModel.onComplete) onDismiss()
    }

    AddReminderDialogContent(
        onConfirm = viewModel::saveReminder,
        onDismiss = onDismiss,
        onNavigateBackFromTypeSelection = onNavigateBackFromTypeSelection,
        hasAnyFeatures = viewModel.hasAnyFeatures.collectAsStateWithLifecycle().value,
    )
}

@Composable
private fun AddSymlinkDestination(
    groupId: Long,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<AddSymlinkViewModelImpl>()
    val disabledItems by viewModel.disabledItems.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) { viewModel.initialize(groupId) }

    fun createSymlink(childId: Long, childType: GroupChildType) {
        viewModel.createSymlink(
            inGroupId = groupId,
            childId = childId,
            childType = childType,
            onComplete = onDismiss,
        )
    }

    SelectItemDialogContent(
        title = stringResource(R.string.symlink),
        selectableTypes = setOf(
            SelectableItemType.GROUP,
            SelectableItemType.TRACKER,
            SelectableItemType.GRAPH,
            SelectableItemType.FUNCTION,
        ),
        disabledItems = disabledItems,
        onGroupSelected = { createSymlink(it, GroupChildType.GROUP) },
        onTrackerSelected = { createSymlink(it, GroupChildType.TRACKER) },
        onGraphSelected = { createSymlink(it, GroupChildType.GRAPH) },
        onFunctionSelected = { createSymlink(it, GroupChildType.FUNCTION) },
        onDismissRequest = onDismiss,
        dismissAfterSelection = false,
        selectionEnabled = !saving,
    )
}

@Composable
private fun ComponentTypeSelectionScreen(
    onAddTracker: () -> Unit,
    onAddGraphOrStat: () -> Unit,
    onAddGroup: () -> Unit,
    onAddFunction: () -> Unit,
    onAddReminder: () -> Unit,
    onAddSymlink: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = cardElevation),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.add_component),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface,
        )
        DialogInputSpacing()
        ComponentHeroCard(R.string.tracker, R.string.add_tracker_description, onAddTracker)
        DialogInputSpacing()
        ComponentHeroCard(R.string.graph_or_stat, R.string.add_graph_or_stat_description, onAddGraphOrStat)
        DialogInputSpacing()
        ComponentHeroCard(R.string.group, R.string.add_group_description, onAddGroup)
        DialogInputSpacing()
        ComponentHeroCard(R.string.function, R.string.add_function_description, onAddFunction)
        DialogInputSpacing()
        ComponentHeroCard(R.string.reminder, R.string.add_reminder_description, onAddReminder)
        DialogInputSpacing()
        ComponentHeroCard(R.string.symlink, R.string.add_symlink_description, onAddSymlink)
        DialogInputSpacing()
        ContinueCancelButtons(
            cancelVisible = true,
            continueVisible = false,
            cancelText = R.string.cancel,
            onCancel = onDismiss,
        )
    }
}

@Composable
private fun ComponentHeroCard(title: Int, description: Int, onClick: () -> Unit) {
    HeroCardButton(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(title),
        description = stringResource(description),
        onClick = onClick,
    )
}
