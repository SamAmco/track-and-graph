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

import com.samco.trackandgraph.data.lua.LuaEngineImplTest
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LuaFunctionCatalogueAdapterTest : LuaEngineImplTest() {

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
    fun `parses community functions catalogue correctly`() = runBlocking {
        val catalogueScript = readCommunityFunctionsLua()

        val uut = uut()
        val vmLock = uut.acquireVM()

        val functions = try {
            uut.runLuaCatalogue(vmLock, catalogueScript)
        } finally {
            uut.releaseVM(vmLock)
        }

        // Verify we parsed the expected number of functions
        assertEquals("Should parse 3 functions from community-functions.lua", 3, functions.size)

        // Verify function IDs are correct
        val functionIds = functions.map { it.id }.toSet()
        assertTrue(
            "Should contain filter-by-label function",
            functionIds.contains("filter-by-label")
        )
        assertTrue("Should contain multiply function", functionIds.contains("multiply"))
        assertTrue("Should contain override-label function", functionIds.contains("override-label"))

        functions.forEach { function ->
            assertEquals(
                "All functions should have version 0.0.0",
                "0.0.0".toVersion(),
                function.version
            )
        }

        // Verify all functions have inputCount = 1
        functions.forEach { function ->
            assertEquals("All functions should have inputCount = 1", 1, function.inputCount)
        }

        // Verify specific function details using helper functions
        assertFunctionDetails(
            functions.find { it.id == "filter-by-label" },
            "filter-by-label",
            "Filter by Label",
            setOf("filter_label", "case_sensitive", "match_exactly")
        )

        assertFunctionDetails(
            functions.find { it.id == "multiply" },
            "multiply",
            "Multiply Values",
            setOf("multiplier")
        )

        assertFunctionDetails(
            functions.find { it.id == "override-label" },
            "override-label",
            "Override Label",
            setOf("new_label")
        )
    }
}
