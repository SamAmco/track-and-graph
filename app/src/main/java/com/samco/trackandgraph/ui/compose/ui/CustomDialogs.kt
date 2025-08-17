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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.DialogTheme
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    scrollContent: Boolean = true,
    paddingValues: PaddingValues = PaddingValues(
        inputSpacingLarge
    ),
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) = DialogTheme {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = false,
            dismissOnClickOutside = dismissOnClickOutside,
        )
    ) {
        Surface(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
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
    @StringRes cancelText: Int = R.string.cancel,
    continueEnabled: Boolean = true,
    cancelVisible: Boolean = true,
    scrollContent: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    scrollContent = false,
    dismissOnClickOutside = dismissOnClickOutside,
    paddingValues = PaddingValues(
        start = inputSpacingLarge,
        end = inputSpacingLarge,
        bottom = halfDialogInputSpacing,
        top = inputSpacingLarge,
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
            if (cancelVisible) {
                SmallTextButton(
                    stringRes = cancelText,
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.tngColors.onSurface
                    )
                )
            }
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
    @StringRes body: Int,
    onConfirm: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    onDismissRequest: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
) = CustomContinueCancelDialog(
    onConfirm = onConfirm,
    dismissOnClickOutside = dismissOnClickOutside,
    onDismissRequest = onDismissRequest,
    continueText = continueText,
    cancelVisible = false,
    content = {
        Text(
            text = stringResource(id = body),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.tngColors.onSurface
        )
    }
)

@Composable
fun ContinueCancelDialog(
    @StringRes body: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes cancelText: Int = R.string.cancel,
) = CustomContinueCancelDialog(
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm,
    continueText = continueText,
    cancelText = cancelText,
    content = {
        Text(
            text = stringResource(id = body),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.tngColors.onSurface
        )
    }
)

@Preview
@Composable
private fun CustomDialogPreview() {
    TnGComposeTheme {
        CustomDialog(
            onDismissRequest = { }
        ) {
            Text(
                text = "This is a custom dialog with scrollable content. " +
                        "It can contain multiple lines of text and other UI elements.",
                style = MaterialTheme.typography.bodyLarge
            )
            DialogInputSpacing()
            Text(
                text = "Additional content below spacing",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
private fun CustomContinueCancelDialogPreview() {
    TnGComposeTheme {
        CustomContinueCancelDialog(
            onDismissRequest = { },
            onConfirm = { }
        ) {
            Text(
                text = "Headline",
                style = MaterialTheme.typography.titleMedium
            )
            DialogInputSpacing()
            SelectorButton(
                modifier = Modifier,
                text = "Selector Text Button",
                onClick = {}
            )
            DialogInputSpacing()
            Text(
                text = "This is a continue/cancel dialog. You can confirm or cancel this action.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
private fun CustomContinueCancelDialogDisabledPreview() {
    TnGComposeTheme {
        CustomContinueCancelDialog(
            onDismissRequest = { },
            onConfirm = { },
            continueEnabled = false
        ) {
            Text(
                text = "This dialog has the continue button disabled.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
private fun ContinueDialogPreview() {
    TnGComposeTheme {
        ContinueDialog(
            body = R.string.ru_sure_del_graph,
            onConfirm = { },
            onDismissRequest = { }
        )
    }
}

@Preview
@Composable
private fun ContinueCancelDialogPreview() {
    TnGComposeTheme {
        ContinueCancelDialog(
            body = R.string.ru_sure_del_feature,
            onDismissRequest = { },
            onConfirm = { }
        )
    }
}
