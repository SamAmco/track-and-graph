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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.compose.ui.TnGMarkdown
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.FadingScrollColumn
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.resolve
import com.samco.trackandgraph.ui.compose.ui.smallIconSize
import io.github.z4kn4fein.semver.toVersion

sealed class InfoDisplay {
    data object DataSource : InfoDisplay()
    data object LuaScript : InfoDisplay()
    data class Function(val metadata: LuaFunctionMetadata) : InfoDisplay()
}

@Composable
fun NodeDescriptionDialog(
    infoDisplay: InfoDisplay,
    onDismiss: () -> Unit,
) = CustomDialog(
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
            when (infoDisplay) {
                is InfoDisplay.Function -> FunctionHeader(infoDisplay.metadata)
                is InfoDisplay.LuaScript -> DefaultHeader(stringResource(R.string.lua_script))
                is InfoDisplay.DataSource -> DefaultHeader(stringResource(R.string.data_source))
            }

            Divider()

            DialogInputSpacing()

            // Description content
            FadingScrollColumn(modifier = Modifier.fillMaxWidth()) {
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

                TnGMarkdown(content = descriptionText)
            }
        }
    }
}

@Composable
private fun DefaultHeader(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
)

@Composable
private fun FunctionHeader(metadata: LuaFunctionMetadata) = Column {
    Text(
        text = metadata.title.resolve() ?: "",
        style = MaterialTheme.typography.headlineSmall,
    )
    metadata.version?.let {
        val str = remember(it) { it.toString() }
        Text(
            text = str,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        HalfDialogInputSpacing()
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoDisplayDialogDataSourcePreview() {
    TnGComposeTheme {
        NodeDescriptionDialog(
            infoDisplay = InfoDisplay.DataSource,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoDisplayDialogLuaScriptPreview() {
    TnGComposeTheme {
        NodeDescriptionDialog(
            infoDisplay = InfoDisplay.LuaScript,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoDisplayDialogFunctionPreview() {
    val function = LuaFunctionMetadata(
        script = "-- script",
        id = "multiply",
        description = TranslatedString.Simple(
            """
            ##### This description supports markdown
            
            - Like bullet points
            - Or **bold** text
            - Or `code` snippets
        """.trimIndent()
        ),
        version = "1.0.1".toVersion(),
        title = TranslatedString.Simple("Multiply Values"),
        inputCount = 2,
        config = emptyList(),
        categories = mapOf("math" to TranslatedString.Simple("Math"))
    )

    TnGComposeTheme {
        NodeDescriptionDialog(
            infoDisplay = InfoDisplay.Function(function),
            onDismiss = {}
        )
    }
}
