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
package com.samco.trackandgraph.functions.node_editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.functions.viewmodel.Node
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngTypography
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.IconTextButton
import com.samco.trackandgraph.ui.compose.ui.luaCodeVisualTransformation
import com.samco.trackandgraph.ui.compose.ui.slimOutlinedTextField
import com.samco.trackandgraph.ui.compose.ui.InputSpacingXLarge
import com.samco.trackandgraph.ui.compose.ui.LuaScriptEditDialog
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
internal fun LuaScriptNode(
    node: Node.LuaScript,
    onDeleteNode: () -> Unit = {},
    onUpdateScript: (String) -> Unit = {},
    onUpdateScriptFromFile: (Uri?) -> Unit = {},
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var tempScript by remember { mutableStateOf(TextFieldValue(node.script)) }

    // Update temp script when node script changes
    LaunchedEffect(node.script) {
        tempScript = TextFieldValue(node.script)
    }

    Column(
        Modifier
            .width(nodeCardContentWidth)
            .padding(horizontal = connectorSize / 2, vertical = cardPadding),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.lua_script),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                modifier = Modifier.size(buttonSize),
                onClick = onDeleteNode
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }

        Buttons(
            onUpdateScriptFromClipboard = onUpdateScript,
            onReadFile = onUpdateScriptFromFile
        )

        ScriptPreviewTextField(
            scriptPreview = node.script,
            onScriptPreviewClicked = { textFieldValue ->
                showDialog = true
                // Handle cursor position translation if needed
                tempScript = tempScript.copy(
                    selection = textFieldValue.selection
                )
            }
        )

        val focusManager = LocalFocusManager.current

        // Configuration inputs
        ConfigurationInputs(
            focusManager = focusManager,
            configuration = node.configuration
        )

        // Dialog
        if (showDialog) {
            LuaScriptEditDialog(
                script = tempScript,
                onDismiss = {
                    showDialog = false
                    // Update the view model with the final script when dialog is closed
                    onUpdateScript(tempScript.text)
                },
                onValueChanged = { newValue ->
                    tempScript = newValue
                }
            )
        }
    }
}

@Composable
private fun ScriptPreviewTextField(
    scriptPreview: String,
    onScriptPreviewClicked: (TextFieldValue) -> Unit,
) = Box {
    val localFocusManager = LocalFocusManager.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    val maxVisibleLines = 8

    val showOverlay = remember(scriptPreview) {
        scriptPreview.lines().size > maxVisibleLines
    }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .slimOutlinedTextField()
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
            .horizontalScroll(rememberScrollState())
            .drawWithContent {
                drawContent()
                drawRect(color = surfaceColor.copy(alpha = 0.3f))

                // Draw gradient overlay at bottom if content overflows
                if (showOverlay) {
                    val gradientHeight = size.height * 0.3f // 30% of field height
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0.0f),
                            surfaceColor.copy(alpha = 1.0f)
                        ),
                        startY = size.height - gradientHeight,
                        endY = size.height
                    )
                    drawRect(
                        brush = gradient,
                        topLeft = Offset(0f, size.height - gradientHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, gradientHeight)
                    )
                }
            },
        maxLines = 8,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        value = TextFieldValue(scriptPreview),
        onValueChange = { textFieldValue ->
            onScriptPreviewClicked(textFieldValue)
            localFocusManager.clearFocus()
        },
        placeholder = {
            Text(
                text = stringResource(R.string.lua_script_input_hint),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        visualTransformation = luaCodeVisualTransformation(),
        textStyle = MaterialTheme.tngTypography.code,
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        )
    )
}

@Composable
private fun Buttons(
    onReadFile: (Uri?) -> Unit,
    onUpdateScriptFromClipboard: (String) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
) {

    val clipboardManager = LocalClipboardManager.current
    IconTextButton(
        onClick = {
            val text = clipboardManager.getText()?.text
                ?: return@IconTextButton
            onUpdateScriptFromClipboard(text)
        },
        icon = R.drawable.content_paste,
        text = stringResource(R.string.paste)
    )

    InputSpacingXLarge()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { onReadFile(it) }

    IconTextButton(
        onClick = { launcher.launch("*/*") },
        icon = R.drawable.folder_open,
        text = stringResource(R.string.file)
    )
}

@Composable
private fun ConfigurationInputs(
    focusManager: FocusManager,
    configuration: Map<String, LuaScriptConfigurationInput>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(inputSpacingLarge)
    ) {
        configuration.forEach { (_, input) ->
            ConfigurationInputField(
                modifier = Modifier.fillMaxWidth(),
                focusManager = focusManager,
                input = input,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun LuaScriptNodePreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.LuaScript(
                id = 1,
                inputConnectorCount = 2,
                script = """
                    function main(input1, input2)
                        return input1 + input2 + input3 + input4 + input5 + input6 + input7 + input8 + input9 + input10
                    end
                """.trimIndent(),
                configuration = mapOf(
                    "threshold" to LuaScriptConfigurationInput.Number(
                        name = TranslatedString.Simple("Threshold"),
                        value = remember { mutableStateOf(TextFieldValue("10.5")) }
                    ),
                    "label" to LuaScriptConfigurationInput.Text(
                        name = TranslatedString.Simple("Label"),
                        value = remember { mutableStateOf(TextFieldValue("Sample Label")) }
                    )
                ),
            )

            LuaScriptNode(
                node = sampleNode,
                onDeleteNode = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LuaScriptNodeEmptyPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.LuaScript(
                id = 2,
                inputConnectorCount = 1,
                script = "",
                configuration = emptyMap(),
            )

            LuaScriptNode(
                node = sampleNode,
                onDeleteNode = { }
            )
        }
    }
}
