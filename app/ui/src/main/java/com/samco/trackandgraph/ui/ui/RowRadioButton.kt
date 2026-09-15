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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph. If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

@Composable
fun RowRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
) = Row(
    modifier = modifier
        .selectable(
            selected = selected,
            enabled = onClick != null,
            role = Role.RadioButton,
            onClick = { onClick?.invoke() },
        )
        .padding(end = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    RadioButton(
        selected = selected,
        enabled = onClick != null,
        onClick = null,
    )
    Text(
        text = text,
        style = textStyle,
    )
}

@Preview(showBackground = true)
@Composable
private fun RowRadioButtonPreview() {
    TnGComposeTheme {
        var selectedOption by remember { mutableStateOf(0) }

        Column {
            RowRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = selectedOption == 0,
                onClick = { selectedOption = 0 },
                text = "First radio option",
            )

            RowRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = selectedOption == 1,
                onClick = { selectedOption = 1 },
                text = "Second radio option",
            )

            RowRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = false,
                onClick = null,
                text = "Disabled radio option",
            )
        }
    }
}
