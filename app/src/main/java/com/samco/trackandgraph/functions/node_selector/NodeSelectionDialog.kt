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
package com.samco.trackandgraph.functions.node_selector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing


@Composable
fun NodeSelectionDialog(
    onDismiss: () -> Unit,
    onDataSourceSelected: () -> Unit,
    onLuaScriptSelected: () -> Unit
) {
    CustomDialog(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.select_node_type),
                style = MaterialTheme.typography.headlineSmall,
            )

            DialogInputSpacing()

            Divider()

            Text(
                text = stringResource(R.string.data_source),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDataSourceSelected()
                        onDismiss()
                    }
                    .padding(vertical = dialogInputSpacing)
            )

            Divider()

            Text(
                text = stringResource(R.string.lua_script),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLuaScriptSelected()
                        onDismiss()
                    }
                    .padding(vertical = dialogInputSpacing)
            )
            Divider()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogPreview() {
    TnGComposeTheme {
        NodeSelectionDialog(
            onDismiss = { },
            onDataSourceSelected = { },
            onLuaScriptSelected = { }
        )
    }
}
