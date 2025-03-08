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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun LuaScriptEditDialog(
    script: TextFieldValue,
    onDismiss: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
        decorFitsSystemWindows = false,
        usePlatformDefaultWidth = false
    ),
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(dialogInputSpacing)
            .imePadding()
            .background(MaterialTheme.colors.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        CodeEditor(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(8.dp),
            script = script,
            onValueChanged = onValueChanged
        )
    }
}

@Composable
private fun CodeEditor(
    modifier: Modifier = Modifier,
    script: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val verticalScrollState = rememberScrollState()
    val lineHeight = 22.sp
    Row(
        modifier = modifier
            .imePadding()
            .padding(4.dp)
    ) {
        val numberOfLines by remember { derivedStateOf { script.text.lines().size } }
        Column(
            modifier = Modifier
                .verticalScroll(verticalScrollState)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (index in 1..numberOfLines) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.body1,
                    lineHeight = lineHeight,
                    color = MaterialTheme.tngColors.textColorSecondary,
                    modifier = Modifier
                )
            }
        }

        // Scrollable Text Field
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(verticalScrollState),
            value = script,
            onValueChange = { onValueChanged(it) },
            visualTransformation = luaCodeVisualTransformation(),
            textStyle = MaterialTheme.typography.body1.copy(
                lineHeight = lineHeight,
                color = MaterialTheme.colors.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LuaScriptEditDialogPreview() =
    TnGComposeTheme(darkTheme = true) {
        LuaScriptEditDialog(
            script = TextFieldValue(exampleScript),
            onDismiss = {},
            onValueChanged = {}
        )
    }

private val exampleScript = """
--- A Lua script
local tng = require("tng")

return function(sources)
  local _, source = next(sources)
  if source == nil then
    error("Failed to find a data source")
  end

  if source.dp == nil then
    error("Data source does not have a dp function")
  end

  local sum = 0
  for _ = 1, 10 do
    local dp = source.dp()
    if dp == nil then
      break
    end

    sum = sum + dp.value
  end

  return {
    type = tng.GRAPH_TYPE.TEXT,
    text = "Sum: " .. sum,
  }
end
""".trimIndent()