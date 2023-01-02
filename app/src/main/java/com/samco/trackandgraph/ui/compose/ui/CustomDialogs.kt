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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    scrollContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) = Dialog(
    onDismissRequest = onDismissRequest
) {
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_padding))
                .apply {
                    if (scrollContent) verticalScroll(state = rememberScrollState())
                },
            content = content
        )
    }
}

@Composable
fun SlimConfirmCancelDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel,
    continueEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    scrollContent = false
) {
    Column(
        Modifier
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())
    ) {
        content()

        SpacingSmall()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallTextButton(
                stringRes = dismissText,
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
            SmallTextButton(
                stringRes = continueText,
                onClick = onConfirm,
                enabled = continueEnabled
            )
        }
    }
}

@Composable
fun ConfirmCancelDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel,
    content: @Composable ColumnScope.() -> Unit
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.small,
    text = {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(dimensionResource(id = R.dimen.card_padding)),
            content = content
        )
    },
    confirmButton = {
        SmallTextButton(
            stringRes = continueText,
            onClick = onConfirm
        )
    },
    dismissButton = {
        SmallTextButton(
            stringRes = dismissText,
            onClick = onDismissRequest
        )
    }
)

@Composable
fun ConfirmCancelDialog(
    @StringRes body: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel
) = ConfirmCancelDialog(
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm,
    continueText = continueText,
    dismissText = dismissText,
    content = { Text(text = stringResource(id = body)) }
)
