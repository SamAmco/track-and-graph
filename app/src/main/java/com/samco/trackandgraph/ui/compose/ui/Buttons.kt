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

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun FilledButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(text = text)
    }
}

@Composable
fun WideButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) = FilledButton(
    text = text,
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    enabled = enabled
)


@Composable
fun SmallTextButton(
    @StringRes stringRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    colors = colors,
    interactionSource = interactionSource,
) {
    Text(stringResource(stringRes))
}

@Composable
fun SelectorTextButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    shape = MaterialTheme.shapes.small,
    contentPadding = PaddingValues(8.dp),
    colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.tngColors.selectorButtonColor
    )
) {
    Text(
        text = text,
        fontWeight = MaterialTheme.typography.subtitle2.fontWeight,
        fontSize = MaterialTheme.typography.subtitle2.fontSize,
    )
}

@Composable
fun AddBarButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = Button(
    modifier = modifier.widthIn(min = 160.dp),
    onClick = onClick,
    shape = MaterialTheme.shapes.small
) {
    Icon(
        painter = painterResource(id = R.drawable.add_icon),
        contentDescription = stringResource(id = R.string.add)
    )
}