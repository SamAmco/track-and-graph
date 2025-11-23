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

package com.samco.trackandgraph.functions.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.EnumOption
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationEncoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class LuaScriptConfigurationEncoderTest {

    private lateinit var encoder: LuaScriptConfigurationEncoder

    // Test data declarations at class level
    private val textInput = LuaScriptConfigurationInput.Text(
        name = TranslatedString.Simple("Text Config"),
        value = mutableStateOf(TextFieldValue("text value"))
    )

    private val numberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Number Config"),
        value = mutableStateOf(TextFieldValue("123.45"))
    )

    private val checkboxInput = LuaScriptConfigurationInput.Checkbox(
        name = TranslatedString.Simple("Checkbox Config"),
        value = mutableStateOf(true)
    )

    private val enumInput = LuaScriptConfigurationInput.Enum(
        name = TranslatedString.Simple("Enum Config"),
        options = listOf(
            EnumOption("hours", TranslatedString.Simple("Hours")),
            EnumOption("days", TranslatedString.Simple("Days"))
        ),
        value = mutableStateOf("hours")
    )

    private val uintInput = LuaScriptConfigurationInput.UInt(
        name = TranslatedString.Simple("UInt Config"),
        value = mutableStateOf(TextFieldValue("42"))
    )

    private val durationInput = LuaScriptConfigurationInput.Duration(
        name = TranslatedString.Simple("Duration Config"),
        viewModel = DurationInputViewModelImpl().apply { setDurationFromDouble(3600.0) }
    )

    private val localtimeInput = LuaScriptConfigurationInput.LocalTime(
        name = TranslatedString.Simple("LocalTime Config"),
        time = mutableStateOf(SelectedTime(14, 30))
    )

    private val instantInput = LuaScriptConfigurationInput.Instant(
        name = TranslatedString.Simple("Instant Config"),
        dateTime = mutableStateOf(OffsetDateTime.parse("2023-06-15T14:30:00Z"))
    )

    @Before
    fun setUp() {
        encoder = LuaScriptConfigurationEncoder()
    }

    @Test
    fun `encodeConfiguration handles empty configuration map`() {
        // Given
        val emptyConfiguration = emptyMap<String, LuaScriptConfigurationInput>()

        // When
        val result = encoder.encodeConfiguration(emptyConfiguration)

        // Then
        assertTrue("Empty configuration should produce empty list", result.isEmpty())
    }

    @Test
    fun `encodeConfiguration handles all configuration types and covers all enum values`() {
        // Given - Create configuration with all types using class-level declarations
        val configuration = mapOf(
            "textConfig" to textInput,
            "numberConfig" to numberInput,
            "checkboxConfig" to checkboxInput,
            "enumConfig" to enumInput,
            "uintConfig" to uintInput,
            "durationConfig" to durationInput,
            "localtimeConfig" to localtimeInput,
            "instantConfig" to instantInput
        )

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then - Verify all types are encoded correctly
        assertEquals("Should produce eight configuration values", 8, result.size)
        
        // Collect all encoded types
        val encodedTypes = result.map { it::class }.toSet()
        
        // Get all sealed subclasses that should be tested
        val allConfigTypes = LuaScriptConfigurationValue::class.sealedSubclasses.toSet()
        
        // Ensure our test covers all configuration types
        val missingTypes = allConfigTypes - encodedTypes
        
        if (missingTypes.isNotEmpty()) {
            val missingTypeNames = missingTypes.map { it.simpleName }.joinToString(", ")
            fail("Test does not cover all LuaScriptConfigurationValue types. Missing: $missingTypeNames. " +
                 "Please update this test to include all configuration input types. " +
                 "This test protects against missing encoding logic for new configuration types.")
        }
        
        // Also verify we're not testing non-existent types (defensive check)
        val extraTypes = encodedTypes - allConfigTypes
        if (extraTypes.isNotEmpty()) {
            val extraTypeNames = extraTypes.map { it.simpleName }.joinToString(", ")
            fail("Test includes unknown configuration types: $extraTypeNames")
        }

        // Verify specific encodings
        val textResult = result.find { it.id == "textConfig" } as LuaScriptConfigurationValue.Text
        assertEquals("Text value should be encoded correctly", "text value", textResult.value)
        
        val numberResult = result.find { it.id == "numberConfig" } as LuaScriptConfigurationValue.Number
        assertEquals("Number value should be encoded correctly", 123.45, numberResult.value, 0.0001)
        
        val checkboxResult = result.find { it.id == "checkboxConfig" } as LuaScriptConfigurationValue.Checkbox
        assertEquals("Checkbox value should be encoded correctly", true, checkboxResult.value)

        val enumResult = result.find { it.id == "enumConfig" } as LuaScriptConfigurationValue.Enum
        assertEquals("Enum value should be encoded correctly", "hours", enumResult.value)

        val uintResult = result.find { it.id == "uintConfig" } as LuaScriptConfigurationValue.UInt
        assertEquals("UInt value should be encoded correctly", 42, uintResult.value)

        val durationResult = result.find { it.id == "durationConfig" } as LuaScriptConfigurationValue.Duration
        assertEquals("Duration value should be encoded correctly", 3600.0, durationResult.seconds, 0.001)

        val localtimeResult = result.find { it.id == "localtimeConfig" } as LuaScriptConfigurationValue.LocalTime
        assertEquals("LocalTime value should be encoded correctly", 870, localtimeResult.minutes)  // 14*60+30

        val instantResult = result.find { it.id == "instantConfig" } as LuaScriptConfigurationValue.Instant
        assertEquals("Instant value should be encoded correctly", 1686839400000L, instantResult.epochMilli)
    }
}
