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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing

@Composable
fun ReminderTypeSelectionScreen(
    onReminderTypeSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        Text(
            text = "Select Reminder Type",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        // Currently only one reminder type available

        ReminderTypeButton(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Check,
            text = "Week Day Reminder",
            onClick = onReminderTypeSelected
        )

        DialogInputSpacing()

        ContinueCancelButtons(
            cancelVisible = true,
            continueVisible = false,
            cancelText = R.string.cancel,
            onCancel = onDismiss
        )
    }
}

@Composable
fun ReminderTypeButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.tngColors.outline
        ),
        color = MaterialTheme.tngColors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.tngColors.onSurface
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.tngColors.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderTypeButtonPreview() {
    TnGComposeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderTypeButton(
                icon = Icons.Default.Check,
                text = "Week Day Reminder",
                onClick = {}
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
