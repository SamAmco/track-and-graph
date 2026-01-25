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
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInput
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInputFactory
import io.github.z4kn4fein.semver.Version
import org.junit.Assert.*
import org.junit.Test

class LuaScriptConfigurationInputFactoryTest {

    private val factory = LuaScriptConfigurationInputFactory()


    // Comprehensive metadata containing all possible configuration types
    // This ensures we test all enum values and catch missing implementations
    private val allTypesMetadata = LuaFunctionMetadata(
        script = "comprehensive test script",
        id = "comprehensive-test",
        inputCount = 3,
        config = listOf(
            LuaFunctionConfigSpec.Text(
                id = "textConfig",
                name = TranslatedString.Simple("Text Configuration")
            ),
            LuaFunctionConfigSpec.Number(
                id = "numberConfig",
                name = TranslatedString.Simple("Number Configuration")
            ),
            LuaFunctionConfigSpec.Checkbox(
                id = "checkboxConfig",
                name = TranslatedString.Simple("Checkbox Configuration")
            ),
            LuaFunctionConfigSpec.Enum(
                id = "enumConfig",
                name = TranslatedString.Simple("Enum Configuration"),
                options = listOf(
                    EnumOption("hours", TranslatedString.Simple("Hours")),
                    EnumOption("days", TranslatedString.Simple("Days"))
                ),
                defaultValue = "hours"
            ),
            LuaFunctionConfigSpec.UInt(
                id = "uintConfig",
                name = TranslatedString.Simple("UInt Configuration"),
                defaultValue = 10
            ),
            LuaFunctionConfigSpec.Duration(
                id = "durationConfig",
                name = TranslatedString.Simple("Duration Configuration"),
                defaultValueSeconds = 3600.0  // Stored as seconds
            ),
            LuaFunctionConfigSpec.LocalTime(
                id = "localtimeConfig",
                name = TranslatedString.Simple("LocalTime Configuration"),
                defaultValueMinutes = 930  // Stored as minutes (15.5 hours)
            ),
            LuaFunctionConfigSpec.Instant(
                id = "instantConfig",
                name = TranslatedString.Simple("Instant Configuration"),
                defaultValueEpochMilli = 1686835800000L  // 2023-06-15T14:30:00Z
            )
        ),
        version = Version(1, 0, 0),
        title = TranslatedString.Simple("Comprehensive Test Script"),
        description = TranslatedString.Simple("A comprehensive test script that demonstrates all configuration types"),
    )

    private val allTypesConfig = listOf(
        LuaScriptConfigurationValue.Text(
            id = "textConfig",
            value = "default text"
        ),
        LuaScriptConfigurationValue.Number(
            id = "numberConfig",
            value = 123.45
        ),
        LuaScriptConfigurationValue.Checkbox(
            id = "checkboxConfig",
            value = true
        ),
        LuaScriptConfigurationValue.Enum(
            id = "enumConfig",
            value = "days"
        ),
        LuaScriptConfigurationValue.UInt(
            id = "uintConfig",
            value = 25
        ),
        LuaScriptConfigurationValue.Duration(
            id = "durationConfig",
            seconds = 7200.0
        ),
        LuaScriptConfigurationValue.LocalTime(
            id = "localtimeConfig",
            minutes = 930
        ),
        LuaScriptConfigurationValue.Instant(
            id = "instantConfig",
            epochMilli = 1687012200000L  // 2023-06-17T15:30:00Z
        )
    )

