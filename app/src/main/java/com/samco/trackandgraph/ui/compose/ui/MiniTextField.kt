@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.disabledAlpha
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MiniTextField(
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
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) = BasicTextField(
    value = textFieldValue,
    onValueChange = {
        if (charLimit == null || it.text.length <= charLimit) onValueChange(it)
    },
    textStyle = MaterialTheme.typography.h5.copy(
        textAlign = textAlign,
        color = MaterialTheme.tngColors.onSurface
    ),
    cursorBrush = SolidColor(MaterialTheme.tngColors.primary),
    interactionSource = interactionSource,
    decorationBox = {
        if (textFieldValue.text == "") Text(
            "0",
            style = MaterialTheme.typography.h5,
            textAlign = textAlign,
            color = MaterialTheme.tngColors.onSurface.copy(
                alpha = MaterialTheme.tngColors.disabledAlpha
            )
        )
        else it()
    },
    keyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(
                overrideFocusDirection ?: FocusDirection.Right
            )
        }
    ),
    singleLine = true,
    modifier = modifier
        .width(IntrinsicSize.Min)
        .padding(start = 4.dp)
        .widthIn(min = 40.dp, max = 40.dp)
        .indicatorLine(
            enabled = true,
            isError = false,
            interactionSource = interactionSource,
            colors = colors
        )
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                focusUpdateScope.launch {
                    delay(20)
                    onValueChange(
                        textFieldValue.copy(
                            selection = TextRange(0, textFieldValue.text.length)
                        )
                    )
                }
            }
        }
        .let {
            if (focusRequester != null) it.focusRequester(focusRequester)
            else it
        },
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    )
)