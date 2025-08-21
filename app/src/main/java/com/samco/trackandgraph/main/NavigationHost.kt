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
package com.samco.trackandgraph.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.samco.trackandgraph.aboutpage.AboutNavKey
import com.samco.trackandgraph.aboutpage.AboutScreen
import com.samco.trackandgraph.addtracker.AddTrackerNavKey
import com.samco.trackandgraph.addtracker.AddTrackerScreen
import com.samco.trackandgraph.backupandrestore.BackupAndRestoreNavKey
import com.samco.trackandgraph.backupandrestore.BackupAndRestoreScreen
import com.samco.trackandgraph.featurehistory.FeatureHistoryNavKey
import com.samco.trackandgraph.featurehistory.FeatureHistoryScreen
import com.samco.trackandgraph.graphstatinput.GraphStatInputNavKey
import com.samco.trackandgraph.graphstatinput.GraphStatInputScreen
import com.samco.trackandgraph.group.GroupNavKey
import com.samco.trackandgraph.group.GroupScreen
import com.samco.trackandgraph.notes.NotesNavKey
import com.samco.trackandgraph.notes.NotesScreen
import com.samco.trackandgraph.reminders.RemindersNavKey
import com.samco.trackandgraph.reminders.RemindersScreen
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.viewgraphstat.ViewGraphStatNavKey
import com.samco.trackandgraph.viewgraphstat.ViewGraphStatScreen

/**
 * POC Navigation implementation using Navigation 3 - demonstrates pattern with NotesScreen only
 */
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    backStack: NavBackStack,
    urlNavigator: UrlNavigator,
) = NavDisplay(
    modifier = modifier,
    entryDecorators = listOf(
        // Add the default decorators for managing scenes and saving state
        rememberSceneSetupNavEntryDecorator(),
        rememberSavedStateNavEntryDecorator(),
        // Then add the view model store decorator
        rememberViewModelStoreNavEntryDecorator()
    ),
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = { destination ->
        when (destination) {
            is GroupNavKey -> NavEntry(destination) {
                GroupScreen(
                    navArgs = destination,
                    onTrackerEdit = { tracker ->
                        backStack.add(AddTrackerNavKey(groupId = destination.groupId, editTrackerId = tracker.id))
                    },
                    onGraphStatEdit = { graphStat ->
                        backStack.add(GraphStatInputNavKey(graphStatId = graphStat.graphOrStat.id, groupId = destination.groupId))
                    },
                    onGraphStatClick = { graphStat ->
                        backStack.add(ViewGraphStatNavKey(graphStatId = graphStat.graphOrStat.id))
                    },
                    onGroupClick = { group ->
                        backStack.add(GroupNavKey(groupId = group.id, groupName = group.name))
                    },
                    onTrackerHistory = { tracker ->
                        backStack.add(FeatureHistoryNavKey(featureId = tracker.featureId, featureName = tracker.name))
                    },
                    onAddTracker = { groupId ->
                        backStack.add(AddTrackerNavKey(groupId = groupId))
                    },
                    onAddGraphStat = { groupId ->
                        backStack.add(GraphStatInputNavKey(groupId = groupId))
                    }
                )
            }

            is NotesNavKey -> NavEntry(destination) {
                NotesScreen(destination)
            }

            is RemindersNavKey -> NavEntry(destination) {
                RemindersScreen(destination)
            }

            is AboutNavKey -> NavEntry(destination) {
                AboutScreen(destination, urlNavigator)
            }

            is BackupAndRestoreNavKey -> NavEntry(destination) {
                BackupAndRestoreScreen(destination)
            }

            is ViewGraphStatNavKey -> NavEntry(destination) {
                ViewGraphStatScreen(destination)
            }

            is FeatureHistoryNavKey -> NavEntry(destination) {
                FeatureHistoryScreen(destination)
            }

            is AddTrackerNavKey -> NavEntry(destination) {
                AddTrackerScreen(
                    navArgs = destination,
                    onPopBack = { backStack.removeLastOrNull() }
                )
            }

            is GraphStatInputNavKey -> NavEntry(destination) {
                GraphStatInputScreen(
                    navArgs = destination,
                    urlNavigator = urlNavigator,
                    onPopBack = { backStack.removeLastOrNull() }
                )
            }

            else -> error("Unknown destination: $destination")
        }
    }
)