    @Test
    fun `factory handles all configuration types correctly and covers all sealed class types`() {
        // Given - Create a map of saved values by ID for testing restoration
        val savedValueMap = allTypesConfig.associateBy { it.id }

        // When - Create inputs for each config spec with corresponding saved values
        val createdInputs = allTypesMetadata.config.associate { config ->
            config.id to factory.createConfigurationInput(config, savedValueMap[config.id])
        }

        // Then - Validate each configuration type is created correctly
        val textInput = createdInputs["textConfig"] as LuaScriptConfigurationInput.Text
        assertSame(allTypesMetadata.config[0].name, textInput.name)
        assertEquals("default text", textInput.value.value.text)

        val numberInput = createdInputs["numberConfig"] as LuaScriptConfigurationInput.Number
        assertSame(allTypesMetadata.config[1].name, numberInput.name)
        assertEquals(123.45, numberInput.value.value.text.toDouble(), 0.0001)

        val checkboxInput = createdInputs["checkboxConfig"] as LuaScriptConfigurationInput.Checkbox
        assertSame(allTypesMetadata.config[2].name, checkboxInput.name)
        assertEquals(true, checkboxInput.value.value)

        val enumInput = createdInputs["enumConfig"] as LuaScriptConfigurationInput.Enum
        assertSame(allTypesMetadata.config[3].name, enumInput.name)
        assertEquals("days", enumInput.value.value)  // Should use saved value
        assertEquals(2, enumInput.options.size)

        val uintInput = createdInputs["uintConfig"] as LuaScriptConfigurationInput.UInt
        assertSame(allTypesMetadata.config[4].name, uintInput.name)
        assertEquals("25", uintInput.value.value.text)  // Should use saved value

        val durationInput = createdInputs["durationConfig"] as LuaScriptConfigurationInput.Duration
        assertSame(allTypesMetadata.config[5].name, durationInput.name)
        assertEquals(7200.0, durationInput.viewModel.getDurationAsDouble(), 0.001)  // Should use saved value

        val localtimeInput = createdInputs["localtimeConfig"] as LuaScriptConfigurationInput.LocalTime
        assertSame(allTypesMetadata.config[6].name, localtimeInput.name)
        assertEquals(15, localtimeInput.time.value.hour)  // 930 minutes = 15 hours 30 minutes
        assertEquals(30, localtimeInput.time.value.minute)

        val instantInput = createdInputs["instantConfig"] as LuaScriptConfigurationInput.Instant
        assertSame(allTypesMetadata.config[7].name, instantInput.name)
        assertEquals(1687012200000L, instantInput.dateTime.value.toInstant().toEpochMilli())  // Should use saved value

        // CRITICAL: Ensure all sealed class types are tested
        // This assertion will fail if a new LuaFunctionConfigSpec type is added but not included in allTypesMetadata
        val testedConfigTypes = allTypesMetadata.config.map { it::class }.toSet()
        val allConfigTypes = LuaFunctionConfigSpec::class.sealedSubclasses.toSet()
        assertEquals(
            "All LuaFunctionConfigSpec sealed class types must be tested in allTypesMetadata. " +
                    "Missing types: ${allConfigTypes - testedConfigTypes}. " +
                    "If you added a new type, update allTypesMetadata to include it.",
            allConfigTypes, testedConfigTypes
        )

        // Also verify all input types are created
        val createdInputTypes = createdInputs.values.map { it::class }.toSet()
        val allInputTypes = LuaScriptConfigurationInput::class.sealedSubclasses.toSet()
        assertEquals(
            "All LuaScriptConfigurationInput sealed class types should be created. " +
                    "Missing types: ${allInputTypes - createdInputTypes}",
            allInputTypes, createdInputTypes
        )
    }

