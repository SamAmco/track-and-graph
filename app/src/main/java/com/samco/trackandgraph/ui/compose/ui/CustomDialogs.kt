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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    scrollContent: Boolean = true,
    paddingValues: PaddingValues = PaddingValues(
        dimensionResource(id = R.dimen.card_padding_large)
    ),
    properties: DialogProperties? = null,
    content: @Composable ColumnScope.() -> Unit,
) = DialogTheme {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties ?: DialogProperties(decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .let {
                        if (scrollContent) it.verticalScroll(state = rememberScrollState())
                        else it
                    },
                content = content
            )
        }
    }
}

@Composable
fun CustomContinueCancelDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel,
    continueEnabled: Boolean = true,
    scrollContent: Boolean = true,
    properties: DialogProperties? = null,
    content: @Composable ColumnScope.() -> Unit
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    scrollContent = false,
    properties = properties,
    paddingValues = PaddingValues(
        start = dimensionResource(id = R.dimen.card_padding_large),
        end = dimensionResource(id = R.dimen.card_padding_large),
        bottom = dimensionResource(id = R.dimen.card_padding),
        top = dimensionResource(id = R.dimen.card_padding_large),
    )
) {
    Column(
        modifier = Modifier.wrapContentWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .let {
                    if (scrollContent) it.verticalScroll(state = rememberScrollState())
                    else it
                },
            content = content
        )

        DialogInputSpacing()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
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
fun ContinueDialog(
    onConfirm: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    onDismissRequest: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    content: @Composable ColumnScope.() -> Unit
) = DialogTheme {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.small,
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState()),
                content = content
            )
        },
        confirmButton = {
            SmallTextButton(
                stringRes = continueText,
                onClick = onConfirm
            )
        },
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnClickOutside
        ),
    )
}

@Composable
fun ContinueCancelDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel,
    continueEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) = DialogTheme {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.small,
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState()),
                content = content
            )
        },
        confirmButton = {
            SmallTextButton(
                stringRes = continueText,
                enabled = continueEnabled,
                onClick = onConfirm
            )
        },
        dismissButton = {
            SmallTextButton(
                stringRes = dismissText,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                ),
                onClick = onDismissRequest
            )
        }
    )
}

@Composable
fun ContinueCancelDialog(
    @StringRes body: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel
) = ContinueCancelDialog(
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm,
    continueText = continueText,
    dismissText = dismissText,
    content = {
        Text(
            text = stringResource(id = body),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.tngColors.onSurface
        )
    }
)
