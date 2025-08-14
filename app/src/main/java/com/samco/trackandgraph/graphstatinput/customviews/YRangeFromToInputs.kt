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

package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.YRangeConfigBehaviour
import com.samco.trackandgraph.ui.compose.ui.DurationInput
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
fun YRangeFromToInputs(
    viewModel: YRangeConfigBehaviour,
    fromEnabled: Boolean = true
) {
    val toText =
        if (fromEnabled) stringResource(id = R.string.to)
        else stringResource(id = R.string.y_range_max)
    val timeBased = viewModel.timeBasedRange.collectAsState(initial = false).value
    if (timeBased) TimeBasedRangeInputs(viewModel, fromEnabled, toText)
    else NumericRangeInputs(viewModel, fromEnabled, toText)
}

@Composable
private fun TimeBasedRangeInputs(
    viewModel: YRangeConfigBehaviour,
    fromEnabled: Boolean,
    toText: String
) = Column(
    modifier = Modifier
        .padding(horizontal = cardPadding)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {

    if (fromEnabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.from),
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.width(dialogInputSpacing))

            DurationInput(
                viewModel = viewModel.yRangeFromDurationViewModel,
                nextFocusDirection = FocusDirection.Down,
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = toText,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.width(dialogInputSpacing))

        DurationInput(viewModel = viewModel.yRangeToDurationViewModel)
    }
}

@Composable
private fun NumericRangeInputs(
    viewModel: YRangeConfigBehaviour,
    fromEnabled: Boolean,
    toText: String
) = Row(
    modifier = Modifier
        .padding(horizontal = cardPadding)
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    if (fromEnabled) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = stringResource(id = R.string.from),
            style = MaterialTheme.typography.titleSmall
        )

        MiniNumericTextField(
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
            textAlign = TextAlign.Center,
            textFieldValue = viewModel.yRangeFrom,
            onValueChange = { viewModel.updateYRangeFrom(it) }
        )
    }

    Text(
        modifier = Modifier.alignByBaseline(),
        text = toText,
        style = MaterialTheme.typography.titleSmall
    )

    MiniNumericTextField(
        modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        textAlign = TextAlign.Center,
        textFieldValue = viewModel.yRangeTo,
        onValueChange = { viewModel.updateYRangeTo(it) }
    )
}