    @Test
    fun `createConfigurationInput creates Text input with default value when no saved value`() {
        // Given
        val config = LuaFunctionConfigSpec.Text(
            id = "textConfig",
            name = TranslatedString.Simple("Text Config"),
            defaultValue = "default text"
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Text input", result is LuaScriptConfigurationInput.Text)
        val textInput = result as LuaScriptConfigurationInput.Text
        assertEquals("Should use default value", "default text", textInput.value.value.text)
        assertSame("Should preserve name", config.name, textInput.name)
    }

    @Test
    fun `createConfigurationInput creates Text input with empty default when no default specified`() {
        // Given
        val config = LuaFunctionConfigSpec.Text(
            id = "textConfig",
            name = TranslatedString.Simple("Text Config"),
            defaultValue = null
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Text input", result is LuaScriptConfigurationInput.Text)
        val textInput = result as LuaScriptConfigurationInput.Text
        assertEquals("Should use empty default", "", textInput.value.value.text)
    }

    @Test
    fun `createConfigurationInput creates Text input with saved value when provided`() {
        // Given
        val config = LuaFunctionConfigSpec.Text(
            id = "textConfig",
            name = TranslatedString.Simple("Text Config"),
            defaultValue = "default text"
        )
        val savedValue = LuaScriptConfigurationValue.Text(
            id = "textConfig",
            value = "saved text"
        )

        // When
        val result = factory.createConfigurationInput(config, savedValue)

        // Then
        assertTrue("Should create Text input", result is LuaScriptConfigurationInput.Text)
        val textInput = result as LuaScriptConfigurationInput.Text
        assertEquals("Should use saved value", "saved text", textInput.value.value.text)
    }

    @Test
    fun `createConfigurationInput creates Number input with default value when no saved value`() {
        // Given
        val config = LuaFunctionConfigSpec.Number(
            id = "numberConfig",
            name = TranslatedString.Simple("Number Config"),
            defaultValue = 42.5
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Number input", result is LuaScriptConfigurationInput.Number)
        val numberInput = result as LuaScriptConfigurationInput.Number
        assertEquals("Should use default value", "42.5", numberInput.value.value.text)
        assertSame("Should preserve name", config.name, numberInput.name)
    }

    @Test
    fun `createConfigurationInput creates Number input with zero default when no default specified`() {
        // Given
        val config = LuaFunctionConfigSpec.Number(
            id = "numberConfig",
            name = TranslatedString.Simple("Number Config"),
            defaultValue = null
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Number input", result is LuaScriptConfigurationInput.Number)
        val numberInput = result as LuaScriptConfigurationInput.Number
        assertEquals("Should use zero default", "1.0", numberInput.value.value.text)
    }

    @Test
    fun `createConfigurationInput creates Number input with saved value when provided`() {
        // Given
        val config = LuaFunctionConfigSpec.Number(
            id = "numberConfig",
            name = TranslatedString.Simple("Number Config"),
            defaultValue = 42.5
        )
        val savedValue = LuaScriptConfigurationValue.Number(
            id = "numberConfig",
            value = 123.45
        )

        // When
        val result = factory.createConfigurationInput(config, savedValue)

        // Then
        assertTrue("Should create Number input", result is LuaScriptConfigurationInput.Number)
        val numberInput = result as LuaScriptConfigurationInput.Number
        assertEquals("Should use saved value", "123.45", numberInput.value.value.text)
    }

    @Test
    fun `createConfigurationInput creates Checkbox input with default value when no saved value`() {
        // Given
        val config = LuaFunctionConfigSpec.Checkbox(
            id = "checkboxConfig",
            name = TranslatedString.Simple("Checkbox Config"),
            defaultValue = true
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Checkbox input", result is LuaScriptConfigurationInput.Checkbox)
        val checkboxInput = result as LuaScriptConfigurationInput.Checkbox
        assertEquals("Should use default value", true, checkboxInput.value.value)
        assertSame("Should preserve name", config.name, checkboxInput.name)
    }

    @Test
    fun `createConfigurationInput creates Checkbox input with false default when no default specified`() {
        // Given
        val config = LuaFunctionConfigSpec.Checkbox(
            id = "checkboxConfig",
            name = TranslatedString.Simple("Checkbox Config"),
            defaultValue = null
        )

        // When
        val result = factory.createConfigurationInput(config)

        // Then
        assertTrue("Should create Checkbox input", result is LuaScriptConfigurationInput.Checkbox)
        val checkboxInput = result as LuaScriptConfigurationInput.Checkbox
        assertEquals("Should use false default", false, checkboxInput.value.value)
    }

    @Test
    fun `createConfigurationInput creates Checkbox input with saved value when provided`() {
        // Given
        val config = LuaFunctionConfigSpec.Checkbox(
            id = "checkboxConfig",
            name = TranslatedString.Simple("Checkbox Config"),
            defaultValue = false
        )
        val savedValue = LuaScriptConfigurationValue.Checkbox(
            id = "checkboxConfig",
            value = true
        )

        // When
        val result = factory.createConfigurationInput(config, savedValue)

        // Then
        assertTrue("Should create Checkbox input", result is LuaScriptConfigurationInput.Checkbox)
        val checkboxInput = result as LuaScriptConfigurationInput.Checkbox
        assertEquals("Should use saved value", true, checkboxInput.value.value)
    }

    @Test
    fun `createOrRecoverInput returns existing input when compatible`() {
        // Given
        val existingInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test"),
            value = mutableStateOf(TextFieldValue("existing"))
        )
        val config = LuaFunctionConfigSpec.Text(
            id = "test",
            name = TranslatedString.Simple("Test"),
            defaultValue = "default"
        )

        // When
        val result = factory.createOrRecoverInput(config, existingInput)

        // Then
        assertSame("Should return existing input when compatible", existingInput, result)
    }

    @Test
    fun `createOrRecoverInput creates new input when incompatible`() {
        // Given
        val existingInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test"),
            value = mutableStateOf(TextFieldValue("existing"))
        )
        val config = LuaFunctionConfigSpec.Number(
            id = "test",
            name = TranslatedString.Simple("Test"),
            defaultValue = 42.0
        )

        // When
        val result = factory.createOrRecoverInput(config, existingInput)

        // Then
        assertNotSame("Should create new input when incompatible", existingInput, result)
        assertTrue("Should create Number input", result is LuaScriptConfigurationInput.Number)
        val numberInput = result as LuaScriptConfigurationInput.Number
        assertEquals("Should use default value", "42.0", numberInput.value.value.text)
    }

    @Test
    fun `createOrRecoverInput creates new input when existing is null`() {
        // Given
        val config = LuaFunctionConfigSpec.Text(
            id = "test",
            name = TranslatedString.Simple("Test"),
            defaultValue = "default"
        )

        // When
        val result = factory.createOrRecoverInput(config, null)

        // Then
        assertTrue("Should create Text input", result is LuaScriptConfigurationInput.Text)
        val textInput = result as LuaScriptConfigurationInput.Text
        assertEquals("Should use default value", "default", textInput.value.value.text)
    }
}
