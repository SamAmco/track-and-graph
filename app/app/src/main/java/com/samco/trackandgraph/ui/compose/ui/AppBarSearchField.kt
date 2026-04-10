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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

/**
 * Underlined, transparent-container text field suitable for use inside a top app bar's title
 * slot. Built on Material3 [TextField] so it inherits the app theme (colors, typography,
 * cursor, indicator). The only customisation is making the container transparent so it blends
 * into the app bar background.
 *
 * When [autoFocus] is true (the default) the field requests focus on first composition, which
 * also opens the software keyboard — appropriate for a search field that appears in response
 * to a user tap.
 *
 * The IME action is [ImeAction.Done] rather than [ImeAction.Search] because search-as-you-type
 * is performed automatically; the enter key only needs to dismiss the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarSearchField(
    textFieldState: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    TextField(
        state = textFieldState,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        textStyle = MaterialTheme.typography.titleMedium,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            // App-bar search has no visible indicator line — the bar itself frames the field.
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Preview(showBackground = true, name = "Empty (placeholder)")
@Composable
private fun AppBarSearchFieldEmptyPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.tngColors.toolbarBackgroundColor),
        ) {
            AppBarSearchField(
                textFieldState = TextFieldState(),
                placeholder = "Search",
                autoFocus = false,
            )
        }
    }
}

@Preview(showBackground = true, name = "With query")
@Composable
private fun AppBarSearchFieldWithQueryPreview() {
    TnGComposeTheme {
        var query by remember { mutableStateOf("weight") }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.tngColors.toolbarBackgroundColor),
        ) {
            AppBarSearchField(
                textFieldState = TextFieldState(),
                placeholder = "Search",
                autoFocus = false,
            )
        }
    }
}
