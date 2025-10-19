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

import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.toVersion
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties

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
            assertTrue("Config should be text type", config is LuaFunctionConfigSpec.Text)
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
            assertTrue("Config should be text type", config is LuaFunctionConfigSpec.Text)
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
            assertTrue("Threshold config should be text type", thresholdConfig is LuaFunctionConfigSpec.Text)
            assertTrue(
                "Threshold name should be a simple string",
                thresholdConfig.name is TranslatedString.Simple
            )
            assertEquals("Threshold", (thresholdConfig.name as TranslatedString.Simple).value)

            val labelConfig = metadata.config[1]
            assertEquals("label", labelConfig.id)
            assertTrue("Label config should be text type", labelConfig is LuaFunctionConfigSpec.Text)
            assertTrue(
                "Label name should be translations",
                labelConfig.name is TranslatedString.Translations
            )
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
            assertTrue("Config should be text type", config is LuaFunctionConfigSpec.Text)
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

    @Test
    fun `Function handles all configuration types and covers all enum values`() {
        val script = """
            return {
                inputCount = 3,
                config = {
                    {
                        id = "textConfig",
                        type = "text",
                        name = "Text Configuration",
                        default = "Default text value"
                    },
                    {
                        id = "numberConfig",
                        type = "number",
                        name = "Number Configuration",
                        default = 123
                    },
                    {
                        id = "checkboxConfig",
                        type = "checkbox",
                        name = "Checkbox Configuration",
                        default = true
                    },
                    {
                        id = "enumConfig",
                        type = "enum",
                        name = "Enum Configuration",
                        options = {
                            {id = "hours", name = {en = "Hours", de = "Stunden"}},
                            {id = "days", name = {en = "Days", de = "Tage"}}
                        },
                        default = "hours"
                    },
                    {
                        id = "uintConfig",
                        type = "uint",
                        name = "UInt Configuration",
                        default = 42
                    },
                    {
                        id = "durationConfig",
                        type = "duration",
                        name = "Duration Configuration",
                        default = 3600000
                    },
                    {
                        id = "localtimeConfig",
                        type = "localtime",
                        name = "LocalTime Configuration",
                        default = 52200000
                    },
                    {
                        id = "instantConfig",
                        type = "instant",
                        name = "Instant Configuration",
                        default = 1686835800000
                    }
                },
                generator = function(data_sources)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script) {
            assertEquals(3, metadata.inputCount)
            assertEquals(8, metadata.config.size)
            assertEquals("Script should be preserved", script, metadata.script)

            // Validate each configuration type is parsed correctly
            val textConfig = metadata.config.find { it.id == "textConfig" } as? LuaFunctionConfigSpec.Text
            assertTrue("Text configuration should be parsed", textConfig != null)
            assertEquals("Text Configuration", (textConfig!!.name as TranslatedString.Simple).value)
            assertEquals("Default text value should be parsed", "Default text value", textConfig.defaultValue)

            val numberConfig = metadata.config.find { it.id == "numberConfig" } as? LuaFunctionConfigSpec.Number
            assertTrue("Number configuration should be parsed", numberConfig != null)
            assertEquals(
                "Number Configuration",
                (numberConfig!!.name as TranslatedString.Simple).value
            )
            assertEquals("Number default should be parsed", 123.0, numberConfig.defaultValue)

            val checkboxConfig = metadata.config.find { it.id == "checkboxConfig" } as? LuaFunctionConfigSpec.Checkbox
            assertTrue("Checkbox configuration should be parsed", checkboxConfig != null)
            assertEquals(
                "Checkbox Configuration",
                (checkboxConfig!!.name as TranslatedString.Simple).value
            )
            assertEquals("Checkbox default should be parsed", true, checkboxConfig.defaultValue)

            val enumConfig = metadata.config.find { it.id == "enumConfig" } as? LuaFunctionConfigSpec.Enum
            assertTrue("Enum configuration should be parsed", enumConfig != null)
            assertEquals(
                "Enum Configuration",
                (enumConfig!!.name as TranslatedString.Simple).value
            )
            assertEquals("Enum default should be parsed", "hours", enumConfig.defaultValue)
            assertEquals("Enum should have 2 options", 2, enumConfig.options.size)
            assertEquals("First option ID should be hours", "hours", enumConfig.options[0].id)
            assertEquals("First option name should be Hours", "Hours",
                (enumConfig.options[0].displayName as TranslatedString.Translations).values["en"])
            assertEquals("Second option ID should be days", "days", enumConfig.options[1].id)
            assertEquals("Second option name should be Days", "Days",
                (enumConfig.options[1].displayName as TranslatedString.Translations).values["en"])

            val uintConfig = metadata.config.find { it.id == "uintConfig" } as? LuaFunctionConfigSpec.UInt
            assertTrue("UInt configuration should be parsed", uintConfig != null)
            assertEquals("UInt Configuration", (uintConfig!!.name as TranslatedString.Simple).value)
            assertEquals("UInt default should be parsed", 42, uintConfig.defaultValue)

            val durationConfig = metadata.config.find { it.id == "durationConfig" } as? LuaFunctionConfigSpec.Duration
            assertTrue("Duration configuration should be parsed", durationConfig != null)
            assertEquals("Duration Configuration", (durationConfig!!.name as TranslatedString.Simple).value)
            // Lua provides 3600000 milliseconds, converted to 3600 seconds for internal storage
            assertEquals("Duration default should be parsed", 3600.0, durationConfig.defaultValueSeconds)

            val localtimeConfig = metadata.config.find { it.id == "localtimeConfig" } as? LuaFunctionConfigSpec.LocalTime
            assertTrue("LocalTime configuration should be parsed", localtimeConfig != null)
            assertEquals("LocalTime Configuration", (localtimeConfig!!.name as TranslatedString.Simple).value)
            // Lua provides 52200000 milliseconds (14.5 hours), converted to 870 minutes for internal storage
            assertEquals("LocalTime default should be parsed", 870, localtimeConfig.defaultValueMinutes)

            val instantConfig = metadata.config.find { it.id == "instantConfig" } as? LuaFunctionConfigSpec.Instant
            assertTrue("Instant configuration should be parsed", instantConfig != null)
            assertEquals("Instant Configuration", (instantConfig!!.name as TranslatedString.Simple).value)
            // Instant stays in epoch milliseconds (no conversion needed)
            assertEquals("Instant default should be parsed", 1686835800000L, instantConfig.defaultValueEpochMilli)

            // CRITICAL: Ensure all sealed class types are tested
            // This assertion will fail if a new LuaFunctionConfigSpec type is added but not included in this test
            val testedTypes = metadata.config.map { it::class }.toSet()
            val allConfigTypes = LuaFunctionConfigSpec::class.sealedSubclasses.toSet()
            assertEquals(
                "All LuaFunctionConfigSpec sealed class types must be tested in this comprehensive test. " +
                        "Missing types: ${allConfigTypes - testedTypes}. " +
                        "If you added a new type, update this test to include it and implement parsing logic in LuaFunctionMetadataAdapter.",
                allConfigTypes, testedTypes
            )
        }
    }

    @Test
    fun `Function handles all metadata fields`() {
        // Create test translations (some will be used, some won't)
        val translations: LocalizationsTable = mapOf(
            "_comprehensive_title" to TranslatedString.Translations(mapOf(
                "en" to "Comprehensive Function",
                "de" to "Umfassende Funktion",
                "es" to "Función Integral",
                "fr" to "Fonction Complète"
            )),
            "_config_name" to TranslatedString.Translations(mapOf(
                "en" to "Sample Configuration",
                "de" to "Beispielkonfiguration",
                "fr" to "Configuration d'Exemple",
                "es" to "Configuración de Muestra"
            )),
            "_hours" to TranslatedString.Translations(mapOf(
                "en" to "Hours",
                "de" to "Stunden",
                "es" to "Horas",
                "fr" to "Heures"
            )),
            "_days" to TranslatedString.Translations(mapOf(
                "en" to "Days",
                "de" to "Tage",
                "es" to "Días",
                "fr" to "Jours"
            )),
            "_unused_key" to TranslatedString.Translations(mapOf(
                "en" to "Unused",
                "de" to "Unbenutzt",
                "es" to "No utilizado",
                "fr" to "Inutilisé"
            )),
            "_test_category" to TranslatedString.Translations(mapOf(
                "en" to "Test Category",
                "de" to "Testkategorie",
                "es" to "Categoría de prueba",
                "fr" to "Catégorie de test"
            ))
        )

        val script = """
            return {
                id = "comprehensive-function",
                version = "2.1.0",
                inputCount = 2,
                categories = {"_test_category"},
                title = "_comprehensive_title",
                description = {
                    ["en"] = "A function that demonstrates all metadata fields",
                    ["de"] = "Eine Funktion, die alle Metadatenfelder demonstriert",
                    ["es"] = "Una función que demuestra todos los campos de metadatos",
                    ["fr"] = "Une fonction qui démontre tous les champs de métadonnées"
                },
                config = {
                    {
                        id = "sampleConfig",
                        type = "text",
                        name = "_config_name"
                    },
                    {
                        id = "enumConfig",
                        type = "enum",
                        name = "Period",
                        options = {"_hours", "_days"},
                        default = "_hours"
                    }
                },
                generator = function(data_sources, config)
                    -- Function implementation
                end
            }
        """.trimIndent()
        testLuaFunctionMetadata(script, translations) {
            // Use reflection to ensure all fields are non-null
            val metadataClass = LuaFunctionMetadata::class
            val properties = metadataClass.memberProperties
            properties.forEach { property ->
                val value = property.get(metadata)
                assertNotNull("Field '${property.name}' should not be null in comprehensive test", value)
            }

            // Test all metadata fields are parsed correctly
            assertEquals("comprehensive-function", metadata.id)
            assertEquals("2.1.0".toVersion(), metadata.version)
            assertEquals(2, metadata.inputCount)
            assertEquals(script, metadata.script)

            // Test title was looked up from translations
            val title = metadata.title as? TranslatedString.Translations
            assertNotNull("Title should be parsed as translations", title)
            assertEquals("Comprehensive Function", title!!.values["en"])
            assertEquals("Umfassende Funktion", title.values["de"])
            assertEquals("Función Integral", title.values["es"])
            assertEquals("Fonction Complète", title.values["fr"])

            // Test description with inline translations (not looked up)
            val description = metadata.description as? TranslatedString.Translations
            assertNotNull("Description should be parsed as translations", description)
            assertEquals("A function that demonstrates all metadata fields", description!!.values["en"])
            assertEquals("Eine Funktion, die alle Metadatenfelder demonstriert", description.values["de"])
            assertEquals("Una función que demuestra todos los campos de metadatos", description.values["es"])
            assertEquals("Une fonction qui démontre tous les champs de métadonnées", description.values["fr"])

            // Test config is parsed
            assertEquals(2, metadata.config.size)

            // First config: text with translation lookup
            val textConfig = metadata.config[0]
            assertEquals("sampleConfig", textConfig.id)
            assertTrue("Text config should be Text type", textConfig is LuaFunctionConfigSpec.Text)
            val configName = textConfig.name as? TranslatedString.Translations
            assertNotNull("Config name should be parsed as translations", configName)
            assertEquals("Sample Configuration", configName!!.values["en"])
            assertEquals("Beispielkonfiguration", configName.values["de"])
            assertEquals("Configuration d'Exemple", configName.values["fr"])

            // Second config: enum with translation lookups for options
            val enumConfig = metadata.config[1]
            assertEquals("enumConfig", enumConfig.id)
            assertTrue("Enum config should be Enum type", enumConfig is LuaFunctionConfigSpec.Enum)
            val enumSpec = enumConfig as LuaFunctionConfigSpec.Enum
            assertEquals("Period", (enumSpec.name as TranslatedString.Simple).value)
            assertEquals("_hours", enumSpec.defaultValue)
            assertEquals(2, enumSpec.options.size)
            assertEquals("_hours", enumSpec.options[0].id)
            assertEquals("Hours", (enumSpec.options[0].displayName as TranslatedString.Translations).values["en"])
            assertEquals("_days", enumSpec.options[1].id)
            assertEquals("Days", (enumSpec.options[1].displayName as TranslatedString.Translations).values["en"])

            // Test categories were parsed and hydrated
            assertEquals("Should have 1 category", 1, metadata.categories.size)
            assertTrue("Should contain _test_category", metadata.categories.containsKey("_test_category"))
            val categoryTranslation = metadata.categories["_test_category"] as? TranslatedString.Translations
            assertNotNull("Category should be translations", categoryTranslation)
            assertEquals("Test Category", categoryTranslation!!.values["en"])

            // Test usedTranslations contains only what was looked up (title, category, config name, 2 enum options)
            assertNotNull("usedTranslations should not be null", metadata.usedTranslations)
            assertEquals("Should have tracked 5 translation lookups", 5, metadata.usedTranslations!!.size)
            assertTrue("Should contain _comprehensive_title", metadata.usedTranslations.containsKey("_comprehensive_title"))
            assertTrue("Should contain _test_category", metadata.usedTranslations.containsKey("_test_category"))
            assertTrue("Should contain _config_name", metadata.usedTranslations.containsKey("_config_name"))
            assertTrue("Should contain _hours", metadata.usedTranslations.containsKey("_hours"))
            assertTrue("Should contain _days", metadata.usedTranslations.containsKey("_days"))
            assertTrue("Should NOT contain _unused_key", !metadata.usedTranslations.containsKey("_unused_key"))
        }
    }
}
