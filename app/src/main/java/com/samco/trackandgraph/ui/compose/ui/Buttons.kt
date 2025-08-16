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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
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
    colors = colors,
    interactionSource = interactionSource,
) {
    Text(stringResource(stringRes))
}

@Composable
fun SelectorButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) = SelectorButton(
    modifier = modifier,
    enabled = enabled,
    onClick = onClick
) {
    Text(
        text = text,
        fontWeight = MaterialTheme.typography.titleSmall.fontWeight,
        fontSize = MaterialTheme.typography.titleSmall.fontSize,
    )
}

@Composable
fun SelectorButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    shape = MaterialTheme.shapes.small,
    enabled = enabled,
    contentPadding = PaddingValues(
        horizontal = 8.dp,
        vertical = 6.dp
    ),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.tngColors.selectorButtonColor
    ),
    content = content,
)

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

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
) = Button(
    modifier = modifier,
    onClick = onClick,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
fun IconTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    text: String,
) = Button(
    modifier = modifier,
    onClick = onClick,
) {
    Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = icon),
        contentDescription = text,
        tint = MaterialTheme.colorScheme.onPrimary,
    )
    HalfDialogInputSpacing()
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge
    )
}

@Preview(showBackground = true)
@Composable
fun ButtonPreview() = TnGComposeTheme(
    darkTheme = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledButton(
            text = "Filled Button",
            onClick = {}
        )
        WideButton(
            text = "Wide Button",
            onClick = {}
        )
        SmallTextButton(
            stringRes = R.string.continue_word,
            onClick = {}
        )
        SelectorButton(
            modifier = Modifier,
            text = "Selector Text Button",
            onClick = {}
        )
        AddBarButton(
            onClick = {}
        )
        TextButton(
            text = "Text Button",
            onClick = {}
        )
        IconTextButton(
            icon = R.drawable.add_icon,
            text = "Some Text",
            onClick = {}
        )
    }
}