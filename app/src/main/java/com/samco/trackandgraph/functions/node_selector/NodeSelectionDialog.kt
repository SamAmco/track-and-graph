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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import com.samco.trackandgraph.functions.node_editor.viewmodel.AddNodeData
import com.samco.trackandgraph.ui.compose.ui.FadingScrollColumn
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.resolve
import com.samco.trackandgraph.ui.compose.ui.smallIconSize
import com.mikepenz.markdown.m3.Markdown

sealed class InfoDisplay {
    data object DataSource : InfoDisplay()
    data object LuaScript : InfoDisplay()
    data class Function(val metadata: LuaFunctionMetadata) : InfoDisplay()
}

@Composable
fun NodeSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (AddNodeData) -> Unit,
) {
    val viewModel: NodeSelectionViewModel = hiltViewModel<NodeSelectionViewModelImpl>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var infoDisplay by remember { mutableStateOf<InfoDisplay?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    NodeSelectionDialogUi(
        state = state,
        onDismiss = onDismiss,
        onSelect = {
            viewModel.clearSelection()
            onSelect(it)
        },
        onRetry = viewModel::retry,
        onSelectCategory = viewModel::selectCategory,
        isLandscape = isLandscape,
        infoDisplay = infoDisplay,
        onShowInfo = { infoDisplay = it },
        onCloseInfo = { infoDisplay = null }
    )
}

@Composable
fun NodeSelectionDialogUi(
    state: NodeSelectionUiState,
    onDismiss: () -> Unit,
    onSelect: (AddNodeData) -> Unit,
    onRetry: () -> Unit = {},
    onSelectCategory: (String?) -> Unit = {},
    isLandscape: Boolean,
    infoDisplay: InfoDisplay? = null,
    onShowInfo: (InfoDisplay) -> Unit = {},
    onCloseInfo: () -> Unit = {},
) {
    val usePlatformDefaultWidth =
        state !is NodeSelectionUiState.Ready || state.selectedCategory == null
    val widthPercentage = if (usePlatformDefaultWidth) 1f else 0.9f

    CustomDialog(
        onDismissRequest = onDismiss,
        scrollContent = false,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        paddingValues = PaddingValues(inputSpacingLarge),
    ) {
        Column(modifier = Modifier.fillMaxWidth(widthPercentage)) {
            SelectionView(
                state = state,
                isLandscape = isLandscape,
                onSelect = onSelect,
                onDismiss = onDismiss,
                onRetry = onRetry,
                onSelectCategory = onSelectCategory,
                onShowInfo = onShowInfo
            )
        }
    }

    // Show info dialog on top when needed
    if (infoDisplay != null) {
        InfoDisplayDialog(
            infoDisplay = infoDisplay,
            onDismiss = onCloseInfo
        )
    }
}

@Composable
private fun SelectionView(
    state: NodeSelectionUiState,
    isLandscape: Boolean,
    onSelect: (AddNodeData) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onShowInfo: (InfoDisplay) -> Unit,
) {
    Text(
        text = stringResource(R.string.select_node_type),
        style = MaterialTheme.typography.headlineSmall,
    )

    DialogInputSpacing()

    when (state) {
        is NodeSelectionUiState.Loading -> {
            LoadingState()
        }

        is NodeSelectionUiState.Ready -> {
            if (isLandscape) {
                LandscapeReadyState(
                    state = state,
                    onSelect = onSelect,
                    onDismiss = onDismiss,
                    onSelectCategory = onSelectCategory,
                    onShowInfo = onShowInfo
                )
            } else {
                // TODO: Implement portrait mode with different UI flow
                PortraitReadyState(
                    state = state,
                    onSelect = onSelect,
                    onDismiss = onDismiss,
                    onSelectCategory = onSelectCategory,
                    onShowInfo = onShowInfo
                )
            }
        }

        is NodeSelectionUiState.Error -> {
            ErrorState(
                error = state.error,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun InfoDisplayDialog(
    infoDisplay: InfoDisplay,
    onDismiss: () -> Unit,
) {
    CustomDialog(
        onDismissRequest = onDismiss,
        paddingValues = PaddingValues(),
        scrollContent = false,
    ) {
        Box {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(buttonSize)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(smallIconSize)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(inputSpacingLarge)
            ) {
                // Header
                Text(
                    text = when (infoDisplay) {
                        is InfoDisplay.DataSource -> stringResource(R.string.data_source)
                        is InfoDisplay.LuaScript -> stringResource(R.string.lua_script)
                        is InfoDisplay.Function -> infoDisplay.metadata.title.resolve() ?: ""
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )

                Divider()

                DialogInputSpacing()

                // Description content
                FadingScrollColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 400.dp)
                ) {
                    val descriptionText = when (infoDisplay) {
                        is InfoDisplay.DataSource -> {
                            stringResource(R.string.data_source_description)
                        }

                        is InfoDisplay.LuaScript -> {
                            stringResource(R.string.lua_script_description)
                        }

                        is InfoDisplay.Function -> {
                            infoDisplay.metadata.description.resolve()?.trim() ?: ""
                        }
                    }

                    Markdown(descriptionText)
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LandscapeReadyState(
    state: NodeSelectionUiState.Ready,
    onSelect: (AddNodeData) -> Unit,
    onDismiss: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onShowInfo: (InfoDisplay) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
    ) {
        // Left side - Categories list
        FadingScrollColumn(modifier = Modifier.weight(1f)) {
            Divider()

            // Data Source option at the top
            SelectionRow(
                text = stringResource(R.string.data_source),
                onClick = {
                    onSelect(AddNodeData.DataSourceNode)
                    onDismiss()
                },
                showInfoIcon = true,
                onInfoClick = {
                    onShowInfo(InfoDisplay.DataSource)
                }
            )

            Divider()

            // Categories in the middle
            state.allCategories.forEach { (categoryId, categoryName) ->
                SelectionRow(
                    text = categoryName.resolve() ?: categoryId,
                    onClick = { onSelectCategory(categoryId) },
                    isSelected = state.selectedCategory == categoryId
                )
                Divider()
            }

            // Lua Script option at the bottom
            SelectionRow(
                text = stringResource(R.string.lua_script),
                onClick = {
                    onSelect(AddNodeData.LuaScriptNode)
                    onDismiss()
                },
                showInfoIcon = true,
                onInfoClick = {
                    onShowInfo(InfoDisplay.LuaScript)
                }
            )

            Divider()
        }

        // Show functions list if a category is selected
        if (state.selectedCategory != null) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            )

            // Right side - Functions list for selected category
            FadingScrollColumn(modifier = Modifier.weight(1f)) {
                Divider()

                state.displayedFunctions.forEach { function ->
                    val title = function.title.resolve() ?: return@forEach
                    SelectionRow(
                        text = title,
                        onClick = {
                            onSelect(AddNodeData.LibraryFunction(function))
                            onDismiss()
                        },
                        showInfoIcon = true,
                        onInfoClick = {
                            onShowInfo(InfoDisplay.Function(function))
                        }
                    )
                    Divider()
                }

                if (state.displayedFunctions.isEmpty()) {
                    Text(
                        text = "No functions in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitReadyState(
    state: NodeSelectionUiState.Ready,
    onSelect: (AddNodeData) -> Unit,
    onDismiss: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onShowInfo: (InfoDisplay) -> Unit,
) {
    // TODO: Implement portrait mode
    // For now, just show all functions as before
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Divider()

        // Data Source option at the top
        SelectionRow(
            text = stringResource(R.string.data_source),
            onClick = {
                onSelect(AddNodeData.DataSourceNode)
                onDismiss()
            },
            showInfoIcon = true,
            onInfoClick = {
                onShowInfo(InfoDisplay.DataSource)
            }
        )

        Divider()

        // Show all functions for now
        state.allFunctions.forEach { function ->
            val title = function.title.resolve() ?: return@forEach
            SelectionRow(
                text = title,
                onClick = {
                    onSelect(AddNodeData.LibraryFunction(function))
                    onDismiss()
                },
                showInfoIcon = true,
                onInfoClick = {
                    onShowInfo(InfoDisplay.Function(function))
                }
            )
            Divider()
        }

        // Lua Script option at the bottom
        SelectionRow(
            text = stringResource(R.string.lua_script),
            onClick = {
                onSelect(AddNodeData.LuaScriptNode)
                onDismiss()
            },
            showInfoIcon = true,
            onInfoClick = {
                onShowInfo(InfoDisplay.LuaScript)
            }
        )

        Divider()
    }
}

@Composable
private fun ErrorState(
    error: FetchError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (error) {
                FetchError.VERIFICATION_FAILURE -> {
                    // TODO: Add string resource for verification failure message
                    "Failed to verify function signatures"
                }

                FetchError.NETWORK_FAILURE -> {
                    // TODO: Add string resource for network failure message
                    "Failed to load functions. Check your network connection."
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        DialogInputSpacing()

        IconButton(onClick = onRetry) {
            Icon(
                painter = painterResource(id = R.drawable.refresh),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SelectionRow(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    showInfoIcon: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = buttonSize)
            .clickable { onClick() }
            .let {
                if (isSelected) {
                    it.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
                } else it
            }
            .padding(horizontal = dialogInputSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = dialogInputSpacing)
        )

        if (showInfoIcon) {
            IconButton(
                onClick = { onInfoClick?.invoke() },
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.about_icon),
                    contentDescription = stringResource(R.string.info),
                    modifier = Modifier.size(smallIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogPreview() {
    val functions = listOf(
        LuaFunctionMetadata(
            script = "-- script1",
            id = null,
            description = null,
            version = null,
            title = TranslatedString.Simple("Multiply Values"),
            inputCount = 1,
            config = emptyList(),
            categories = mapOf("math" to TranslatedString.Simple("Math"))
        ),
        LuaFunctionMetadata(
            script = "-- script2",
            id = null,
            description = null,
            version = null,
            title = TranslatedString.Simple("Filter by Label"),
            inputCount = 1,
            config = emptyList(),
            categories = mapOf("filter" to TranslatedString.Simple("Filter"))
        )
    )

    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Ready(
                allFunctions = functions,
                displayedFunctions = functions,
                selectedCategory = "math",
                allCategories = mapOf(
                    "math" to TranslatedString.Simple("Math"),
                    "filter" to TranslatedString.Simple("Filter")
                )
            ),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
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
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogErrorPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Error(FetchError.NETWORK_FAILURE),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogVerificationErrorPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Error(FetchError.VERIFICATION_FAILURE),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogDataSourceInfoPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Ready(
                allFunctions = emptyList(),
                displayedFunctions = emptyList(),
                selectedCategory = null,
                allCategories = emptyMap()
            ),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
            infoDisplay = InfoDisplay.DataSource,
            onShowInfo = {},
            onCloseInfo = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogLuaScriptInfoPreview() {
    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Ready(
                allFunctions = emptyList(),
                displayedFunctions = emptyList(),
                selectedCategory = null,
                allCategories = emptyMap()
            ),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
            infoDisplay = InfoDisplay.LuaScript,
            onShowInfo = {},
            onCloseInfo = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeSelectionDialogFunctionInfoPreview() {
    val function = LuaFunctionMetadata(
        script = "-- script",
        id = "multiply",
        description = TranslatedString.Simple(
            "This function multiplies two input values together and returns the result. " +
                    "It's useful for scaling data or combining multiple metrics. " +
                    "The function handles both positive and negative numbers correctly."
        ),
        version = null,
        title = TranslatedString.Simple("Multiply Values"),
        inputCount = 2,
        config = emptyList(),
        categories = mapOf("math" to TranslatedString.Simple("Math"))
    )

    TnGComposeTheme {
        NodeSelectionDialogUi(
            state = NodeSelectionUiState.Ready(
                allFunctions = listOf(function),
                displayedFunctions = listOf(function),
                selectedCategory = null,
                allCategories = mapOf("math" to TranslatedString.Simple("Math"))
            ),
            onDismiss = {},
            onSelect = {},
            onRetry = {},
            onSelectCategory = {},
            isLandscape = true,
            infoDisplay = InfoDisplay.Function(function),
            onShowInfo = {},
            onCloseInfo = {}
        )
    }
}