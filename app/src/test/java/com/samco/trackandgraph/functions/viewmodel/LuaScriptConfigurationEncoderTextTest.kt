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
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationEncoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LuaScriptConfigurationEncoderTextTest {

    private lateinit var encoder: LuaScriptConfigurationEncoder
    
    // Test data declarations
    private val sampleTextInput = LuaScriptConfigurationInput.Text(
        name = TranslatedString.Simple("Text Config"),
        value = mutableStateOf(TextFieldValue("Sample text value"))
    )
    
    private val emptyTextInput = LuaScriptConfigurationInput.Text(
        name = TranslatedString.Simple("Empty Text Config"),
        value = mutableStateOf(TextFieldValue(""))
    )
    
    private val multilineTextInput = LuaScriptConfigurationInput.Text(
        name = TranslatedString.Simple("Multiline Text Config"),
        value = mutableStateOf(TextFieldValue("Line 1\nLine 2\nLine 3"))
    )

    @Before
    fun setUp() {
        encoder = LuaScriptConfigurationEncoder()
    }

    @Test
    fun `encodeConfiguration handles text configuration correctly`() {
        // Given
        val configuration = mapOf("textConfig" to sampleTextInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0]
        assertTrue("Should be Text configuration", encodedValue is LuaScriptConfigurationValue.Text)
        
        val textValue = encodedValue as LuaScriptConfigurationValue.Text
        assertEquals("ID should match", "textConfig", textValue.id)
        assertEquals("Value should match", "Sample text value", textValue.value)
    }

    @Test
    fun `encodeConfiguration handles empty text correctly`() {
        // Given
        val configuration = mapOf("emptyTextConfig" to emptyTextInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Text
        assertEquals("ID should match", "emptyTextConfig", encodedValue.id)
        assertEquals("Empty text should be preserved", "", encodedValue.value)
    }

    @Test
    fun `encodeConfiguration handles multiline text correctly`() {
        // Given
        val configuration = mapOf("multilineTextConfig" to multilineTextInput)

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce one configuration value", 1, result.size)
        val encodedValue = result[0] as LuaScriptConfigurationValue.Text
        assertEquals("ID should match", "multilineTextConfig", encodedValue.id)
        assertEquals("Multiline text should be preserved", "Line 1\nLine 2\nLine 3", encodedValue.value)
    }

    @Test
    fun `encodeConfiguration handles multiple text configurations`() {
        // Given
        val configuration = mapOf(
            "text1" to sampleTextInput,
            "text2" to emptyTextInput,
            "text3" to multilineTextInput
        )

        // When
        val result = encoder.encodeConfiguration(configuration)

        // Then
        assertEquals("Should produce three configuration values", 3, result.size)
        
        // Verify all are text configurations
        result.forEach { encodedValue ->
            assertTrue("All should be Text configurations", encodedValue is LuaScriptConfigurationValue.Text)
        }
        
        // Verify specific values
        val text1 = result.find { it.id == "text1" } as LuaScriptConfigurationValue.Text
        assertEquals("Text1 value should match", "Sample text value", text1.value)
        
        val text2 = result.find { it.id == "text2" } as LuaScriptConfigurationValue.Text
        assertEquals("Text2 value should be empty", "", text2.value)
        
        val text3 = result.find { it.id == "text3" } as LuaScriptConfigurationValue.Text
        assertEquals("Text3 value should be multiline", "Line 1\nLine 2\nLine 3", text3.value)
    }
}
