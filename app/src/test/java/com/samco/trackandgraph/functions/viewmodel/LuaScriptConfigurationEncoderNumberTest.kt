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
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationEncoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LuaScriptConfigurationEncoderNumberTest {

    private lateinit var encoder: LuaScriptConfigurationEncoder
    
    // Test data declarations
    private val validNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Number Config"),
        value = mutableStateOf(TextFieldValue("42.5"))
    )
    
    private val invalidNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Invalid Number Config"),
        value = mutableStateOf(TextFieldValue("invalid number"))
    )
    
    private val emptyNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Empty Number Config"),
        value = mutableStateOf(TextFieldValue(""))
    )
    
    private val integerNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Integer Number Config"),
        value = mutableStateOf(TextFieldValue("123"))
    )
    
    private val negativeNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Negative Number Config"),
        value = mutableStateOf(TextFieldValue("-456.789"))
    )
    
    private val zeroNumberInput = LuaScriptConfigurationInput.Number(
        name = TranslatedString.Simple("Zero Number Config"),
        value = mutableStateOf(TextFieldValue("0"))
    )

    @Before
    fun setUp() {
        encoder = LuaScriptConfigurationEncoder()
    }

    @Test
    fun `encodeConfiguration handles number configuration correctly`() {
        // Given
        val configuration = mapOf("numberConfig" to validNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0]
        assertTrue("Should be Number configuration", encodedValue is LuaScriptConfigurationValue.Number)
        
        val numberValue = encodedValue as LuaScriptConfigurationValue.Number
        assertEquals("ID should match", "numberConfig", numberValue.id)
        assertEquals("Value should match", 42.5, numberValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles invalid number gracefully`() {
        // Given
        val configuration = mapOf("numberConfig" to invalidNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Number
        assertEquals("Invalid number should default to 1.0", 1.0, encodedValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles empty number string gracefully`() {
        // Given
        val configuration = mapOf("numberConfig" to emptyNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Number
        assertEquals("Empty number should default to 1.0", 1.0, encodedValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles integer numbers correctly`() {
        // Given
        val configuration = mapOf("integerConfig" to integerNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Number
        assertEquals("Integer should be converted to double", 123.0, encodedValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles negative numbers correctly`() {
        // Given
        val configuration = mapOf("negativeConfig" to negativeNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Number
        assertEquals("Negative number should be preserved", -456.789, encodedValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles zero correctly`() {
        // Given
        val configuration = mapOf("zeroConfig" to zeroNumberInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Number
        assertEquals("Zero should be preserved", 0.0, encodedValue.value, 0.0001)
    }

    @Test
    fun `encodeConfiguration handles multiple number configurations`() {
        // Given
        val configuration = mapOf(
            "valid" to validNumberInput,
            "invalid" to invalidNumberInput,
            "empty" to emptyNumberInput,
            "integer" to integerNumberInput,
            "negative" to negativeNumberInput,
            "zero" to zeroNumberInput
        )

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce six configuration values", 6, result.size)
        
        // Verify all are number configurations
        result.forEach { encodedValue ->
            assertTrue("All should be Number configurations", encodedValue is LuaScriptConfigurationValue.Number)
        }
        
        // Verify specific values
        val validResult = result.find { it.id == "valid" } as LuaScriptConfigurationValue.Number
        assertEquals("Valid number should match", 42.5, validResult.value, 0.0001)
        
        val invalidResult = result.find { it.id == "invalid" } as LuaScriptConfigurationValue.Number
        assertEquals("Invalid number should default to 1.0", 1.0, invalidResult.value, 0.0001)
        
        val emptyResult = result.find { it.id == "empty" } as LuaScriptConfigurationValue.Number
        assertEquals("Empty number should default to 1.0", 1.0, emptyResult.value, 0.0001)
        
        val integerResult = result.find { it.id == "integer" } as LuaScriptConfigurationValue.Number
        assertEquals("Integer should be converted", 123.0, integerResult.value, 0.0001)
        
        val negativeResult = result.find { it.id == "negative" } as LuaScriptConfigurationValue.Number
        assertEquals("Negative should be preserved", -456.789, negativeResult.value, 0.0001)
        
        val zeroResult = result.find { it.id == "zero" } as LuaScriptConfigurationValue.Number
        assertEquals("Zero should be preserved", 0.0, zeroResult.value, 0.0001)
    }
}
