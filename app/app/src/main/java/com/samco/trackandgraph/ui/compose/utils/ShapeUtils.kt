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

package com.samco.trackandgraph.ui.compose.utils

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Scales a Shape by the given scale factor.
 * Currently supports RoundedCornerShape with proper corner radius scaling.
 * Other shape types are returned unchanged.
 *
 * @param shape The shape to scale
 * @param scale The scale factor to apply (e.g., 0.55f for 55% size)
 * @return A new scaled Shape, or the original shape if scaling is not supported
 */
@Composable
fun scaleShape(shape: Shape, scale: Float): Shape {
    val density = LocalDensity.current
    
    return remember(shape, scale) {
        when (shape) {
            is RoundedCornerShape -> {
                // Extract actual corner sizes and scale them
                val topStartPx = shape.topStart.toPx(Size.Unspecified, density)
                val topEndPx = shape.topEnd.toPx(Size.Unspecified, density)
                val bottomStartPx = shape.bottomStart.toPx(Size.Unspecified, density)
                val bottomEndPx = shape.bottomEnd.toPx(Size.Unspecified, density)
                
                RoundedCornerShape(
                    topStart = CornerSize((topStartPx * scale / density.density).dp),
                    topEnd = CornerSize((topEndPx * scale / density.density).dp),
                    bottomStart = CornerSize((bottomStartPx * scale / density.density).dp),
                    bottomEnd = CornerSize((bottomEndPx * scale / density.density).dp)
                )
            }
            else -> shape // Fallback for other shape types
        }
    }
}
