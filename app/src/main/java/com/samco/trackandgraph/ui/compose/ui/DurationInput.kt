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
@file:OptIn(ExperimentalMaterialApi::class)

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.disabledAlpha
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl


@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
fun DurationInputPreview() = DurationInput(
    modifier = Modifier,
    viewModel = DurationInputViewModelImpl()
)

@Composable
fun DurationInput(
    modifier: Modifier = Modifier,
    viewModel: DurationInputViewModel,
    focusManager: FocusManager? = null,
    nextFocusDirection: FocusDirection? = null
) = Row(
    modifier = modifier
        .padding(
            horizontal = dimensionResource(id = R.dimen.card_padding),
            vertical = dimensionResource(id = R.dimen.card_padding)
        ),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.Center
) {
    val focusManager = focusManager ?: LocalFocusManager.current
    val hours = viewModel.hours.observeAsState("")
    val minutes = viewModel.minutes.observeAsState("")
    val seconds = viewModel.seconds.observeAsState("")
    DurationInputComponent(
        value = hours.value,
        onValueChange = { viewModel.setHours(it) },
        suffix = stringResource(id = R.string.hours_suffix),
        charLimit = 8,
        focusManager
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
    )
    DurationInputComponent(
        value = minutes.value,
        onValueChange = { viewModel.setMinutes(it) },
        suffix = stringResource(id = R.string.minutes_suffix),
        charLimit = 3,
        focusManager
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
    )
    DurationInputComponent(
        value = seconds.value,
        onValueChange = { viewModel.setSeconds(it) },
        suffix = stringResource(id = R.string.seconds_suffix),
        charLimit = 3,
        focusManager,
        overrideFocusDirection = nextFocusDirection
    )
}


@Composable
private fun DurationInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    charLimit: Int,
    focusManager: FocusManager,
    overrideFocusDirection: FocusDirection? = null
) {
    val colors = TextFieldDefaults.textFieldColors()
    val interactionSource = remember { MutableInteractionSource() }
    val textField = remember(value) {
        mutableStateOf(
            TextFieldValue(
                value,
                TextRange(value.length, value.length)
            )
        )
    }

    BasicTextField(
        value = textField.value,
        onValueChange = {
            if (it.text != textField.value.text && it.text.length <= charLimit) {
                textField.value = it
                onValueChange.invoke(it.text)
            }
        },
        textStyle = MaterialTheme.typography.body2.copy(
            textAlign = TextAlign.End,
            color = MaterialTheme.colors.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        interactionSource = interactionSource,
        decorationBox = {
            if (textField.value.text == "") Text(
                "0",
                fontSize = MaterialTheme.typography.body2.fontSize,
                textAlign = TextAlign.End,
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = MaterialTheme.colors.disabledAlpha
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
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .widthIn(min = 40.dp, max = 80.dp)
            .indicatorLine(
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = colors
            )
            .onFocusChanged { focusState ->
                val textLength = textField.value.text.length
                if (focusState.isFocused) {
                    textField.value = textField.value.copy(
                        selection = TextRange(0, textLength)
                    )
                } else {
                    textField.value = textField.value.copy(
                        selection = TextRange(textLength, textLength)
                    )
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
    )
    Text(
        text = suffix,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

