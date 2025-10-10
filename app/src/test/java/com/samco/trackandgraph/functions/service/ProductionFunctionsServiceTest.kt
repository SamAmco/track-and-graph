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
package com.samco.trackandgraph.functions.service

import com.samco.trackandgraph.downloader.DownloadResult
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.fakes.FakeRemoteConfiguration
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.storage.FileCache
import com.samco.trackandgraph.storage.CachedFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import java.net.URI

class ProductionFunctionsServiceTest {

    private val remoteConfigProvider: RemoteConfigProvider = mock()
    private val fileDownloader: FileDownloader = mock()
    private val fileCache: FileCache = mock()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var service: ProductionFunctionsService

    private val testCatalogueUrl = "https://example.com/catalogue.lua"
    private val testSignatureUrl = "https://example.com/signature.json"
    private val testLuaScript = "-- test lua script".toByteArray()
    private val testSignature = "dGVzdC1zaWduYXR1cmU="
    private val testKeyId = "test-key"
    private val testSignatureJson =
        """{"keyId":"$testKeyId","algorithm":"ECDSA-P256-SHA256","signature":"$testSignature"}"""
    private val testSignatureBytes = testSignatureJson.toByteArray()

    private val testRemoteConfig = FakeRemoteConfiguration.copy(
        endpoints = FakeRemoteConfiguration.endpoints.copy(
            functionCatalogueLocation = testCatalogueUrl,
            functionCatalogueSignature = testSignatureUrl
        )
    )

    @Before
    fun setup() {
        service = ProductionFunctionsService(
            remoteConfigProvider = remoteConfigProvider,
            fileDownloader = fileDownloader,
            fileCache = fileCache,
            json = json
        )
    }

    @Test
    fun `fetchFunctionsCatalog - online success downloads new files`() = runTest {
        // Given
        whenever(remoteConfigProvider.getRemoteConfiguration()).thenReturn(testRemoteConfig)
        whenever(fileCache.getETag("function_catalogue")).thenReturn(null)
        whenever(fileDownloader.downloadFileWithETag(URI(testCatalogueUrl), null))
            .thenReturn(DownloadResult.Downloaded(testLuaScript, "new-etag"))
        whenever(fileDownloader.downloadFileToBytes(URI(testSignatureUrl)))
            .thenReturn(testSignatureBytes)

        // When
        val result = service.fetchFunctionsCatalog()

        // Then
        assertEquals(testLuaScript, result.luaScriptBytes)
        assertEquals(testKeyId, result.signatureData.keyId)
        assertEquals("ECDSA-P256-SHA256", result.signatureData.algorithm)
        assertEquals(testSignature, result.signatureData.signature)

        // Verify all interactions in correct order
        verify(remoteConfigProvider).getRemoteConfiguration()
        verify(fileCache).getETag("function_catalogue")
        verify(fileDownloader).downloadFileWithETag(URI(testCatalogueUrl), null)
        verify(fileDownloader).downloadFileToBytes(URI(testSignatureUrl))
        verify(fileCache).storeFile("function_catalogue", testLuaScript, "new-etag")
        verify(fileCache).storeFile("function_signature", testSignatureBytes, null)
        
        // Verify no additional interactions
        verifyNoMoreInteractions(remoteConfigProvider, fileDownloader, fileCache)
    }

    @Test
    fun `fetchFunctionsCatalog - online fails uses cached files`() = runTest {
        // Given
        whenever(remoteConfigProvider.getRemoteConfiguration()).thenReturn(testRemoteConfig)
        whenever(fileCache.getETag("function_catalogue")).thenReturn("cached-etag")
        whenever(fileDownloader.downloadFileWithETag(URI(testCatalogueUrl), "cached-etag"))
            .thenReturn(null) // Network failure
        whenever(fileCache.getFile("function_catalogue"))
            .thenReturn(CachedFile(testLuaScript, "cached-etag"))
        whenever(fileCache.getFile("function_signature"))
            .thenReturn(CachedFile(testSignatureBytes, null))

        // When
        val result = service.fetchFunctionsCatalog()

        // Then
        assertEquals(testLuaScript, result.luaScriptBytes)
        assertEquals(testKeyId, result.signatureData.keyId)
        assertEquals("ECDSA-P256-SHA256", result.signatureData.algorithm)
        assertEquals(testSignature, result.signatureData.signature)
        
        // Verify all interactions in correct order
        verify(remoteConfigProvider).getRemoteConfiguration()
        verify(fileCache).getETag("function_catalogue")
        verify(fileDownloader).downloadFileWithETag(URI(testCatalogueUrl), "cached-etag")
        verify(fileCache).getFile("function_catalogue")
        verify(fileCache).getFile("function_signature")

        verifyNoMoreInteractions(remoteConfigProvider, fileDownloader, fileCache)
    }

