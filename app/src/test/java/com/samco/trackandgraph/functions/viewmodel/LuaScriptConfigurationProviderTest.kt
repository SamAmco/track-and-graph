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
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfig
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LuaScriptConfigurationProviderTest {

    private val mockLuaEngine: LuaEngine = mock()
    private val provider = LuaScriptConfigurationProvider(mockLuaEngine)
    
    // Comprehensive metadata containing all possible configuration types
    // This ensures we test all enum values and catch missing implementations
    private val allTypesMetadata = LuaFunctionMetadata(
        script = "comprehensive test script",
        inputCount = 3,
        config = listOf(
            LuaFunctionConfig(
                id = "textConfig",
                type = LuaFunctionConfigType.TEXT,
                name = TranslatedString.Simple("Text Configuration")
            ),
            LuaFunctionConfig(
                id = "numberConfig", 
                type = LuaFunctionConfigType.NUMBER,
                name = TranslatedString.Simple("Number Configuration")
            )
        )
    )

    @Test
    fun `createLuaScriptNode handles all configuration types correctly and covers all enum values`() {
        // Given
        val script = "comprehensive script"
        val nodeId = 999
        whenever(mockLuaEngine.runLuaFunction(script)).thenReturn(allTypesMetadata)

        // When
        val result = provider.createLuaScriptNode(script, nodeId)

        // Then - Basic node properties
        assertEquals(nodeId, result.id)
        assertEquals(3, result.inputConnectorCount)
        assertEquals(script, result.script)
        assertEquals(2, result.configuration.size)

        // Then - Validate each configuration type is created correctly
        val textConfig = result.configuration["textConfig"]
        assertTrue("Text configuration should be created as LuaScriptConfigurationInput.Text",
            textConfig is LuaScriptConfigurationInput.Text)
        assertSame(allTypesMetadata.config[0].name, textConfig?.name)

        val numberConfig = result.configuration["numberConfig"]
        assertTrue("Number configuration should be created as LuaScriptConfigurationInput.Number",
            numberConfig is LuaScriptConfigurationInput.Number)
        assertSame(allTypesMetadata.config[1].name, numberConfig?.name)

        // CRITICAL: Ensure all enum values are tested
        // This assertion will fail if a new LuaFunctionConfigType is added but not included in allTypesMetadata
        val testedTypes = allTypesMetadata.config.map { it.type }.toSet()
        val allEnumValues = LuaFunctionConfigType.entries.toSet()
        assertEquals("All LuaFunctionConfigType enum values must be tested in allTypesMetadata. " +
                "Missing types: ${allEnumValues - testedTypes}. " +
                "If you added a new type, update allTypesMetadata to include it.",
            allEnumValues, testedTypes)

        verify(mockLuaEngine).runLuaFunction(script)
    }

    @Test
    fun `createLuaScriptNode with valid script creates node with correct configuration`() {
        // Given
        val script = "valid lua script"
        val nodeId = 123
        val metadata = LuaFunctionMetadata(
            script = script,
            inputCount = 2,
            config = listOf(
                LuaFunctionConfig(
                    id = "config1",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Config 1")
                ),
                LuaFunctionConfig(
                    id = "config2", 
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Config 2")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(script)).thenReturn(metadata)

        // When
        val result = provider.createLuaScriptNode(script, nodeId)

        // Then
        assertEquals(nodeId, result.id)
        assertEquals(2, result.inputConnectorCount)
        assertEquals(script, result.script)
        assertEquals(2, result.configuration.size)
        
        assertTrue(result.configuration["config1"] is LuaScriptConfigurationInput.Text)
        assertTrue(result.configuration["config2"] is LuaScriptConfigurationInput.Text)
        
        verify(mockLuaEngine).runLuaFunction(script)
    }

    @Test
    fun `createLuaScriptNode with empty config creates node with empty configuration`() {
        // Given
        val script = "simple script"
        val nodeId = 456
        val metadata = LuaFunctionMetadata(
            script = script,
            inputCount = 1,
            config = emptyList()
        )
        whenever(mockLuaEngine.runLuaFunction(script)).thenReturn(metadata)

        // When
        val result = provider.createLuaScriptNode(script, nodeId)

        // Then
        assertEquals(nodeId, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(script, result.script)
        assertTrue(result.configuration.isEmpty())
        
        verify(mockLuaEngine).runLuaFunction(script)
    }

    @Test
    fun `createLuaScriptNode with invalid script returns fallback node`() {
        // Given
        val script = "invalid script"
        val nodeId = 789
        whenever(mockLuaEngine.runLuaFunction(script)).thenThrow(RuntimeException("Script error"))

        // When
        val result = provider.createLuaScriptNode(script, nodeId)

        // Then
        assertEquals(nodeId, result.id)
        assertEquals(1, result.inputConnectorCount) // Fallback value
        assertEquals(script, result.script)
        assertTrue(result.configuration.isEmpty())
        
        verify(mockLuaEngine).runLuaFunction(script)
    }

    @Test
    fun `updateLuaScriptNode preserves existing configuration with same type`() {
        // Given
        val existingTextInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test Config"),
            value = mutableStateOf(TextFieldValue("existing value"))
        )
        val existingNode = Node.LuaScript(
            id = 100,
            inputConnectorCount = 1,
            script = "old script",
            configuration = mapOf("config1" to existingTextInput)
        )
        
        val newScript = "new script"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            inputCount = 2,
            config = listOf(
                LuaFunctionConfig(
                    id = "config1",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Config 1")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(100, result.id)
        assertEquals(2, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        
        // Should preserve the existing input with its value
        val preservedInput = result.configuration["config1"] as LuaScriptConfigurationInput.Text
        assertEquals("existing value", preservedInput.value.value.text)
        assertSame(existingTextInput, preservedInput) // Should be the exact same instance
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

    @Test
    fun `updateLuaScriptNode adds new configuration inputs`() {
        // Given
        val existingNode = Node.LuaScript(
            id = 200,
            inputConnectorCount = 1,
            script = "old script",
            configuration = emptyMap()
        )
        
        val newScript = "new script with config"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfig(
                    id = "newConfig",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("New Config")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(200, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        
        assertTrue(result.configuration["newConfig"] is LuaScriptConfigurationInput.Text)
        val newInput = result.configuration["newConfig"] as LuaScriptConfigurationInput.Text
        assertEquals("", newInput.value.value.text) // New input should have empty default value
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

    @Test
    fun `updateLuaScriptNode removes old configuration inputs`() {
        // Given
        val existingInput1 = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Config 1"),
            value = mutableStateOf(TextFieldValue("value1"))
        )
        val existingInput2 = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Config 2"),
            value = mutableStateOf(TextFieldValue("value2"))
        )
        val existingNode = Node.LuaScript(
            id = 300,
            inputConnectorCount = 2,
            script = "old script",
            configuration = mapOf(
                "config1" to existingInput1,
                "config2" to existingInput2
            )
        )
        
        val newScript = "new script with only one config"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfig(
                    id = "config1",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Config 1")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(300, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        
        // Should preserve config1 and remove config2
        assertTrue(result.configuration.containsKey("config1"))
        assertFalse(result.configuration.containsKey("config2"))
        assertSame(existingInput1, result.configuration["config1"])
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

    @Test
    fun `updateLuaScriptNode creates new input when type changes`() {
        // Given
        val existingTextInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test Config"),
            value = mutableStateOf(TextFieldValue("existing value"))
        )
        val existingNode = Node.LuaScript(
            id = 400,
            inputConnectorCount = 1,
            script = "old script",
            configuration = mapOf("config1" to existingTextInput)
        )
        
        val newScript = "new script"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfig(
                    id = "config1",
                    type = LuaFunctionConfigType.NUMBER, // Changed type from TEXT to NUMBER
                    name = TranslatedString.Simple("Config 1")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(400, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        
        // Should create a new input since type changed from TEXT to NUMBER
        val newInput = result.configuration["config1"]
        assertTrue(newInput is LuaScriptConfigurationInput.Number)
        assertNotSame(existingTextInput, newInput) // Should be a different instance
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

    @Test
    fun `updateLuaScriptNode with invalid script returns fallback node`() {
        // Given
        val existingInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test Input"),
            value = mutableStateOf(TextFieldValue("existing value"))
        )
        val existingNode = Node.LuaScript(
            id = 500,
            inputConnectorCount = 2,
            script = "old script",
            configuration = mapOf("config1" to existingInput)
        )
        
        val newScript = "invalid script"
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenThrow(RuntimeException("Script error"))

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(500, result.id)
        assertEquals(1, result.inputConnectorCount) // Fallback value
        assertEquals(newScript, result.script)
        assertTrue(result.configuration.isEmpty()) // Configuration cleared on error
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

    @Test
    fun `updateLuaScriptNode handles complex configuration changes`() {
        // Given - existing node with multiple configurations
        val existingInput1 = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Keep Config"),
            value = mutableStateOf(TextFieldValue("value1"))
        )
        val existingInput2 = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Remove Config"),
            value = mutableStateOf(TextFieldValue("value2"))
        )
        val existingInput3 = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Change Config"),
            value = mutableStateOf(TextFieldValue("value3"))
        )
        val existingNode = Node.LuaScript(
            id = 600,
            inputConnectorCount = 2,
            script = "old script",
            configuration = mapOf(
                "keep" to existingInput1,      // This should be preserved
                "remove" to existingInput2,    // This should be removed
                "change" to existingInput3     // This should be preserved
            )
        )
        
        val newScript = "complex new script"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            inputCount = 3,
            config = listOf(
                LuaFunctionConfig(
                    id = "keep",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Keep Config")
                ),
                LuaFunctionConfig(
                    id = "change",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("Changed Config")
                ),
                LuaFunctionConfig(
                    id = "new",
                    type = LuaFunctionConfigType.TEXT,
                    name = TranslatedString.Simple("New Config")
                )
            )
        )
        whenever(mockLuaEngine.runLuaFunction(newScript)).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(600, result.id)
        assertEquals(3, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(3, result.configuration.size)
        
        // Should preserve existing inputs where possible
        assertSame(existingInput1, result.configuration["keep"])
        assertSame(existingInput3, result.configuration["change"])
        
        // Should not contain removed config
        assertFalse(result.configuration.containsKey("remove"))
        
        // Should have new config
        assertTrue(result.configuration.containsKey("new"))
        assertTrue(result.configuration["new"] is LuaScriptConfigurationInput.Text)
        val newInput = result.configuration["new"] as LuaScriptConfigurationInput.Text
        assertEquals("", newInput.value.value.text)
        
        verify(mockLuaEngine).runLuaFunction(newScript)
    }

}
