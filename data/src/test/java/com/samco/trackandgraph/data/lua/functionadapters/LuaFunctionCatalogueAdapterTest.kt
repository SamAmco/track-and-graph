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
package com.samco.trackandgraph.data.lua.functionadapters

import com.samco.trackandgraph.data.lua.ApiLevelCalculator
import com.samco.trackandgraph.data.lua.LuaEngineImplTest
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class LuaFunctionCatalogueAdapterTest : LuaEngineImplTest() {

    // This test needs to actually call getMaxApiLevel, so stub it to return 1
    override val apiLevelCalculator: ApiLevelCalculator = mock<ApiLevelCalculator>()

    private fun readCommunityFunctionsLua(): String {
        return javaClass.getResourceAsStream("/community-functions.lua")
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Could not read community-functions.lua from test resources")
    }

    private fun assertTranslatedStringEquals(
        expected: String,
        actual: TranslatedString?,
        fieldName: String
    ) {
        assertNotNull("$fieldName should not be null", actual)
        when (actual) {
            is TranslatedString.Translations -> {
                assertEquals(
                    "$fieldName should have correct English text",
                    expected,
                    actual.values["en"]
                )
            }

            is TranslatedString.Simple -> {
                assertEquals("$fieldName should have correct text", expected, actual.value)
            }

            null -> throw AssertionError("$fieldName should not be null")
        }
    }

    private fun assertTranslatedStringNotEmpty(actual: TranslatedString?, fieldName: String) {
        assertNotNull("$fieldName should not be null", actual)
        when (actual) {
            is TranslatedString.Translations -> {
                assertTrue(
                    "$fieldName should have non-empty English text",
                    actual.values["en"]?.isNotEmpty() == true
                )
            }

            is TranslatedString.Simple -> {
                assertTrue("$fieldName should have non-empty text", actual.value.isNotEmpty())
            }

            null -> throw AssertionError("$fieldName should not be null")
        }
    }

    private fun assertFunctionDetails(
        function: LuaFunctionMetadata?,
        id: String,
        expectedTitle: String,
        expectedConfigs: Set<String>
    ) {
        assertNotNull("$id function should exist", function)
        function?.let { func ->
            assertTranslatedStringEquals(expectedTitle, func.title, "$id title")
            assertTranslatedStringNotEmpty(func.description, "$id description")
            assertTrue("$id function should have configuration", func.config.isNotEmpty())

            val configIds = func.config.map { it.id }.toSet()
            expectedConfigs.forEach { configId ->
                assertTrue("$id should have $configId config", configIds.contains(configId))
            }
        }
    }

    @Test
    fun `parses community functions catalogue correctly`() = runTest {
        whenever(apiLevelCalculator.getMaxApiLevel(any())).thenReturn(1)
        val catalogueScript = readCommunityFunctionsLua()

        val uut = uut()
        val vmLock = uut.acquireVM()

        val catalogue = try {
            uut.runLuaCatalogue(vmLock, catalogueScript)
        } finally {
            uut.releaseVM(vmLock)
        }

        // Verify we parsed the expected number of functions
        assertEquals("Should parse 3 functions from community-functions.lua", 3, catalogue.functions.size)

        // Verify function IDs are correct
        val functionIds = catalogue.functions.map { it.id }.toSet()
        assertTrue(
            "Should contain filter-by-label function",
            functionIds.contains("filter-by-label")
        )
        assertTrue("Should contain multiply function", functionIds.contains("multiply"))
        assertTrue("Should contain override-label function", functionIds.contains("override-label"))

        catalogue.functions.forEach { function ->
            assertEquals(
                "All functions should have version 1.0.0",
                "1.0.0".toVersion(),
                function.version
            )
        }

        // Verify all functions have inputCount = 1
        catalogue.functions.forEach { function ->
            assertEquals("All functions should have inputCount = 1", 1, function.inputCount)
        }

        // Verify specific function details using helper functions
        assertFunctionDetails(
            catalogue.functions.find { it.id == "filter-by-label" },
            "filter-by-label",
            "Filter by Label",
            setOf("filter_label", "case_sensitive", "match_exactly")
        )

        assertFunctionDetails(
            catalogue.functions.find { it.id == "multiply" },
            "multiply",
            "Multiply Values",
            setOf("multiplier")
        )

        assertFunctionDetails(
            catalogue.functions.find { it.id == "override-label" },
            "override-label",
            "Override Label",
            setOf("new_label")
        )

        // Verify categories are present
        assertTrue("Catalogue should have categories", catalogue.categories.isNotEmpty())
    }

    @Test
    fun `filters deprecated and version-incompatible functions correctly`() = runTest {
        // Test with API level 2 - should filter functions based on version and deprecation
        whenever(apiLevelCalculator.getMaxApiLevel(any())).thenReturn(2)

        // Inline catalog with 4 functions testing all filtering scenarios
        val catalogueScript = """
            return {
                categories = {
                    test = {
                        en = "Test Category"
                    }
                },
                functions = {
                    ["func-deprecated-at-2"] = {
                        version = "1.0.0",
                        deprecated = 2,
                        script = "INVALID SYNTAX - should never be parsed (deprecated at API level 2)"
                    },
                    ["func-deprecated-at-1"] = {
                        version = "1.0.0",
                        deprecated = 1,
                        script = "INVALID SYNTAX - should never be parsed (deprecated at API level 1)"
                    },
                    ["func-version-2"] = {
                        version = "2.0.0",
                        script = "return { id='func-version-2', version='2.0.0', inputCount=1, categories={'test'}, title={en='Test'}, description={en='Test'}, config={}, generator=function(s)return function()return nil end end }"
                    },
                    ["func-version-3"] = {
                        version = "3.0.0",
                        script = "INVALID SYNTAX - should never be parsed (requires API level 3)"
                    }
                },
                published_at = "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        val uut = uut()
        val vmLock = uut.acquireVM()

        val catalogue = try {
            uut.runLuaCatalogue(vmLock, catalogueScript)
        } finally {
            uut.releaseVM(vmLock)
        }

        // At API level 2:
        // - func-deprecated-at-2: version 1 (ok) but deprecated=2 (EXCLUDED: deprecated <= maxApiLevel)
        // - func-deprecated-at-1: version 1 (ok) but deprecated=1 (EXCLUDED: deprecated <= maxApiLevel)
        // - func-version-2: version 2 (ok) and no deprecated (INCLUDED)
        // - func-version-3: version 3 (EXCLUDED: major > maxApiLevel)
        assertEquals("Should only return func-version-2", 1, catalogue.functions.size)
        assertEquals("Should be func-version-2", "func-version-2", catalogue.functions[0].id)
    }

    @Test
    fun `hydrates enum options with translations from catalog`() = runTest {
        whenever(apiLevelCalculator.getMaxApiLevel(any())).thenReturn(1)

        // Inline catalog with translations and a function that uses them
        val catalogueScript = """
            return {
                translations = {
                    ["_days"] = {
                        en = "Days",
                        de = "Tage",
                        es = "Días",
                        fr = "Jours"
                    },
                    ["_weeks"] = {
                        en = "Weeks",
                        de = "Wochen",
                        es = "Semanas",
                        fr = "Semaines"
                    }
                },
                categories = {
                    test = {
                        en = "Test Category"
                    }
                },
                functions = {
                    ["test-enum-function"] = {
                        version = "1.0.0",
                        script = [[
return {
    id = "test-enum-function",
    version = "1.0.0",
    inputCount = 1,
    categories = {"test"},
    title = {
        en = "Test Enum Function"
    },
    description = {
        en = "Test function with enum config"
    },
    config = {
        {
            id = "period",
            type = "enum",
            options = {"_days", "_weeks"},
            name = {
                en = "Period"
            }
        }
    },
    generator = function(s) return function() return nil end end
}
]]
                    }
                },
                published_at = "2025-01-01T00:00:00Z"
            }
        """.trimIndent()

        val uut = uut()
        val vmLock = uut.acquireVM()

        val catalogue = try {
            uut.runLuaCatalogue(vmLock, catalogueScript)
        } finally {
            uut.releaseVM(vmLock)
        }

        // Verify function was parsed
        assertEquals("Should return one function", 1, catalogue.functions.size)
        val func = catalogue.functions[0]
        assertEquals("Should be test-enum-function", "test-enum-function", func.id)

        // Verify enum config was parsed
        assertEquals("Should have one config", 1, func.config.size)
        val enumConfig = func.config[0]
        assertEquals("Config should be enum type", "period", enumConfig.id)

        // Verify enum config is an Enum type with hydrated options
        assertTrue("Config should be Enum type", enumConfig is LuaFunctionConfigSpec.Enum)
        val enumConfigSpec = enumConfig as LuaFunctionConfigSpec.Enum

        // Verify options were hydrated with EnumOption objects
        assertEquals("Should have 2 options", 2, enumConfigSpec.options.size)

        val daysOption = enumConfigSpec.options[0]
        assertEquals("First option ID should be days", "_days", daysOption.id)
        assertTrue("Days option display name should be Translations", daysOption.displayName is TranslatedString.Translations)
        assertEquals("Days option should have English translation", "Days",
            (daysOption.displayName as TranslatedString.Translations).values["en"])
        assertEquals("Days option should have German translation", "Tage",
            daysOption.displayName.values["de"])
        assertEquals("Days option should have Spanish translation", "Días",
            daysOption.displayName.values["es"])
        assertEquals("Days option should have French translation", "Jours",
            daysOption.displayName.values["fr"])

        val weeksOption = enumConfigSpec.options[1]
        assertEquals("Second option ID should be weeks", "_weeks", weeksOption.id)
        assertTrue("Weeks option display name should be Translations", weeksOption.displayName is TranslatedString.Translations)
        assertEquals("Weeks option should have English translation", "Weeks",
            (weeksOption.displayName as TranslatedString.Translations).values["en"])
        assertEquals("Weeks option should have German translation", "Wochen",
            weeksOption.displayName.values["de"])
        assertEquals("Weeks option should have Spanish translation", "Semanas",
            weeksOption.displayName.values["es"])
        assertEquals("Weeks option should have French translation", "Semaines",
            weeksOption.displayName.values["fr"])
    }
}
