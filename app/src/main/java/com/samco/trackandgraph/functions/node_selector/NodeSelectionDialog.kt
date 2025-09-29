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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.functions.viewmodel.AddNodeData
import com.samco.trackandgraph.ui.compose.ui.resolve


@Composable
fun NodeSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (AddNodeData) -> Unit,
) {
    val viewModel: NodeSelectionViewModel = hiltViewModel<NodeSelectionViewModelImpl>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    NodeSelectionDialogUi(
        state = state,
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
fun NodeSelectionDialogUi(
    state: NodeSelectionUiState,
    onDismiss: () -> Unit,
    onSelect: (AddNodeData) -> Unit,
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

            when (state) {
                is NodeSelectionUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is NodeSelectionUiState.Ready -> {
                    Divider()

                    // Always include a Data Source option at the top
                    SelectionRow(
                        text = stringResource(R.string.data_source),
                        onClick = {
                            onSelect(AddNodeData.DataSourceNode)
                            onDismiss()
                        }
                    )

                    Divider()

                    for (meta in state.items) {
                        val title = meta.title.resolve() ?: continue
                        SelectionRow(
                            text = title,
                            onClick = {
                                onSelect(AddNodeData.LibraryFunction(meta))
                                onDismiss()
                            }
                        )
                        Divider()
                    }

                    // Plain Lua Script option at the bottom
                    SelectionRow(
                        text = stringResource(R.string.lua_script),
                        onClick = {
                            onSelect(AddNodeData.LuaScriptNode)
                            onDismiss()
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = dialogInputSpacing)
    )
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Ready(
                items = listOf(
                    LuaFunctionMetadata(
                        script = "-- script1",
                        version = null,
                        title = TranslatedString.Simple("Multiply Values"),
                        inputCount = 1,
                        config = emptyList(),
                    ),
                    LuaFunctionMetadata(
                        script = "-- script2",
                        version = null,
                        title = TranslatedString.Simple("Filter by Label"),
                        inputCount = 1,
                        config = emptyList(),
                    )
                )
            ),
            onDismiss = {},
            onSelect = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogLoadingPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Loading,
            onDismiss = {},
            onSelect = {}
        )
    }
}