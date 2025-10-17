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
import com.samco.trackandgraph.data.lua.TestLuaVMFixtures
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LuaScriptNodeProviderTest {

    private val mockLuaEngine: LuaEngine = mock()
    private val provider = LuaScriptNodeProvider(
        mockLuaEngine,
        LuaScriptConfigurationInputFactory()
    )
    private val testVmLock = TestLuaVMFixtures.createTestLuaVMLock()

    @Before
    fun setup() {
        runBlocking {
            whenever(mockLuaEngine.acquireVM()).thenReturn(testVmLock)
        }
    }

    @Test
    fun `createLuaScriptNode with empty config creates node with empty configuration`() = runTest {
        // Given - test with translations to verify they're passed through
        val translations = mapOf(
            "_test_key" to TranslatedString.Translations(mapOf("en" to "Test"))
        )
        val script = "simple script"
        val nodeId = 456
        val metadata = LuaFunctionMetadata(
            script = script,
            id = null,
            inputCount = 1,
            config = emptyList(),
            version = null,
            title = null,
            description = null,
            usedTranslations = null
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                vmLock = any(),
                script = eq(script),
                translations = eq(translations)
            )
        ).thenReturn(metadata)

        // When - pass translations
        val result = provider.createLuaScriptNode(
            script = script,
            nodeId = nodeId,
            inputConnectorCount = 1,
            configuration = emptyList(),
            translations = translations
        )

        // Then
        assertEquals(nodeId, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(script, result.script)
        assertTrue(result.configuration.isEmpty())
        assertEquals(true, result.showEditTools)
        assertEquals(null, result.title)
        assertEquals(metadata, result.metadata) // Metadata should be set

        // Verify translations were passed through
        verify(mockLuaEngine).runLuaFunction(
            vmLock = any(),
            script = eq(script),
            translations = eq(translations)
        )
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `createLuaScriptNode with invalid script returns fallback node`() = runTest {
        // Given
        val script = "invalid script"
        val nodeId = 789
        whenever(
            mockLuaEngine.runLuaFunction(
                any(),
                eq(script),
                any()
            )
        ).thenThrow(RuntimeException("Script error"))

        // When
        val result = provider.createLuaScriptNode(script, nodeId, 1, emptyList())

        // Then
        assertEquals(nodeId, result.id)
        assertEquals(1, result.inputConnectorCount) // Fallback value
        assertEquals(script, result.script)
        assertTrue(result.configuration.isEmpty())

        verify(mockLuaEngine).runLuaFunction(any(), eq(script), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode preserves existing configuration with same type`() = runTest {
        // Given - node with metadata that has translations
        val existingTranslations = mapOf(
            "_old_key" to TranslatedString.Translations(mapOf("en" to "Old Translation"))
        )
        val existingMetadata = LuaFunctionMetadata(
            script = "old script",
            id = null,
            inputCount = 1,
            config = emptyList(),
            version = null,
            title = null,
            description = null,
            usedTranslations = existingTranslations
        )
        val existingTextInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test Config"),
            value = mutableStateOf(TextFieldValue("existing value"))
        )
        val existingNode = Node.LuaScript(
            id = 100,
            inputConnectorCount = 1,
            script = "old script",
            configuration = mapOf("config1" to existingTextInput),
            metadata = existingMetadata
        )

        val newScript = "new script"
        val newMetadata = LuaFunctionMetadata(
            script = newScript,
            id = null,
            inputCount = 2,
            config = listOf(
                LuaFunctionConfigSpec.Text(
                    id = "config1",
                    name = TranslatedString.Simple("Config 1")
                )
            ),
            version = Version(1, 0, 0),
            title = null,
            description = null,
            usedTranslations = null
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                any(),
                eq(newScript),
                eq(existingTranslations)
            )
        ).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(100, result.id)
        assertEquals(2, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        assertEquals(false, result.showEditTools)
        assertEquals(null, result.title)
        assertEquals(newMetadata, result.metadata) // Metadata should be set

        // Should preserve the existing input with its value
        val preservedInput = result.configuration["config1"] as LuaScriptConfigurationInput.Text
        assertEquals("existing value", preservedInput.value.value.text)
        assertSame(existingTextInput, preservedInput) // Should be the exact same instance

        // Verify translations were passed through from existing metadata
        verify(mockLuaEngine).runLuaFunction(
            vmLock = any(),
            script = eq(newScript),
            translations = eq(existingTranslations)
        )
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode adds new configuration inputs`() = runTest {
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
            id = null,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfigSpec.Text(
                    id = "newConfig",
                    name = TranslatedString.Simple("New Config")
                )
            ),
            version = null,
            title = TranslatedString.Simple("New Script"),
            description = null,
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                vmLock = any(),
                script = eq(newScript),
                translations = anyOrNull()
            )
        ).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(200, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        assertEquals(true, result.showEditTools)
        assertEquals(TranslatedString.Simple("New Script"), result.title)

        assertTrue(result.configuration["newConfig"] is LuaScriptConfigurationInput.Text)
        val newInput = result.configuration["newConfig"] as LuaScriptConfigurationInput.Text
        assertEquals("", newInput.value.value.text) // New input should have empty default value

        verify(mockLuaEngine).runLuaFunction(any(), eq(newScript), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode removes old configuration inputs`() = runTest {
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
            id = null,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfigSpec.Text(
                    id = "config1",
                    name = TranslatedString.Simple("Config 1")
                )
            ),
            version = null,
            title = null,
            description = null,
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                vmLock = any(),
                script = eq(newScript),
                translations = anyOrNull()
            )
        ).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(300, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        assertEquals(true, result.showEditTools)
        assertEquals(null, result.title)

        // Should preserve config1 and remove config2
        assertTrue(result.configuration.containsKey("config1"))
        assertFalse(result.configuration.containsKey("config2"))
        assertSame(existingInput1, result.configuration["config1"])

        verify(mockLuaEngine).runLuaFunction(any(), eq(newScript), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode creates new input when type changes`() = runTest {
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
            id = null,
            inputCount = 1,
            config = listOf(
                LuaFunctionConfigSpec.Number( // Changed type from TEXT to NUMBER
                    id = "config1",
                    name = TranslatedString.Simple("Config 1")
                )
            ),
            version = null,
            title = null,
            description = null,
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                vmLock = any(),
                script = eq(newScript),
                translations = anyOrNull()
            )
        ).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(400, result.id)
        assertEquals(1, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(1, result.configuration.size)
        assertEquals(true, result.showEditTools)
        assertEquals(null, result.title)

        // Should create a new input since type changed from TEXT to NUMBER
        val newInput = result.configuration["config1"]
        assertTrue(newInput is LuaScriptConfigurationInput.Number)
        assertNotSame(existingTextInput, newInput) // Should be a different instance

        verify(mockLuaEngine).runLuaFunction(any(), eq(newScript), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode with invalid script returns fallback node`() = runTest {
        // Given
        val existingInput = LuaScriptConfigurationInput.Text(
            name = TranslatedString.Simple("Test Input"),
            value = mutableStateOf(TextFieldValue("existing value"))
        )
        val existingConfiguration = mapOf("config1" to existingInput)
        val existingNode = Node.LuaScript(
            id = 500,
            inputConnectorCount = 2,
            script = "old script",
            configuration = existingConfiguration,
            showEditTools = false,
            title = TranslatedString.Simple("Old Script"),
        )

        val newScript = "invalid script"
        whenever(
            mockLuaEngine.runLuaFunction(
                any(),
                eq(newScript),
                any()
            )
        ).thenThrow(RuntimeException("Script error"))

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(500, result.id)
        assertEquals(1, result.inputConnectorCount) // Fallback value
        assertEquals(newScript, result.script)
        assertEquals(existingConfiguration, result.configuration)
        assertEquals(false, result.showEditTools)
        assertEquals(TranslatedString.Simple("Old Script"), result.title)

        verify(mockLuaEngine).runLuaFunction(any(), eq(newScript), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

    @Test
    fun `updateLuaScriptNode handles complex configuration changes`() = runTest {
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
            id = null,
            inputCount = 3,
            config = listOf(
                LuaFunctionConfigSpec.Text(
                    id = "keep",
                    name = TranslatedString.Simple("Keep Config")
                ),
                LuaFunctionConfigSpec.Text(
                    id = "change",
                    name = TranslatedString.Simple("Changed Config")
                ),
                LuaFunctionConfigSpec.Text(
                    id = "new",
                    name = TranslatedString.Simple("New Config")
                )
            ),
            version = Version(1, 0, 0),
            title = TranslatedString.Simple("New Script"),
            description = null,
        )
        whenever(
            mockLuaEngine.runLuaFunction(
                vmLock = any(),
                script = eq(newScript),
                translations = anyOrNull()
            )
        ).thenReturn(newMetadata)

        // When
        val result = provider.updateLuaScriptNode(existingNode, newScript)

        // Then
        assertEquals(600, result.id)
        assertEquals(3, result.inputConnectorCount)
        assertEquals(newScript, result.script)
        assertEquals(3, result.configuration.size)
        assertEquals(false, result.showEditTools)
        assertEquals(TranslatedString.Simple("New Script"), result.title)

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

        verify(mockLuaEngine).runLuaFunction(any(), eq(newScript), anyOrNull())
        verify(mockLuaEngine).releaseVM(testVmLock)
    }

}
