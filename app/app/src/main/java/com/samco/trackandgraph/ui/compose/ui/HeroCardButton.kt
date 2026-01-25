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

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun HeroCardButton(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.tngColors.heroCardButtonBackgroundColor,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        shadowElevation = if (isPressed) 0.dp else cardElevation,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.tngColors.onBackground,
            )
            HalfDialogInputSpacing()
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.tngColors.textColorSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeroCardButtonPreview() {
    DialogTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            HeroCardButton(
                modifier = Modifier.fillMaxWidth(),
                title = "Week Day Reminder",
                description = "Set reminders for specific days of the week",
                onClick = {}
            )
            DialogInputSpacing()
            HeroCardButton(
                modifier = Modifier.fillMaxWidth(),
                title = "Periodic Reminder",
                description = "Repeat at regular intervals",
                onClick = {}
            )
        }
    }
}
