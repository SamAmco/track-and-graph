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
package com.samco.trackandgraph.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
internal fun SymlinksDialog(
    viewModel: SymlinksDialogViewModel,
) {
    val data = viewModel.dialogData.collectAsStateWithLifecycle().value ?: return

    SymlinksDialogContent(
        data = data,
        onDismiss = { viewModel.dismiss() },
    )
}

/**
 * @param onPathClick When non-null, each row is tappable and forwards its index; the dialog
 * is intended to dismiss itself via [onDismiss] from the caller's click handler. When null,
 * the dialog is information-only.
 */
@Composable
internal fun SymlinksDialogContent(
    data: SymlinksDialogData,
    onDismiss: () -> Unit,
    onPathClick: ((Int) -> Unit)? = null,
) {
    CustomDialog(
        onDismissRequest = onDismiss,
        paddingValues = PaddingValues(
            start = inputSpacingLarge,
            end = inputSpacingLarge,
            bottom = halfDialogInputSpacing,
            top = inputSpacingLarge,
        )
    ) {
        Text(
            text = stringResource(R.string.symlink_locations_title, data.componentName),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        DialogInputSpacing()
        Column(modifier = Modifier.fillMaxWidth()) {
            data.paths.forEachIndexed { index, path ->
                val rowModifier = Modifier
                    .fillMaxWidth()
                    .let { if (onPathClick != null) it.clickable { onPathClick(index) } else it }
                    .padding(vertical = halfDialogInputSpacing)
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = rowModifier,
                )
                if (index < data.paths.size - 1) {
                    HorizontalDivider()
                }
            }
        }
        DialogInputSpacing()
        SmallTextButton(
            stringRes = R.string.ok,
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Preview
@Composable
private fun SymlinksDialogPreview() {
    TnGComposeTheme {
        SymlinksDialogContent(
            data = SymlinksDialogData(
                componentName = "Steps",
                paths = listOf(
                    "/Health/Exercise/Steps",
                    "/Dashboard/Exercise/Steps",
                    "/Favorites/Steps",
                ),
            ),
            onDismiss = {},
        )
    }
}
