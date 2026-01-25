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
package com.samco.trackandgraph.ui.compose.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyScreenText(
    @StringRes textId: Int,
    vararg formatArgs: Any
) = EmptyScreenText(stringResource(textId, *formatArgs))

@Composable
fun EmptyScreenText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    alpha: Float = 0.6f
) = Box {
    Text(
        modifier = Modifier
            .width(250.dp)
            .wrapContentHeight()
            .align(Alignment.Center),
        text = text,
        fontSize = MaterialTheme.typography.titleSmall.fontSize,
        fontWeight = MaterialTheme.typography.titleSmall.fontWeight,
        textAlign = TextAlign.Center,
        color = color.copy(alpha = alpha)
    )
}

