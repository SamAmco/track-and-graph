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
package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

enum class RowCheckboxPosition {
    Start,
    End,
}

@Composable
fun RowCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    text: String,
    textStyle: TextStyle = LocalTextStyle.current,
    checkboxPosition: RowCheckboxPosition = RowCheckboxPosition.Start,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .clickable(enabled = onCheckedChange != null) { onCheckedChange?.invoke(!checked) }
        .padding(end = if (checkboxPosition == RowCheckboxPosition.Start) 14.dp else 0.dp)
) {
    when (checkboxPosition) {
        RowCheckboxPosition.Start -> {
            Checkbox(
                checked = checked,
                enabled = onCheckedChange != null,
                onCheckedChange = onCheckedChange,
            )
            Text(
                text = text,
                style = textStyle,
            )
        }

        RowCheckboxPosition.End -> {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = textStyle,
            )
            InputSpacingLarge()
            Checkbox(
                checked = checked,
                enabled = onCheckedChange != null,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RowCheckboxPreview() {
    TnGComposeTheme {
        var checked by remember { mutableStateOf(true) }

        Column {
            RowCheckbox(
                checked = checked,
                onCheckedChange = { checked = it },
                text = "Start checkbox option"
            )

            DialogInputSpacing()

            RowCheckbox(
                modifier = Modifier.fillMaxWidth(),
                checked = checked,
                onCheckedChange = { checked = it },
                text = "End checkbox option",
                checkboxPosition = RowCheckboxPosition.End,
            )

            DialogInputSpacing()

            RowCheckbox(
                checked = false,
                onCheckedChange = {},
                text = "Unchecked checkbox option"
            )

            DialogInputSpacing()

            RowCheckbox(
                checked = true,
                onCheckedChange = null,
                text = "Disabled checkbox option"
            )
        }
    }
}
