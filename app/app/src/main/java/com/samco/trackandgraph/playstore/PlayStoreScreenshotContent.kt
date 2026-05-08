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
package com.samco.trackandgraph.playstore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.functions.FunctionsScreenContent
import com.samco.trackandgraph.group.FunctionClickListeners
import com.samco.trackandgraph.group.GraphStatClickListeners
import com.samco.trackandgraph.group.GroupClickListeners
import com.samco.trackandgraph.group.GroupScreenView
import com.samco.trackandgraph.group.TrackerClickListeners
import com.samco.trackandgraph.reminders.ui.RemindersScreen
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

private const val PLAY_STORE_DEVICE = "spec:width=1080px,height=2340px,dpi=420"

@Preview(name = "Play Store 1 - Daily", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreDailyGroupPreview() {
    PlayStoreDailyGroupScreenshotContent()
}

@Preview(name = "Play Store 2 - Exercise", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreExercisePreview() {
    PlayStoreExerciseScreenshotContent()
}

@Preview(name = "Play Store 3 - Daily add Stress", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreDailyAddStressPreview() {
    PlayStoreDailyAddStressScreenshotContent()
}

@Preview(name = "Play Store 4 - Daily dark", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreDailyDarkPreview() {
    PlayStoreDailyDarkScreenshotContent()
}

@Preview(name = "Play Store 5 - Rest day statistics", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreRestDayStatisticsPreview() {
    PlayStoreRestDayStatisticsScreenshotContent()
}

@Preview(name = "Play Store 6 - Groups list", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreGroupsListPreview() {
    PlayStoreGroupsListScreenshotContent()
}

@Preview(name = "Play Store 7 - Reminders", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreRemindersPreview() {
    PlayStoreRemindersScreenshotContent()
}

@Preview(name = "Play Store 8 - Function editor", device = PLAY_STORE_DEVICE)
@Composable
internal fun PlayStoreFunctionEditorPreview() {
    PlayStoreFunctionEditorScreenshotContent()
}

@Composable
internal fun PlayStoreDailyGroupScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            PlayStoreDailyGroup()
        }
    }
}

@Composable
internal fun PlayStoreExerciseScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            PlayStoreGroupFrame(title = "Exercise") {
                GroupScreenView(
                    lazyGridState = rememberLazyGridState(),
                    isLoading = false,
                    showEmptyText = false,
                    showFab = false,
                    showReleaseNotesButton = false,
                    allChildren = playStoreExerciseChildren(),
                    trackerClickListeners = TrackerClickListeners(),
                    graphStatClickListeners = GraphStatClickListeners(),
                    groupClickListeners = GroupClickListeners(),
                    functionClickListeners = FunctionClickListeners(),
                    fabInsetPaddingOverride = PaddingValues(bottom = PlayStoreNavigationBarHeight)
                )
            }
        }
    }
}

@Composable
internal fun PlayStoreDailyAddStressScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayStoreDailyGroup()
                PlayStoreAddStressDialogOverlay()
            }
        }
    }
}

@Composable
internal fun PlayStoreDailyDarkScreenshotContent() {
    TnGComposeTheme(darkTheme = true) {
        PlayStorePreviewEnvironment {
            PlayStoreDailyGroup()
        }
    }
}

@Composable
internal fun PlayStoreRestDayStatisticsScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            PlayStoreGroupFrame(title = "Rest day statistics") {
                GroupScreenView(
                    lazyGridState = rememberLazyGridState(),
                    isLoading = false,
                    showEmptyText = false,
                    showFab = false,
                    showReleaseNotesButton = false,
                    allChildren = playStoreRestDayStatisticsChildren(),
                    trackerClickListeners = TrackerClickListeners(),
                    graphStatClickListeners = GraphStatClickListeners(),
                    groupClickListeners = GroupClickListeners(),
                    functionClickListeners = FunctionClickListeners(),
                    fabInsetPaddingOverride = PaddingValues(bottom = PlayStoreNavigationBarHeight)
                )
            }
        }
    }
}

@Composable
internal fun PlayStoreGroupsListScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            PlayStoreGroupFrame(
                title = "Track & Graph",
                backNavigationAction = false,
            ) {
                GroupScreenView(
                    lazyGridState = rememberLazyGridState(),
                    isLoading = false,
                    showEmptyText = false,
                    showFab = false,
                    showReleaseNotesButton = false,
                    allChildren = playStoreGroupsListChildren(),
                    trackerClickListeners = TrackerClickListeners(),
                    graphStatClickListeners = GraphStatClickListeners(),
                    groupClickListeners = GroupClickListeners(),
                    functionClickListeners = FunctionClickListeners(),
                    fabInsetPaddingOverride = PaddingValues(bottom = PlayStoreNavigationBarHeight)
                )
            }
        }
    }
}

@Composable
internal fun PlayStoreRemindersScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            PlayStoreRemindersFrame {
                RemindersScreen(
                    reminders = playStoreReminders(),
                    isLoading = false,
                    showAddReminderDialog = false,
                    editReminderId = null,
                    lazyListState = LazyListState(),
                    onEditReminder = {},
                    onHideAddReminderDialog = {},
                    onDeleteReminder = {},
                    onDuplicateReminder = {},
                    onDragStart = {},
                    onDragSwap = { _, _ -> },
                    onDragEnd = {},
                )
            }
        }
    }
}

@Composable
internal fun PlayStoreFunctionEditorScreenshotContent() {
    TnGComposeTheme {
        PlayStorePreviewEnvironment {
            val state = remember { PlayStoreFunctionEditorState() }

            FunctionsScreenContent(
                onPopBack = {},
                nodes = state.nodes,
                hints = state.hints,
                onAddNode = { _, _ -> },
                onDragNodeBy = { _, _ -> },
                onDeleteNode = {},
                getWorldPosition = state::getWorldPosition,
                onRegisterNodeBounds = { _, _ -> },
                edges = state.edges,
                selectedEdge = state.selectedEdge,
                onSelectEdge = {},
                onDeleteSelectedEdge = {},
                connectors = state.connectors,
                draggingConnectorId = state.draggingConnector,
                onUpsertConnector = state::onUpsertConnector,
                onDownOnConnector = {},
                onDropConnector = {},
                getConnectorWorldPosition = state::getConnectorWorldPosition,
                isConnectorEnabled = { true },
                onCreateOrUpdateFunction = {},
                onUpdateScriptForNodeId = { _, _ -> },
                onUpdateScriptFromFileForNodeId = { _, _ -> },
                showFirstTimeUserDialog = false,
                onDismissFirstTimeUserDialog = {},
                onOpenFunctionsTutorial = {},
                overlayPadding = PaddingValues(
                    top = PlayStoreStatusBarHeight,
                ),
            )
        }
    }
}

@Composable
internal fun PlayStoreDailyGroup() {
    PlayStoreGroupFrame(title = "Daily") {
        GroupScreenView(
            lazyGridState = rememberLazyGridState(),
            isLoading = false,
            showEmptyText = false,
            showFab = true,
            showReleaseNotesButton = false,
            allChildren = playStoreDailyChildren(),
            trackerClickListeners = TrackerClickListeners(),
            graphStatClickListeners = GraphStatClickListeners(),
            groupClickListeners = GroupClickListeners(),
            functionClickListeners = FunctionClickListeners(),
            fabInsetPaddingOverride = PaddingValues(bottom = PlayStoreNavigationBarHeight)
        )
    }
}
