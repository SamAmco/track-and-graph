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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniNumericTextField(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    charLimit: Int? = null,
    textAlign: TextAlign = TextAlign.End,
    focusManager: FocusManager = LocalFocusManager.current,
    overrideFocusDirection: FocusDirection? = null,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusUpdateScope: CoroutineScope = rememberCoroutineScope(),
    colors: TextFieldColors = TextFieldDefaults.colors(),
    enabled: Boolean = true
) {
    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            if (charLimit == null || it.text.length <= charLimit) onValueChange(it)
        },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            textAlign = textAlign,
            color = MaterialTheme.tngColors.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.tngColors.primary),
        enabled = enabled,
        interactionSource = interactionSource,
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(
                    overrideFocusDirection ?: FocusDirection.Right
                )
            }
        ),
        singleLine = true,
        modifier = modifier
            .wrapContentWidth()
            .widthIn(min = 40.dp, max = 80.dp)
            .padding(start = 4.dp)
            .indicatorLine(
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = colors
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    focusUpdateScope.launch {
                        onValueChange(
                            textFieldValue.copy(
                                selection = TextRange(0, textFieldValue.text.length)
                            )
                        )
                    }
                } else if (textFieldValue.text.isEmpty()) {
                    onValueChange(TextFieldValue(text = "0"))
                }
            }
            .let {
                if (focusRequester != null) it.focusRequester(focusRequester)
                else it
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        decorationBox = {
            TextFieldDefaults.DecorationBox(
                value = textFieldValue.text,
                innerTextField = it,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                // Had a bug here where the duration input nested in a lazy row
                // would cause the scroll state to slowly drift when focused.
                // Somehow adding a content padding of 1dp fixed it.
                contentPadding = PaddingValues(horizontal = 1.dp),
                colors = colors,
                container = {},
            )
        }
    )
}