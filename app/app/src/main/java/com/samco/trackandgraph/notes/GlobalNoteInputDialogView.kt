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

package com.samco.trackandgraph.notes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.settings.mockSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import kotlinx.coroutines.delay
import org.threeten.bp.OffsetDateTime

@Composable
fun GlobalNoteInputDialogView(viewModel: GlobalNoteInputViewModel) {
    val updateMode by viewModel.updateMode.observeAsState(false)
    val addButtonEnabled by viewModel.addButtonEnabled.observeAsState(false)
    val selectedDateTime by viewModel.dateTime.observeAsState(OffsetDateTime.now())

    GlobalNoteInputDialog(
        note = viewModel.note,
        onNoteChange = viewModel::updateNoteText,
        selectedDateTime = selectedDateTime,
        onDateTimeSelected = viewModel::updateTimeStamp,
        updateMode = updateMode,
        addButtonEnabled = addButtonEnabled,
        onDismissRequest = viewModel::onCancelClicked,
        onConfirm = viewModel::onAddClicked
    )

    if (viewModel.showConfirmCancelDialog.observeAsState(false).value) {
        ContinueCancelDialog(
            onDismissRequest = viewModel::onCancelDismissed,
            onConfirm = viewModel::onCancelConfirmed,
            body = R.string.confirm_cancel_notes_will_be_lost
        )
    }
}

@Composable
private fun GlobalNoteInputDialog(
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    selectedDateTime: OffsetDateTime,
    onDateTimeSelected: (OffsetDateTime) -> Unit,
    updateMode: Boolean,
    addButtonEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) = CustomContinueCancelDialog(
    onDismissRequest = onDismissRequest,
    dismissOnClickOutside = false,
    onConfirm = onConfirm,
    continueText = if (updateMode) R.string.update else R.string.add,
    continueEnabled = addButtonEnabled
) {
    val focusRequester = remember { FocusRequester() }

    InputSpacingLarge()

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.add_a_global_note),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )

    InputSpacingLarge()

    DateTimeButtonRow(
        modifier = Modifier.fillMaxWidth(),
        selectedDateTime = selectedDateTime,
        onDateTimeSelected = onDateTimeSelected
    )

    DialogInputSpacing()

    FullWidthTextField(
        modifier = Modifier.heightIn(max = 200.dp),
        textFieldValue = note,
        onValueChange = onNoteChange,
        focusRequester = focusRequester,
        label = stringResource(id = R.string.note_input_hint),
        singleLine = false
    )

    LaunchedEffect(focusRequester) {
        delay(10L)
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
fun PreviewGlobalNoteInputDialog() {
    CompositionLocalProvider(LocalSettings provides mockSettings) {
        TnGComposeTheme {
            val note = remember { mutableStateOf(TextFieldValue("Hello")) }
            val selectedDateTime = OffsetDateTime.parse("2023-06-15T14:30:00+01:00")
            val updateMode = false
            val addButtonEnabled = true

            GlobalNoteInputDialog(
                note = note.value,
                onNoteChange = { note.value = it },
                selectedDateTime = selectedDateTime,
                onDateTimeSelected = {},
                updateMode = updateMode,
                addButtonEnabled = addButtonEnabled,
                onDismissRequest = {},
                onConfirm = {}
            )
        }
    }
}
