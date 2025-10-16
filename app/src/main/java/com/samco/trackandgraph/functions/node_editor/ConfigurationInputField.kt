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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.data.lua.dto.EnumOption
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.resolve

@Composable
fun ConfigurationInputField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    // Display the input field based on type
    when (input) {
        is LuaScriptConfigurationInput.Text -> TextTextField(focusManager, input)
        is LuaScriptConfigurationInput.Number -> NumberTextField(focusManager, input)
        is LuaScriptConfigurationInput.Checkbox -> CheckboxField(input)
        is LuaScriptConfigurationInput.Enum -> EnumDropdownField(input)
    }
}

@Composable
private fun EnumDropdownField(
    input: LuaScriptConfigurationInput.Enum
) {
    TextMapSpinner(
        modifier = Modifier.fillMaxWidth(),
        strings = input.options.associate {
            it.id to (it.displayName.resolve() ?: "")
        },
        selectedItem = input.value.value,
        onItemSelected = { input.value.value = it }
    )
}

@Composable
private fun TextTextField(
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput.Text
) = LabelInputTextField(
    modifier = Modifier.fillMaxWidth(),
    textFieldValue = input.value.value,
    onValueChange = { input.value.value = it },
    focusManager = focusManager,
    label = { input.name.resolve()?.let { Text(it) } },
)


@Composable
private fun NumberTextField(
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput.Number
) = ValueInputTextField(
    modifier = Modifier.fillMaxWidth(),
    textFieldValue = input.value.value,
    onValueChange = { input.value.value = it },
    focusManager = focusManager,
    label = { input.name.resolve()?.let { Text(it) } },
)

@Composable
private fun CheckboxField(
    input: LuaScriptConfigurationInput.Checkbox
) {
    val text = input.name.resolve() ?: ""
    RowCheckbox(
        modifier = Modifier.fillMaxWidth(),
        checked = input.value.value,
        onCheckedChange = { input.value.value = it },
        text = text
    )
}


@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldTextPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Text(
                name = TranslatedString.Simple("Sample Text Parameter"),
                value = remember { mutableStateOf(TextFieldValue("Sample text value")) }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldNumberPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Number(
                name = TranslatedString.Simple("Sample Number Parameter"),
                value = remember { mutableStateOf(TextFieldValue("42.5")) }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldCheckboxPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Checkbox(
                name = TranslatedString.Simple("Sample Checkbox Parameter"),
                value = remember { mutableStateOf(true) }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldEnumPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Enum(
                name = TranslatedString.Simple("Period"),
                options = listOf(
                    EnumOption("day", TranslatedString.Simple("Day")),
                    EnumOption("week", TranslatedString.Simple("Week")),
                ),
                value = remember { mutableStateOf("week") }
            )
        )
    }
}
