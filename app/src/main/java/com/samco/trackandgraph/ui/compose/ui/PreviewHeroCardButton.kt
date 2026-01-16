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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.group.Tracker
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

private const val DEFAULT_PREVIEW_SCALE = 0.6f
private val DEFAULT_PREVIEW_MEASURED_WIDTH = 220.dp

@Composable
fun PreviewHeroCardButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.tngColors.heroCardButtonBackgroundColor,
    previewScale: Float = DEFAULT_PREVIEW_SCALE,
    previewMeasuredWidth: Dp = DEFAULT_PREVIEW_MEASURED_WIDTH,
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
        color = backgroundColor
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
                        scale = previewScale,
                        measuredWidth = previewMeasuredWidth,
                    )
            ) {
                preview()
            }

            // Text label
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.tngColors.onSurface
            )
        }
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

        // Measure "normally", but with optional fixed width/height.
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

@Preview(showBackground = true)
@Composable
private fun PreviewCardButtonPreviewHero() {
    TnGComposeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            PreviewHeroCardButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Create Tracker",
                onClick = {}
            ) {
                Tracker(
                    tracker = DisplayTracker(
                        id = 1L,
                        featureId = 1L,
                        name = "Tracker",
                        groupId = 1L,
                        dataType = DataType.CONTINUOUS,
                        hasDefaultValue = false,
                        defaultValue = 0.0,
                        defaultLabel = "",
                        timestamp = null,
                        displayIndex = 0,
                        description = "",
                        timerStartInstant = null
                    ),
                    onEdit = {},
                    onDelete = {},
                    onMoveTo = {},
                    onDescription = {},
                    onAdd = { _, _ -> },
                    onHistory = {},
                    onPlayTimer = {},
                    onStopTimer = {},
                )

            }
        }
    }
}
