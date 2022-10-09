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

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R

@Composable
fun ConfirmCancelDialog(
    @StringRes body: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.small,
    text = {
        Text(text = stringResource(id = body))
    },
    confirmButton = {
        TextButton(
            onClick = onConfirm,
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(id = continueText))
        }
    },
    dismissButton = {
        TextButton(
            onClick = onDismissRequest,
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(id = dismissText))
        }
    }
)
