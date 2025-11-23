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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.data.lua.dto.EnumOption
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DurationInput
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.RowCheckbox
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.resolve
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import org.threeten.bp.OffsetDateTime

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
        is LuaScriptConfigurationInput.UInt -> UIntTextField(focusManager, input)
        is LuaScriptConfigurationInput.Duration -> DurationField(focusManager, input)
        is LuaScriptConfigurationInput.LocalTime -> LocalTimeField(input)
        is LuaScriptConfigurationInput.Instant -> InstantField(input)
    }
}

@Composable
private fun EnumDropdownField(
    input: LuaScriptConfigurationInput.Enum
) = Column {
    Text(
        modifier = Modifier.padding(horizontal = halfDialogInputSpacing),
        text = input.name.resolve() ?: "",
        style = MaterialTheme.typography.bodySmall
    )
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

@Composable
private fun UIntTextField(
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput.UInt
) = ValueInputTextField(
    modifier = Modifier.fillMaxWidth(),
    textFieldValue = input.value.value,
    onValueChange = { tfv ->
        if (tfv.text.all { it.isDigit() }) {
            input.value.value = tfv
        } else {
            input.value.value = tfv.copy(text = tfv.text.filter { it.isDigit() })
        }
    },
    focusManager = focusManager,
    label = { input.name.resolve()?.let { Text(it) } },
    keyboardType = KeyboardType.Number,
)

@Composable
private fun DurationField(
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput.Duration
) = Column {
    input.name.resolve()?.let {
        Text(
            modifier = Modifier.padding(horizontal = halfDialogInputSpacing),
            text = it,
            style = MaterialTheme.typography.bodySmall
        )
    }
    DurationInput(
        modifier = Modifier.fillMaxWidth(),
        viewModel = input.viewModel,
        focusManager = focusManager
    )
}

@Composable
private fun LocalTimeField(
    input: LuaScriptConfigurationInput.LocalTime
) = Column {

    val previewMode = LocalInspectionMode.current

    val now = remember(input) {
        if (previewMode) {
            OffsetDateTime.parse("2023-06-15T14:30:00+01:00")
        } else {
            OffsetDateTime.now()
        }
    }

    input.name.resolve()?.let {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = halfDialogInputSpacing),
            text = it,
            style = MaterialTheme.typography.bodySmall
        )
    }
    HalfDialogInputSpacing()
    TimeButton(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .align(Alignment.CenterHorizontally),
        dateTime = now
            .withHour(input.time.value.hour.coerceIn(0, 23))
            .withMinute(input.time.value.minute.coerceIn(0, 59)),
        onTimeSelected = { selectedTime ->
            input.time.value = selectedTime
        }
    )
}

@Composable
private fun InstantField(
    input: LuaScriptConfigurationInput.Instant
) = Column {
    input.name.resolve()?.let {
        Text(
            modifier = Modifier.padding(horizontal = halfDialogInputSpacing),
            text = it,
            style = MaterialTheme.typography.bodySmall
        )
    }
    HalfDialogInputSpacing()
    DateTimeButtonRow(
        modifier = Modifier.fillMaxWidth(),
        selectedDateTime = input.dateTime.value,
        onDateTimeSelected = { dateTime ->
            input.dateTime.value = dateTime
        }
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

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldUIntPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.UInt(
                name = TranslatedString.Simple("Sample UInt Parameter"),
                value = remember { mutableStateOf(TextFieldValue("42")) }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldDurationPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Duration(
                name = TranslatedString.Simple("Sample Duration Parameter"),
                viewModel = remember {
                    DurationInputViewModelImpl().apply {
                        setDurationFromDouble(5430.0) // 1h 30m 30s
                    }
                }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldLocalTimePreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.LocalTime(
                name = TranslatedString.Simple("Sample Time Parameter"),
                time = remember { mutableStateOf(SelectedTime(14, 30)) }
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationInputFieldInstantPreview() {
    TnGComposeTheme {
        ConfigurationInputField(
            focusManager = LocalFocusManager.current,
            input = LuaScriptConfigurationInput.Instant(
                name = TranslatedString.Simple("Sample DateTime Parameter"),
                dateTime = remember { mutableStateOf(OffsetDateTime.parse("2023-06-15T14:30:00+01:00")) }
            )
        )
    }
}
