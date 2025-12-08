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

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.serialization.Serializable

sealed class ReminderDialogNavKey : NavKey {
    @Serializable
    object ReminderTypeSelection : ReminderDialogNavKey()
    @Serializable
    object ReminderConfiguration : ReminderDialogNavKey()
}

@Composable
fun AddReminderDialogContent(
    modifier: Modifier = Modifier,
    onConfirm: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) {
    val navBackStack = rememberNavBackStack(ReminderDialogNavKey.ReminderTypeSelection)
    
    NavDisplay(
        modifier = modifier,
        backStack = navBackStack,
        onBack = { navBackStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(tween(300)) { it } + fadeIn() togetherWith
                    slideOutHorizontally(tween(300)) { -it } + fadeOut()
        },
        popTransitionSpec = {
            slideInHorizontally(tween(300)) { -it } + fadeIn() togetherWith
                    slideOutHorizontally(tween(300)) { it } + fadeOut()
        },
        entryProvider = { navKey ->
            when (navKey) {
                is ReminderDialogNavKey.ReminderTypeSelection -> NavEntry(navKey) {
                    ReminderTypeSelectionScreen(
                        onReminderTypeSelected = {
                            navBackStack.add(ReminderDialogNavKey.ReminderConfiguration)
                        },
                        onDismiss = onDismiss
                    )
                }
                is ReminderDialogNavKey.ReminderConfiguration -> NavEntry(navKey) {
                    WeekDayReminderConfigurationScreen(
                        onUpsertReminder = onConfirm,
                        onDismiss = onDismiss
                    )
                }
                else -> error("Unknown navKey: $navKey")
            }
        }
    )
}

@Composable
fun ReminderTypeSelectionScreen(
    onReminderTypeSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Select Reminder Type",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        
        DialogInputSpacing()
        
        // Currently only one reminder type available
        SelectorButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Week Day Reminder",
            onClick = onReminderTypeSelected
        )
        
        DialogInputSpacing()
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallTextButton(
                stringRes = R.string.cancel,
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ReminderTypeSelectionScreenPreview() {
    TnGComposeTheme {
        ReminderTypeSelectionScreen(
            onReminderTypeSelected = {},
            onDismiss = {}
        )
    }
}