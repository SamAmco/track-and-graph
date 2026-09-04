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

package com.samco.trackandgraph.reminders.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.ui.compose.animation.popTransitionSpec
import com.samco.trackandgraph.ui.compose.animation.predictivePopTransitionSpec
import com.samco.trackandgraph.ui.compose.animation.navSizeTransform
import com.samco.trackandgraph.ui.compose.animation.transitionSpec
import kotlinx.serialization.Serializable

sealed class ReminderDialogNavKey : NavKey {
    @Serializable
    object ReminderTypeSelection : ReminderDialogNavKey()
    @Serializable
    object WeekDayReminderConfiguration : ReminderDialogNavKey()
    @Serializable
    object PeriodicReminderConfiguration : ReminderDialogNavKey()
    @Serializable
    object MonthDayReminderConfiguration : ReminderDialogNavKey()
    @Serializable
    object TimeSinceLastReminderConfiguration : ReminderDialogNavKey()
}

@Composable
fun AddReminderDialogContent(
    modifier: Modifier = Modifier,
    onConfirm: (ReminderInput) -> Unit,
    onDismiss: () -> Unit,
    onNavigateBackFromTypeSelection: () -> Unit = onDismiss,
    hasAnyFeatures: Boolean = false,
) {
    val navBackStack = rememberNavBackStack(ReminderDialogNavKey.ReminderTypeSelection)

    val entries = rememberDecoratedNavEntries(
        backStack = navBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { navKey ->
            reminderDialogEntry(
                navKey = navKey,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                onNavigate = navBackStack::add,
                hasAnyFeatures = hasAnyFeatures,
            )
        },
    )

    NavDisplay(
        modifier = modifier,
        entries = entries,
        contentAlignment = Alignment.Center,
        onBack = {
            if (navBackStack.size > 1) {
                navBackStack.removeLastOrNull()
            } else {
                onNavigateBackFromTypeSelection()
            }
        },
        sizeTransform = navSizeTransform(),
        transitionSpec = transitionSpec(),
        popTransitionSpec = popTransitionSpec(),
        predictivePopTransitionSpec = predictivePopTransitionSpec(),
    )
}

private fun reminderDialogEntry(
    navKey: NavKey,
    onConfirm: (ReminderInput) -> Unit,
    onDismiss: () -> Unit,
    onNavigate: (ReminderDialogNavKey) -> Unit,
    hasAnyFeatures: Boolean,
): NavEntry<NavKey> = when (navKey) {
    is ReminderDialogNavKey.ReminderTypeSelection -> NavEntry(navKey) {
        ReminderTypeSelectionDestination(
            onNavigate = onNavigate,
            onDismiss = onDismiss,
            hasAnyFeatures = hasAnyFeatures,
        )
    }
    is ReminderDialogNavKey.WeekDayReminderConfiguration -> NavEntry(navKey) {
        WeekDayReminderConfigurationScreen(
            onUpsertReminder = onConfirm,
            onDismiss = onDismiss,
        )
    }
    is ReminderDialogNavKey.PeriodicReminderConfiguration -> NavEntry(navKey) {
        PeriodicReminderConfigurationScreen(
            onUpsertReminder = onConfirm,
            onDismiss = onDismiss,
        )
    }
    is ReminderDialogNavKey.MonthDayReminderConfiguration -> NavEntry(navKey) {
        MonthDayReminderConfigurationScreen(
            onUpsertReminder = onConfirm,
            onDismiss = onDismiss,
        )
    }
    is ReminderDialogNavKey.TimeSinceLastReminderConfiguration -> NavEntry(navKey) {
        TimeSinceLastReminderConfigurationScreen(
            onUpsertReminder = onConfirm,
            onDismiss = onDismiss,
        )
    }
    else -> error("Unknown navKey: $navKey")
}

@Composable
private fun ReminderTypeSelectionDestination(
    onNavigate: (ReminderDialogNavKey) -> Unit,
    onDismiss: () -> Unit,
    hasAnyFeatures: Boolean,
) {
    ReminderTypeSelectionScreen(
        onWeekDayReminderSelected = {
            onNavigate(ReminderDialogNavKey.WeekDayReminderConfiguration)
        },
        onPeriodicReminderSelected = {
            onNavigate(ReminderDialogNavKey.PeriodicReminderConfiguration)
        },
        onMonthDayReminderSelected = {
            onNavigate(ReminderDialogNavKey.MonthDayReminderConfiguration)
        },
        onTimeSinceLastReminderSelected = {
            onNavigate(ReminderDialogNavKey.TimeSinceLastReminderConfiguration)
        },
        onDismiss = onDismiss,
        hasAnyFeatures = hasAnyFeatures,
    )
}
