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

package com.samco.trackandgraph.addgroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ColorSpinner
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge

@Composable
fun AddGroupDialog(viewModel: AddGroupDialogViewModel, onDismissRequest: () -> Unit = {}) {
    if (viewModel.hidden) return

    val addEnabled by viewModel.addEnabled.collectAsStateWithLifecycle()
    val updateMode by viewModel.updateMode.collectAsStateWithLifecycle()

    CustomContinueCancelDialog(
        onDismissRequest = { onDismissRequest() },
        onConfirm = {
            viewModel.addOrUpdateGroup()
            onDismissRequest()
        },
        continueText = if (updateMode) R.string.update else R.string.add,
        dismissText = R.string.cancel,
        continueEnabled = addEnabled,
    ) {
        AddGroupView(
            modifier = Modifier.fillMaxWidth(),
            colorIndex = viewModel.colorIndex,
            name = viewModel.name,
            onColorIndexChange = viewModel::updateColorIndex,
            onNameChange = viewModel::updateName
        )
    }
}

@Composable
private fun AddGroupView(
    modifier: Modifier,
    colorIndex: Int,
    name: TextFieldValue,
    onColorIndexChange: (Int) -> Unit,
    onNameChange: (TextFieldValue) -> Unit
) = Column(modifier) {
    Text(
        text = stringResource(id = R.string.add_group),
        style = MaterialTheme.typography.h6
    )

    InputSpacingLarge()

    val focusRequester = remember { FocusRequester() }

    Row(modifier = Modifier.fillMaxWidth()) {
        ColorSpinner(
            selectedColor = colorIndex,
            onColorSelected = onColorIndexChange,
        )

        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            value = name,
            onValueChange = onNameChange
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
private fun AddGroupViewPreview() = TnGComposeTheme {
    AddGroupView(
        modifier = Modifier.fillMaxWidth(),
        colorIndex = 2,
        name = TextFieldValue("Sample Group Name"),
        onColorIndexChange = {},
        onNameChange = {}
    )
}
