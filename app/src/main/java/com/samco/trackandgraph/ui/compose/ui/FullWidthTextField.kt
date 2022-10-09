package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FullWidthTextField(
    value: String,
    onValueChange: (String) -> Unit,
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
        if (singleLine) KeyboardOptions(imeAction = ImeAction.Next)
        else KeyboardOptions.Default

    OutlinedTextField(
        value = textField.value,
        label = { Text(text = label) },
        onValueChange = {
            textField.value = it
            onValueChange(it.text)
        },
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        modifier = Modifier
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

