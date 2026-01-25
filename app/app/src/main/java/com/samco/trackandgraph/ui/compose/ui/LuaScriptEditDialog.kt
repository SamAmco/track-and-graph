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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.theming.tngTypography
import kotlin.math.log10

@Composable
fun LuaScriptEditDialog(
    script: TextFieldValue,
    onDismiss: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit
) = CustomDialog(
    onDismissRequest = onDismiss,
    scrollContent = false,
    paddingValues = PaddingValues(halfDialogInputSpacing),
) {
    CodeEditor(
        script = script,
        onValueChanged = onValueChanged
    )
}

@Composable
private fun CodeEditor(
    modifier: Modifier = Modifier,
    script: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    //Set up auto scrolling when adding text past the bottom of the screen
    val lastMaxVerticalScroll = remember { mutableIntStateOf(0) }
    LaunchedEffect(verticalScrollState.maxValue) {
        if (lastMaxVerticalScroll.intValue != 0 &&
            verticalScrollState.value == lastMaxVerticalScroll.intValue
        ) {
            verticalScrollState.scrollTo(verticalScrollState.maxValue)
        }
        lastMaxVerticalScroll.intValue = verticalScrollState.maxValue
    }

    //Set up auto scrolling when adding text past the end of the screen
    val lastMaxHorizontalScroll = remember { mutableIntStateOf(0) }
    LaunchedEffect(horizontalScrollState.maxValue) {
        //Check if it == 0 to skip the first one
        if (lastMaxHorizontalScroll.intValue != 0 &&
            horizontalScrollState.value == lastMaxHorizontalScroll.intValue
        ) {
            horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
        }
        lastMaxHorizontalScroll.intValue = horizontalScrollState.maxValue
    }

    Row(
        modifier = modifier
            .imePadding()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(verticalScrollState)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            val lineNumbersText = remember(script.text) { getLinesText(script.text.lines().size) }
            Text(
                text = lineNumbersText,
                style = MaterialTheme.tngTypography.code,
                color = MaterialTheme.tngColors.textColorSecondary,
            )
        }

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        // Scrollable Text Field
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState),
            value = script,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
            ),
            onValueChange = { onValueChanged(it) },
            visualTransformation = luaCodeVisualTransformation(),
            textStyle = MaterialTheme.tngTypography.code.copy(
                color = MaterialTheme.tngColors.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        )
    }
}

private fun getLinesText(numLines: Int): String {
    val widthPerLine = log10(numLines.toDouble()).toInt() + 1
    return (1..numLines).joinToString("\n") {
        it.toString().padStart(widthPerLine, ' ')
    }
}

@Preview(showBackground = true)
@Composable
private fun LuaScriptEditDialogPreview() = TnGComposeTheme(darkTheme = true) {
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