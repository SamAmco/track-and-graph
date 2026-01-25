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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun FullWidthTextField(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String = "",
    focusManager: FocusManager? = null,
    focusRequester: FocusRequester? = null,
    keyboardController: SoftwareKeyboardController? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val keyboardActions =
        if (focusManager != null) KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Down)
        }) else KeyboardActions.Default

    val keyboardOptions1 = when {
        keyboardOptions != null -> keyboardOptions
        singleLine -> KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences
        )

        else -> KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences
        )
    }

    OutlinedTextField(
        value = textFieldValue,
        label = { Text(text = label) },
        onValueChange = { onValueChange(it) },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions1,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .slimOutlinedTextField()
            .wrapContentHeight()
            .onFocusChanged {
                if (it.hasFocus) keyboardController?.show()
            }
            .let {
                if (focusRequester != null) it.focusRequester(focusRequester)
                else it
            }
    )
}

