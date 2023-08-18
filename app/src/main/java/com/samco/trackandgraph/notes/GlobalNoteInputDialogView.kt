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
@file:OptIn(ExperimentalComposeUiApi::class)

package com.samco.trackandgraph.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.ui.*
import kotlinx.coroutines.delay
import org.threeten.bp.OffsetDateTime

@Composable
fun GlobalNoteInputDialogView(viewModel: GlobalNoteInputViewModel) {
    DialogTheme {
        Dialog(
            onDismissRequest = { viewModel.onCancelConfirmed() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            )
        ) {
            GlobalNoteDialogViewContent(viewModel)
            if (viewModel.showConfirmCancelDialog.observeAsState(false).value) {
                ConfirmCancelDialog(
                    onDismissRequest = viewModel::onCancelDismissed,
                    onConfirm = viewModel::onCancelConfirmed,
                    body = R.string.confirm_cancel_notes_will_be_lost
                )
            }
        }
    }
}

@Composable
private fun GlobalNoteDialogViewContent(viewModel: GlobalNoteInputViewModel) = Surface {
    Column(
        modifier = Modifier
            .heightIn(max = 400.dp)
            .fillMaxWidth(0.9f)
            .padding(dimensionResource(id = R.dimen.card_padding))
    ) {
        val selectedDateTime by viewModel.dateTime.observeAsState(OffsetDateTime.now())

        val focusRequester = remember { FocusRequester() }

        SpacingLarge()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.add_a_global_note),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center
        )

        SpacingLarge()

        DateTimeButtonRow(
            modifier = Modifier.fillMaxWidth(),
            selectedDateTime = selectedDateTime,
            onDateTimeSelected = { viewModel.updateTimeStamp(it) }
        )

        SpacingSmall()

        FullWidthTextField(
            modifier = Modifier.heightIn(max = 200.dp),
            textFieldValue = viewModel.note,
            onValueChange = { viewModel.updateNoteText(it) },
            focusRequester = focusRequester,
            label = stringResource(id = R.string.note_input_hint),
            singleLine = false
        )

        SpacingSmall()

        AddCancelBottomButtons(
            updateMode = viewModel.updateMode.observeAsState(false).value,
            onCancelClicked = viewModel::onCancelClicked,
            onAddClicked = viewModel::onAddClicked,
            addButtonEnabled = viewModel.addButtonEnabled.observeAsState(false).value
        )

        LaunchedEffect(focusRequester) {
            delay(10L)
            focusRequester.requestFocus()
        }

    }
}
