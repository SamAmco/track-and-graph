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
package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigurationValueParserTest {

    private lateinit var parser: ConfigurationValueParser

    @Before
    fun setUp() {
        parser = ConfigurationValueParser()
    }

    @Test
    fun `parseConfigurationValues handles special number values`() {
        // Given
        val zeroConfig = LuaScriptConfigurationValue.Number(id = "zero", value = 0.0)
        val negativeConfig = LuaScriptConfigurationValue.Number(id = "negative", value = -456.789)
        val largeConfig = LuaScriptConfigurationValue.Number(id = "large", value = 1e6)
        val configuration = listOf(zeroConfig, negativeConfig, largeConfig)

        // When
        val result = parser.parseConfigurationValues(configuration)

        // Then
        assertTrue("Should have zero key", !result["zero"].isnil())
        assertTrue("Should have negative key", !result["negative"].isnil())
        assertTrue("Should have large key", !result["large"].isnil())
        
        assertEquals("Zero should be preserved", 0.0, result["zero"].todouble(), 0.0001)
        assertEquals("Negative should be preserved", -456.789, result["negative"].todouble(), 0.0001)
        assertEquals("Large number should be preserved", 1e6, result["large"].todouble(), 0.0001)
    }

    @Test
    fun `parseConfigurationValues handles special text values`() {
        // Given
        val emptyConfig = LuaScriptConfigurationValue.Text(id = "empty", value = "")
        val multilineConfig = LuaScriptConfigurationValue.Text(id = "multiline", value = "Line 1\nLine 2\nLine 3")
        val unicodeConfig = LuaScriptConfigurationValue.Text(id = "unicode", value = "Hello 世界 🌍")
        val configuration = listOf(emptyConfig, multilineConfig, unicodeConfig)

        // When
        val result = parser.parseConfigurationValues(configuration)

        // Then
        assertTrue("Should have empty key", !result["empty"].isnil())
        assertTrue("Should have multiline key", !result["multiline"].isnil())
        assertTrue("Should have unicode key", !result["unicode"].isnil())
        
        assertEquals("Empty string should be preserved", "", result["empty"].tojstring())
        assertEquals("Multiline should be preserved", "Line 1\nLine 2\nLine 3", result["multiline"].tojstring())
        assertEquals("Unicode should be preserved", "Hello 世界 🌍", result["unicode"].tojstring())
    }

    @Test
    fun `parseConfigurationValues handles duplicate IDs by using last value`() {
        // Given - Two configurations with same ID
        val firstConfig = LuaScriptConfigurationValue.Text(id = "param", value = "first")
        val secondConfig = LuaScriptConfigurationValue.Number(id = "param", value = 42.0)
        val configuration = listOf(firstConfig, secondConfig)

        // When
        val result = parser.parseConfigurationValues(configuration)

        // Then
        val luaValue = result["param"]
        assertTrue("Should have param key", !luaValue.isnil())
        assertTrue("Should be the number value (last one)", luaValue.isnumber())
        assertEquals("Should have the number value", 42.0, luaValue.todouble(), 0.0001)
    }

    @Test
    fun `parseConfigurationValues covers all LuaFunctionConfigType enum values`() {
        // Given - Create configuration for all enum types
        val textConfig = LuaScriptConfigurationValue.Text(id = "text", value = "test")
        val numberConfig = LuaScriptConfigurationValue.Number(id = "number", value = 1.0)
        val checkboxConfig = LuaScriptConfigurationValue.Checkbox(id = "checkbox", value = true)
        val configuration = listOf(textConfig, numberConfig, checkboxConfig)

        // When
        val result = parser.parseConfigurationValues(configuration)

        // Then - Verify all enum types are covered
        val configTypes = configuration.map { it.type }.toSet()
        val allEnumTypes = LuaFunctionConfigType.entries.toSet()
        
        assertEquals("Test should cover all LuaFunctionConfigType enum values", allEnumTypes, configTypes)
        
        // Verify all keys exist in the result
        assertTrue("Should have text key", !result["text"].isnil())
        assertTrue("Should have number key", !result["number"].isnil())
        assertTrue("Should have checkbox key", !result["checkbox"].isnil())
        assertTrue("Text should be string", result["text"].isstring())
        assertTrue("Number should be number", result["number"].isnumber())
        assertTrue("Checkbox should be boolean", result["checkbox"].isboolean())
        assertEquals("Checkbox should have correct value", true, result["checkbox"].toboolean())
    }
}
