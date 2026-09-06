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

package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.R
import com.samco.trackandgraph.ui.theming.DialogTheme
import com.samco.trackandgraph.ui.theming.tngColors

private val heroCardElevation = 6.dp
private val heroCardPressedElevation = 2.dp

@Composable
fun HeroCardButton(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color =
            if (isPressed) MaterialTheme.tngColors.surface
            else MaterialTheme.tngColors.surfaceBright,
        shadowElevation = if (isPressed) heroCardPressedElevation else heroCardElevation,
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

@Composable
fun IconHeroCardButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: Painter,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color =
            if (isPressed) MaterialTheme.tngColors.surface
            else MaterialTheme.tngColors.surfaceBright,
        shadowElevation = if (isPressed) heroCardPressedElevation else heroCardElevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.secondary.copy(alpha = 0f),
                        0.5f to MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                        1f to MaterialTheme.colorScheme.secondary,
                    ),
                ),
            ) {
                Box(
                    modifier = Modifier.size(buttonSize + dialogInputSpacing),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(largeIconSize),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.size(inputSpacingLarge))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.tngColors.onBackground,
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

@Preview(showBackground = true)
@Composable
private fun IconHeroCardButtonPreview() {
    DialogTheme(darkTheme = false) {
        IconHeroCardButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            title = "Tracker",
            icon = painterResource(R.drawable.add_icon),
            onClick = {},
        )
    }
}
