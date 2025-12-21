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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import org.threeten.bp.LocalDateTime

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
            text = "Week Day Reminder",
            onClick = onReminderTypeSelected
        ) {
            Reminder(
                reminderViewData = ReminderViewData.WeekDayReminderViewData(
                    id = 1L,
                    displayIndex = 0,
                    name = "Reminder",
                    nextScheduled = LocalDateTime.of(2025, 12, 20, 7, 30),
                    checkedDays = CheckedDays(
                        monday = true,
                        tuesday = true,
                        wednesday = true,
                        thursday = true,
                        friday = true,
                        saturday = false,
                        sunday = false
                    ),
                    reminderDto = null,
                )
            )
        }

        DialogInputSpacing()

        ContinueCancelButtons(
            cancelVisible = true,
            continueVisible = false,
            cancelText = R.string.cancel,
            onCancel = onDismiss
        )
    }
}

fun Modifier.layoutScaled(
    scale: Float,
    measuredWidth: Dp? = null,
    measuredHeight: Dp? = null
) = this.then(
    Modifier.layout { measurable, parentConstraints ->

        val measuredWidthPx = measuredWidth?.roundToPx()
        val measuredHeightPx = measuredHeight?.roundToPx()

        // Measure “normally”, but with optional fixed width/height.
        val measureConstraints = Constraints(
            minWidth = measuredWidthPx ?: parentConstraints.minWidth,
            maxWidth = measuredWidthPx ?: parentConstraints.maxWidth,
            minHeight = measuredHeightPx ?: parentConstraints.minHeight,
            maxHeight = measuredHeightPx ?: parentConstraints.maxHeight,
        )

        val placeable = measurable.measure(measureConstraints)

        // CEIL instead of floor to avoid clipping at small scales
        val scaledWidth = (placeable.width * scale).toInt()
        val scaledHeight = (placeable.height * scale).toInt()

        // Optional: 1px guard against AA/stroke rounding
        val finalWidth = scaledWidth.coerceAtMost(parentConstraints.maxWidth)
        val finalHeight = scaledHeight.coerceAtMost(parentConstraints.maxHeight)

        layout(finalWidth, finalHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
)

private const val PREVIEW_SCALE = 0.6f
private val PREVIEW_MEASURED_WIDTH = 220.dp

@Composable
fun ReminderTypeButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
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
                .padding(cardPadding),
            horizontalArrangement = Arrangement.spacedBy(halfDialogInputSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scaled down preview
            Column(
                modifier = Modifier
                    .layoutScaled(
                        scale = PREVIEW_SCALE,
                        measuredWidth = PREVIEW_MEASURED_WIDTH,
                    )
            ) {
                preview()
            }
            
            // Text label
            Text(
                modifier = modifier.weight(1f),
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.tngColors.onSurface
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