    @Test
    fun `fetchFunctionsCatalog - online uses cache when not modified`() = runTest {
        // Given
        whenever(remoteConfigProvider.getRemoteConfiguration()).thenReturn(testRemoteConfig)
        whenever(fileCache.getETag("function_catalogue")).thenReturn("existing-etag")
        whenever(fileDownloader.downloadFileWithETag(URI(testCatalogueUrl), "existing-etag"))
            .thenReturn(DownloadResult.UseCache) // 304 Not Modified
        whenever(fileCache.getFile("function_catalogue"))
            .thenReturn(CachedFile(testLuaScript, "existing-etag"))
        whenever(fileCache.getFile("function_signature"))
            .thenReturn(CachedFile(testSignatureBytes, null))

        // When
        val result = service.fetchFunctionsCatalog()

        // Then
        assertEquals(testLuaScript, result.luaScriptBytes)
        assertEquals(testKeyId, result.signatureData.keyId)
        assertEquals("ECDSA-P256-SHA256", result.signatureData.algorithm)
        assertEquals(testSignature, result.signatureData.signature)

        // Verify all interactions in correct order
        verify(remoteConfigProvider).getRemoteConfiguration()
        verify(fileCache).getETag("function_catalogue")
        verify(fileDownloader).downloadFileWithETag(URI(testCatalogueUrl), "existing-etag")
        verify(fileCache).getFile("function_catalogue")
        verify(fileCache).getFile("function_signature")
        
        verifyNoMoreInteractions(remoteConfigProvider, fileDownloader, fileCache)
    }

    @Test
    fun `fetchFunctionsCatalog - online fails and cache fails throws error`() = runTest {
        // Given
        whenever(remoteConfigProvider.getRemoteConfiguration()).thenReturn(testRemoteConfig)
        whenever(fileCache.getETag("function_catalogue")).thenReturn(null)
        whenever(fileDownloader.downloadFileWithETag(URI(testCatalogueUrl), null))
            .thenReturn(null) // Network failure
        whenever(fileCache.getFile("function_catalogue")).thenReturn(null) // No cache
        whenever(fileCache.getFile("function_signature")).thenReturn(null) // No cache

        // When & Then
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking { service.fetchFunctionsCatalog() }
        }
        assertEquals(
            "No cached function catalogue available and unable to connect to server",
            exception.message
        )
        
        // Verify all interactions occurred in correct order
        verify(remoteConfigProvider).getRemoteConfiguration()
        verify(fileCache).getETag("function_catalogue")
        verify(fileDownloader).downloadFileWithETag(URI(testCatalogueUrl), null)
        verify(fileCache).getFile("function_catalogue")

        verifyNoMoreInteractions(remoteConfigProvider, fileDownloader, fileCache)
    }

    @Test
    fun `fetchFunctionsCatalog - remote config provider throws exception falls back to cache`() = runTest {
        // Given
        whenever(remoteConfigProvider.getRemoteConfiguration()).thenThrow(RuntimeException("Network error"))
        whenever(fileCache.getFile("function_catalogue"))
            .thenReturn(CachedFile(testLuaScript, "cached-etag"))
        whenever(fileCache.getFile("function_signature"))
            .thenReturn(CachedFile(testSignatureBytes, null))

        // When
        val result = service.fetchFunctionsCatalog()

        // Then
        assertEquals(testLuaScript, result.luaScriptBytes)
        assertEquals(testKeyId, result.signatureData.keyId)
        assertEquals("ECDSA-P256-SHA256", result.signatureData.algorithm)
        assertEquals(testSignature, result.signatureData.signature)
        
        // Verify interactions
        verify(remoteConfigProvider).getRemoteConfiguration()
        verify(fileCache).getFile("function_catalogue")
        verify(fileCache).getFile("function_signature")
        
        // Verify no download attempts
        verifyNoInteractions(fileDownloader)

        verifyNoMoreInteractions(remoteConfigProvider, fileCache)
    }
}
