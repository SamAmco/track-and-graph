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

import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.slimOutlinedTextField
import java.util.Locale

@Composable
fun ConfigurationInputField(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    input: LuaScriptConfigurationInput
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    // Display the configuration name
    val name = input.name.resolve()
    if (name != null) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
        )
    }

    // Display the input field based on type
    when (input) {
        is LuaScriptConfigurationInput.Text -> TextTextField(focusManager, input)
        is LuaScriptConfigurationInput.Number -> NumberTextField(focusManager, input)
    }
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
    label = null,
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
    label = null,
)

@Composable
private fun TranslatedString?.resolve(): String? {
    if (this == null) return null
    return when (this) {
        is TranslatedString.Simple -> value
        is TranslatedString.Translations -> resolveTranslated()
    }
}

@Composable
private fun TranslatedString.Translations?.resolveTranslated(): String? {
    if (this == null) return null
    if (values.isEmpty()) return ""

    val configuration = LocalConfiguration.current

    return remember(this) {
        // Normalize keys to BCP-47 tags
        val supported = values.keys
            .map { Locale.forLanguageTag(it).toLanguageTag() }
            .toTypedArray()

        // Ask Android for the best match given user’s locale prefs
        val userLocales: LocaleList = configuration.locales
        val best: Locale? = userLocales.getFirstMatch(supported)

        // Exact match?
        best?.toLanguageTag()?.let { tag ->
            values[tag]?.let { return@remember it }
        }

        // Language-only fallback
        best?.language?.let { lang ->
            values[lang]?.let { return@remember it }
        }

        // Last-ditch fallback
        values.values.firstOrNull()
    }
}

// TODO: Implement proper TranslatedString handling
private fun getConfigurationDisplayName(name: TranslatedString?): String {
    // TODO: Handle TranslatedString properly - this is a placeholder
    return name?.toString() ?: "Configuration Parameter"
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
