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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

internal class LuaFunctionMetadataTests : LuaEngineImplTest() {

    @Test
    fun `Basic function returns default metadata`() {
        val script = """
            return function(data_sources)
                local source = data_sources[1]
                local data_point = source.dp()
                while data_point do
                    coroutine.yield(data_point)
                    data_point = source.dp()
                end
            end
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(1, metadata.inputCount)
            assertTrue("Config should be empty for basic function", metadata.config.isEmpty())
            assertEquals("Script should be preserved", script, metadata.script)
        }
    }

    @Test
    fun `Function with explicit inputCount`() {
        val script = """
            return {
                inputCount = 3,
                generator = function(data_sources)
                    local source1 = data_sources[1]
                    local source2 = data_sources[2]
                    local source3 = data_sources[3]
                    -- Process multiple sources
                    local dp1 = source1.dp()
                    if dp1 then
                        coroutine.yield(dp1)
                    end
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(3, metadata.inputCount)
            assertTrue("Config should be empty", metadata.config.isEmpty())
            assertEquals("Script should be preserved", script, metadata.script)
        }
    }

    @Test
    fun `Function with text config using string name`() {
        val script = """
            return {
                inputCount = 2,
                config = {
                    {
                        id = "threshold",
                        type = "text",
                        name = "Threshold Value"
                    }
                },
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(2, metadata.inputCount)
            assertEquals(1, metadata.config.size)
            assertEquals("Script should be preserved", script, metadata.script)

            val config = metadata.config[0]
            assertEquals("threshold", config.id)
            assertEquals(LuaFunctionConfigType.TEXT, config.type)
            assertTrue("Name should be a simple string", config.name is TranslatedString.Simple)
            assertEquals("Threshold Value", (config.name as TranslatedString.Simple).value)
        }
    }

    @Test
    fun `Function with text config using translation table`() {
        val script = """
            return {
                config = {
                    {
                        id = "multiplier",
                        type = "text",
                        name = {
                            en = "Multiplier",
                            es = "Multiplicador",
                            fr = "Multiplicateur"
                        }
                    }
                },
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(1, metadata.inputCount) // Should default to 1
            assertEquals(1, metadata.config.size)
            assertEquals("Script should be preserved", script, metadata.script)

            val config = metadata.config[0]
            assertEquals("multiplier", config.id)
            assertEquals(LuaFunctionConfigType.TEXT, config.type)
            assertTrue("Name should be translations", config.name is TranslatedString.Translations)
            val translations = (config.name as TranslatedString.Translations).values
            assertEquals(3, translations.size)
            assertEquals("Multiplier", translations["en"])
            assertEquals("Multiplicador", translations["es"])
            assertEquals("Multiplicateur", translations["fr"])
        }
    }

    @Test
    fun `Function with multiple config items`() {
        val script = """
            return {
                inputCount = 1,
                config = {
                    {
                        id = "threshold",
                        type = "text",
                        name = "Threshold"
                    },
                    {
                        id = "label",
                        type = "text",
                        name = {
                            ["en-GB"] = "Label",
                            de = "Bezeichnung"
                        }
                    }
                },
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(1, metadata.inputCount)
            assertEquals(2, metadata.config.size)
            assertEquals("Script should be preserved", script, metadata.script)

            val thresholdConfig = metadata.config[0]
            assertEquals("threshold", thresholdConfig.id)
            assertEquals(LuaFunctionConfigType.TEXT, thresholdConfig.type)
            assertTrue("Threshold name should be a simple string", thresholdConfig.name is TranslatedString.Simple)
            assertEquals("Threshold", (thresholdConfig.name as TranslatedString.Simple).value)

            val labelConfig = metadata.config[1]
            assertEquals("label", labelConfig.id)
            assertEquals(LuaFunctionConfigType.TEXT, labelConfig.type)
            assertTrue("Label name should be translations", labelConfig.name is TranslatedString.Translations)
            val labelTranslations = (labelConfig.name as TranslatedString.Translations).values
            assertEquals("Label", labelTranslations["en-GB"])
            assertEquals("Bezeichnung", labelTranslations["de"])
        }
    }

    @Test
    fun `Function with config but no name field`() {
        val script = """
            return {
                config = {
                    {
                        id = "value",
                        type = "text"
                        -- No name field
                    }
                },
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(1, metadata.inputCount)
            assertEquals(1, metadata.config.size)
            assertEquals("Script should be preserved", script, metadata.script)

            val config = metadata.config[0]
            assertEquals("value", config.id)
            assertEquals(LuaFunctionConfigType.TEXT, config.type)
            assertEquals("Name should be null when not provided", null, config.name)
        }
    }

    @Test
    fun `Function table with no inputCount defaults to 1`() {
        val script = """
            return {
                -- No inputCount specified
                generator = function(data_sources)
                    local source = data_sources[1]
                    local data_point = source.dp()
                    if data_point then
                        coroutine.yield(data_point)
                    end
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(1, metadata.inputCount)
            assertTrue("Config should be empty", metadata.config.isEmpty())
            assertEquals("Script should be preserved", script, metadata.script)
        }
    }

    @Test
    fun `Function table with no config defaults to empty list`() {
        val script = """
            return {
                inputCount = 2,
                -- No config specified
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(2, metadata.inputCount)
            assertTrue("Config should be empty when not specified", metadata.config.isEmpty())
            assertEquals("Script should be preserved", script, metadata.script)
        }
    }
}
