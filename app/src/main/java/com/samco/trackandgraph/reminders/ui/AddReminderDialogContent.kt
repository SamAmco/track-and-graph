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

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.ui.compose.animation.popTransitionSpec
import com.samco.trackandgraph.ui.compose.animation.predictivePopTransitionSpec
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
    onConfirm: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    onSetCleanup: (() -> Unit) -> Unit = {},
    hasAnyFeatures: Boolean = false,
) {
    val navBackStack = rememberNavBackStack(ReminderDialogNavKey.ReminderTypeSelection)

    NavDisplay(
        modifier = modifier.animateContentSize(),
        backStack = navBackStack,
        onBack = { navBackStack.removeLastOrNull() },
        transitionSpec = transitionSpec(),
        popTransitionSpec = popTransitionSpec(),
        predictivePopTransitionSpec = predictivePopTransitionSpec(),
        entryProvider = { navKey ->
            when (navKey) {
                is ReminderDialogNavKey.ReminderTypeSelection -> NavEntry(navKey) {
                    ReminderTypeSelectionScreen(
                        onWeekDayReminderSelected = {
                            navBackStack.add(ReminderDialogNavKey.WeekDayReminderConfiguration)
                        },
                        onPeriodicReminderSelected = {
                            navBackStack.add(ReminderDialogNavKey.PeriodicReminderConfiguration)
                        },
                        onMonthDayReminderSelected = {
                            navBackStack.add(ReminderDialogNavKey.MonthDayReminderConfiguration)
                        },
                        onTimeSinceLastReminderSelected = {
                            navBackStack.add(ReminderDialogNavKey.TimeSinceLastReminderConfiguration)
                        },
                        onDismiss = onDismiss,
                        hasAnyFeatures = hasAnyFeatures
                    )
                }
                is ReminderDialogNavKey.WeekDayReminderConfiguration -> NavEntry(navKey) {
                    WeekDayReminderConfigurationScreen(
                        onUpsertReminder = onConfirm,
                        onDismiss = onDismiss,
                        onSetCleanup = onSetCleanup
                    )
                }
                is ReminderDialogNavKey.PeriodicReminderConfiguration -> NavEntry(navKey) {
                    PeriodicReminderConfigurationScreen(
                        onUpsertReminder = onConfirm,
                        onDismiss = onDismiss,
                        onSetCleanup = onSetCleanup
                    )
                }
                is ReminderDialogNavKey.MonthDayReminderConfiguration -> NavEntry(navKey) {
                    MonthDayReminderConfigurationScreen(
                        onUpsertReminder = onConfirm,
                        onDismiss = onDismiss,
                        onSetCleanup = onSetCleanup
                    )
                }
                is ReminderDialogNavKey.TimeSinceLastReminderConfiguration -> NavEntry(navKey) {
                    TimeSinceLastReminderConfigurationScreen(
                        onUpsertReminder = onConfirm,
                        onDismiss = onDismiss,
                        onSetCleanup = onSetCleanup
                    )
                }
                else -> error("Unknown navKey: $navKey")
            }
        }
    )
}
