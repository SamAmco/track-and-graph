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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun DurationInputPreview() {
    DurationInputView(
        modifier = Modifier,
        hours = TextFieldValue("1"),
        minutes = TextFieldValue("30"),
        seconds = TextFieldValue("45"),
        onHoursChanged = {},
        onMinutesChanged = {},
        onSecondsChanged = {}
    )
}

// Legacy ViewModel-based component for backward compatibility
@Composable
fun DurationInput(
    modifier: Modifier = Modifier,
    viewModel: DurationInputViewModel,
    focusManager: FocusManager = LocalFocusManager.current,
    nextFocusDirection: FocusDirection? = null,
    focusRequester: FocusRequester? = null
) = DurationInputView(
    modifier = modifier,
    hours = viewModel.hours,
    minutes = viewModel.minutes,
    seconds = viewModel.seconds,
    onHoursChanged = { viewModel.setHoursText(it) },
    onMinutesChanged = { viewModel.setMinutesText(it) },
    onSecondsChanged = { viewModel.setSecondsText(it) },
    focusManager = focusManager,
    nextFocusDirection = nextFocusDirection,
    focusRequester = focusRequester
)

// Pure UI component that doesn't depend on ViewModel
@Composable
fun DurationInputView(
    modifier: Modifier = Modifier,
    hours: TextFieldValue,
    minutes: TextFieldValue,
    seconds: TextFieldValue,
    onHoursChanged: (TextFieldValue) -> Unit,
    onMinutesChanged: (TextFieldValue) -> Unit,
    onSecondsChanged: (TextFieldValue) -> Unit,
    focusManager: FocusManager = LocalFocusManager.current,
    nextFocusDirection: FocusDirection? = null,
    focusRequester: FocusRequester? = null
) = Row(
    modifier = modifier
        .padding(
            horizontal = cardPadding,
            vertical = cardPadding
        ),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.Center
) {
    DurationInputComponent(
        textFieldValue = hours,
        onValueChange = onHoursChanged,
        suffix = stringResource(id = R.string.hours_suffix),
        charLimit = 8,
        focusManager = focusManager,
        focusRequester = focusRequester,
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier.alignByBaseline()
    )
    DurationInputComponent(
        textFieldValue = minutes,
        onValueChange = onMinutesChanged,
        suffix = stringResource(id = R.string.minutes_suffix),
        charLimit = 3,
        focusManager = focusManager
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier.alignByBaseline()
    )
    DurationInputComponent(
        textFieldValue = seconds,
        onValueChange = onSecondsChanged,
        suffix = stringResource(id = R.string.seconds_suffix),
        charLimit = 3,
        focusManager = focusManager,
        overrideFocusDirection = nextFocusDirection
    )
}


@Composable
private fun RowScope.DurationInputComponent(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    suffix: String,
    charLimit: Int,
    focusManager: FocusManager,
    overrideFocusDirection: FocusDirection? = null,
    focusRequester: FocusRequester? = null
) {
    MiniNumericTextField(
        modifier = Modifier
            .alignByBaseline()
            .weight(1f, fill = false),
        textFieldValue = textFieldValue,
        onValueChange = onValueChange,
        charLimit = charLimit,
        focusManager = focusManager,
        overrideFocusDirection = overrideFocusDirection,
        focusRequester = focusRequester
    )

    //Align baseline to above text field
    Text(
        text = suffix,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .alignByBaseline()
    )
}

