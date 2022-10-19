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
package com.samco.trackandgraph.ui.compose.theming

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.android.material.composethemeadapter.createMdcTheme


val Colors.disabledAlpha get() = 0.4f

@Composable
fun TnGComposeTheme(block: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = createMdcTheme(
        context = LocalContext.current,
        layoutDirection = LocalLayoutDirection.current
    )
    MaterialTheme(
        colors = colorScheme ?: MaterialTheme.colors,
        typography = typography ?: MaterialTheme.typography,
        shapes = shapes ?: MaterialTheme.shapes,
        content = block
    )
}
