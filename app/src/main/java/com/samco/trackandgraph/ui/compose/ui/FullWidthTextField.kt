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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue


//TODO remove this
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FullWidthTextFieldLegacy(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    focusManager: FocusManager? = null,
    focusRequester: FocusRequester? = null,
    keyboardController: SoftwareKeyboardController? = null,
    singleLine: Boolean = true
) {
    val textField = remember(value) {
        mutableStateOf(
            TextFieldValue(
                value,
                TextRange(value.length, value.length)
            )
        )
    }

    val keyboardActions =
        if (focusManager != null) KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Down)
        }) else KeyboardActions.Default

    val keyboardOptions =
        if (singleLine) KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences
        ) else KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences
        )

    OutlinedTextField(
        value = textField.value,
        label = { Text(text = label) },
        onValueChange = {
            if (textField.value != it) textField.value = it
            onValueChange(it.text)
        },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FullWidthTextField(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    focusManager: FocusManager? = null,
    focusRequester: FocusRequester? = null,
    keyboardController: SoftwareKeyboardController? = null,
    singleLine: Boolean = true
) {
    val keyboardActions =
        if (focusManager != null) KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Down)
        }) else KeyboardActions.Default

    val keyboardOptions =
        if (singleLine) KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences
        ) else KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences
        )

    OutlinedTextField(
        value = textFieldValue,
        label = { Text(text = label) },
        onValueChange = { onValueChange(it) },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
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

