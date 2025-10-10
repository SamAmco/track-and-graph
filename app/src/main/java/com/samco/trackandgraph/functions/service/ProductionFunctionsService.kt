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
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.storage.FileCache
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

/**
 * Production implementation of FunctionsService that fetches files from online sources.
 */
class ProductionFunctionsService @Inject constructor(
    private val remoteConfigProvider: RemoteConfigProvider,
    private val fileDownloader: FileDownloader,
    private val fileCache: FileCache,
    private val json: Json
) : FunctionsService {

    override suspend fun fetchFunctionsCatalog(): FunctionsCatalogData {
        return tryFetchOnline() ?: tryFetchFromCache()
    }

    private suspend fun tryFetchOnline(): FunctionsCatalogData? {
        return try {
            val config = remoteConfigProvider.getRemoteConfiguration() ?: return null
            val catalogueUrl = config.endpoints.functionCatalogueLocation
            val signatureUrl = config.endpoints.functionCatalogueSignature
            
            Timber.d("Attempting to fetch catalogue from: $catalogueUrl")
            
            fetchCatalogueWithCaching(catalogueUrl, signatureUrl)
        } catch (e: Exception) {
            Timber.w(e, "Online fetch failed")
            null
        }
    }

    private suspend fun fetchCatalogueWithCaching(catalogueUrl: String, signatureUrl: String): FunctionsCatalogData? {
        val catalogueETag = fileCache.getETag(CATALOGUE_CACHE_KEY)
        val catalogueResult = fileDownloader.downloadFileWithETag(URI(catalogueUrl), catalogueETag) ?: return null
        
        val (catalogueBytes, signatureBytes) = when (catalogueResult) {
            is DownloadResult.Downloaded -> {
                Timber.d("Catalogue updated, downloading new signature")
                val signatureBytes = fileDownloader.downloadFileToBytes(URI(signatureUrl)) ?: return null
                
                // Store both files
                fileCache.storeFile(CATALOGUE_CACHE_KEY, catalogueResult.data, catalogueResult.etag)
                fileCache.storeFile(SIGNATURE_CACHE_KEY, signatureBytes, null)
                
                catalogueResult.data to signatureBytes
            }
            is DownloadResult.UseCache -> {
                Timber.d("Catalogue not modified, using cached files")
                val cachedCatalogue = fileCache.getFile(CATALOGUE_CACHE_KEY)?.data ?: return null
                val cachedSignature = fileCache.getFile(SIGNATURE_CACHE_KEY)?.data ?: return null
                
                cachedCatalogue to cachedSignature
            }
        }
        
        return createFunctionsCatalogData(catalogueBytes, signatureBytes)
    }

    private suspend fun tryFetchFromCache(): FunctionsCatalogData {
        Timber.d("Attempting to use cached catalogue (offline mode)")
        
        val cachedCatalogue = fileCache.getFile(CATALOGUE_CACHE_KEY)?.data
        val cachedSignature = fileCache.getFile(SIGNATURE_CACHE_KEY)?.data
        
        if (cachedCatalogue == null || cachedSignature == null) {
            error("No cached function catalogue available and unable to connect to server")
        }
        
        return createFunctionsCatalogData(cachedCatalogue, cachedSignature)
    }

    private fun createFunctionsCatalogData(catalogueBytes: ByteArray, signatureBytes: ByteArray): FunctionsCatalogData {
        val signatureContent = String(signatureBytes, Charsets.UTF_8)
        val signatureData = json.decodeFromString<SignatureData>(signatureContent)
        
        return FunctionsCatalogData(
            luaScriptBytes = catalogueBytes,
            signatureData = signatureData
        )
    }

    companion object {
        private const val CATALOGUE_CACHE_KEY = "function_catalogue"
        private const val SIGNATURE_CACHE_KEY = "function_signature"
    }

}
