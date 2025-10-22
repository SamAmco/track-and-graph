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
package com.samco.trackandgraph.functions.repository

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.TestLuaVMFixtures
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionCatalogue
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.service.FunctionsCatalogData
import com.samco.trackandgraph.functions.service.FunctionsService
import com.samco.trackandgraph.functions.service.SignatureData
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FunctionsRepositoryImplTest {

    private val mockFunctionsService = mock<FunctionsService>()
    private val mockLuaEngine = mock<LuaEngine>()
    private val mockAssetReader = mock<AssetReader>()
    private val testVmLock = TestLuaVMFixtures.createTestLuaVMLock()

    private val repository = FunctionsRepositoryImpl(
        functionsService = mockFunctionsService,
        luaEngine = mockLuaEngine,
        assetReader = mockAssetReader
    )

    @Test
    fun `fetchFunctions happy path - successfully fetches, verifies signature, and parses functions`() =
        runTest {
            // Given - Load test data from resources
            val luaScript =
                javaClass.getResourceAsStream("/functions-catalog/community-functions.lua")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test community-functions.lua")

            val publicKey =
                javaClass.getResourceAsStream("/functions-catalog/debug-20251010T194102Z.pub")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test public key")

            val signatureData = SignatureData(
                keyId = "debug-20251010T194102Z",
                algorithm = "ECDSA-P256-SHA256",
                signature = "MEYCIQC7EtQMp/uZ6whP7RHDTununvnAZmdxSO2LFyxxwOR+qwIhAPpljd0MQvsTJo6LP2oG2Xs1mSOQtKHlAs/X6h2e7xsr"
            )

            val catalogData = FunctionsCatalogData(
                luaScriptBytes = luaScript.toByteArray(Charsets.UTF_8),
                signatureData = signatureData
            )

            // Mock expected function metadata that should be returned by LuaEngine
            val expectedFunctions = listOf(
                LuaFunctionMetadata(
                    script = "mock-script-1",
                    id = "filter-by-label",
                    version = "1.0.0".toVersion(),
                    title = TranslatedString.Simple("Filter by Label"),
                    description = TranslatedString.Simple("Filters data points by label"),
                    inputCount = 1,
                    config = emptyList()
                ),
                LuaFunctionMetadata(
                    script = "mock-script-2",
                    id = "multiply",
                    version = "1.0.0".toVersion(),
                    title = TranslatedString.Simple("Multiply Values"),
                    description = TranslatedString.Simple("Multiplies values"),
                    inputCount = 1,
                    config = emptyList()
                )
            )

            // Mock the catalogue response
            val expectedCatalogue = LuaFunctionCatalogue(expectedFunctions)

            // Setup mocks
            whenever(mockFunctionsService.fetchFunctionsCatalog()).thenReturn(catalogData)
            whenever(mockAssetReader.readAssetToString("functions-catalog/debug-20251010T194102Z.pub"))
                .thenReturn(publicKey)
            whenever(mockLuaEngine.acquireVM()).thenReturn(testVmLock)
            whenever(mockLuaEngine.runLuaCatalogue(testVmLock, luaScript)).thenReturn(
                expectedCatalogue
            )

            // When
            val result = repository.fetchFunctions()

            // Then
            assertEquals("Should return the expected functions", expectedFunctions, result)

            // Verify interactions
            verify(mockFunctionsService).fetchFunctionsCatalog()
            verify(mockAssetReader).readAssetToString("functions-catalog/debug-20251010T194102Z.pub")
            verify(mockLuaEngine).acquireVM()
            verify(mockLuaEngine).runLuaCatalogue(eq(testVmLock), eq(luaScript))
            verify(mockLuaEngine).releaseVM(testVmLock)
        }

    @Test
    fun `fetchFunctions signature verification fails - throws SignatureVerificationException`() =
        runTest {
            // Given - Load test data but with invalid signature
            val luaScript =
                javaClass.getResourceAsStream("/functions-catalog/community-functions.lua")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test community-functions.lua")

            val publicKey =
                javaClass.getResourceAsStream("/functions-catalog/debug-20251010T194102Z.pub")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test public key")

            // Invalid signature that won't match the script
            val invalidSignatureData = SignatureData(
                keyId = "debug-20251010T194102Z",
                algorithm = "ECDSA-P256-SHA256",
                signature = "INVALID_SIGNATURE_THAT_WONT_VERIFY"
            )

            val catalogData = FunctionsCatalogData(
                luaScriptBytes = luaScript.toByteArray(Charsets.UTF_8),
                signatureData = invalidSignatureData
            )

            // Setup mocks
            whenever(mockFunctionsService.fetchFunctionsCatalog()).thenReturn(catalogData)
            whenever(mockAssetReader.readAssetToString("functions-catalog/debug-20251010T194102Z.pub"))
                .thenReturn(publicKey)

            // When & Then
            assertThrows(SignatureVerificationException::class.java) {
                runBlocking { repository.fetchFunctions() }
            }

            // Verify interactions - should not reach Lua engine
            verify(mockFunctionsService).fetchFunctionsCatalog()
            verify(mockAssetReader).readAssetToString("functions-catalog/debug-20251010T194102Z.pub")
            verify(mockLuaEngine, never()).acquireVM()
            verify(mockLuaEngine, never()).releaseVM(any())
        }

    @Test
    fun `fetchFunctions service throws exception - rethrows and releases VM`() = runTest {
        // Given
        whenever(mockFunctionsService.fetchFunctionsCatalog()).thenThrow(RuntimeException("Service unavailable"))

        // When
        assertThrows(RuntimeException::class.java) {
            runBlocking { repository.fetchFunctions() }
        }

        // Verify interactions - should not reach asset reader or Lua engine
        verify(mockFunctionsService).fetchFunctionsCatalog()
        verify(mockAssetReader, never()).readAssetToString(any())
        verify(mockLuaEngine, never()).acquireVM()
        verify(mockLuaEngine, never()).releaseVM(any())
    }

    @Test
    fun `fetchFunctions lua engine throws exception - rethrows and releases VM`() =
        runTest {
            // Given - Valid signature but Lua parsing fails
            val luaScript =
                javaClass.getResourceAsStream("/functions-catalog/community-functions.lua")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test community-functions.lua")

            val publicKey =
                javaClass.getResourceAsStream("/functions-catalog/debug-20251010T194102Z.pub")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not read test public key")

            val signatureData = SignatureData(
                keyId = "debug-20251010T194102Z",
                algorithm = "ECDSA-P256-SHA256",
                signature = "MEYCIQC7EtQMp/uZ6whP7RHDTununvnAZmdxSO2LFyxxwOR+qwIhAPpljd0MQvsTJo6LP2oG2Xs1mSOQtKHlAs/X6h2e7xsr"
            )

            val catalogData = FunctionsCatalogData(
                luaScriptBytes = luaScript.toByteArray(Charsets.UTF_8),
                signatureData = signatureData
            )

            // Setup mocks - Lua engine throws exception
            whenever(mockFunctionsService.fetchFunctionsCatalog()).thenReturn(catalogData)
            whenever(mockAssetReader.readAssetToString("functions-catalog/debug-20251010T194102Z.pub"))
                .thenReturn(publicKey)
            whenever(mockLuaEngine.acquireVM()).thenReturn(testVmLock)
            whenever(mockLuaEngine.runLuaCatalogue(testVmLock, luaScript))
                .thenThrow(RuntimeException("Lua parsing failed"))

            // When
            assertThrows(RuntimeException::class.java) {
                runBlocking { repository.fetchFunctions() }
            }

            // Verify interactions - VM should be acquired and released even on exception
            verify(mockFunctionsService).fetchFunctionsCatalog()
            verify(mockAssetReader).readAssetToString("functions-catalog/debug-20251010T194102Z.pub")
            verify(mockLuaEngine).acquireVM()
            verify(mockLuaEngine).runLuaCatalogue(eq(testVmLock), eq(luaScript))
            verify(mockLuaEngine).releaseVM(testVmLock) // Critical: VM must be released even on exception
        }
}
