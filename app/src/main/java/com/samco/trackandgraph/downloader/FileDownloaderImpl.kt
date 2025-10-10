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
package com.samco.trackandgraph.downloader

import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import javax.inject.Inject

class FileDownloaderImpl @Inject constructor(
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : FileDownloader {
    override suspend fun downloadFileToString(url: URI): String? = withContext(ioDispatcher) {
        try {
            val connection = url.toURL().openConnection()
            BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file from $url")
            null
        }
    }

    override suspend fun downloadFileToBytes(url: URI): ByteArray? = withContext(ioDispatcher) {
        try {
            val connection = url.toURL().openConnection()
            connection.getInputStream().use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to download file bytes from $url")
            null
        }
    }

    override suspend fun downloadFileWithETag(url: URI, ifNoneMatch: String?): DownloadResult? =
        withContext(ioDispatcher) {
            try {
                val connection = url.toURL().openConnection()

                // Set If-None-Match header for conditional request
                if (ifNoneMatch != null) {
                    connection.setRequestProperty("If-None-Match", ifNoneMatch)
                }

                // Check response code for 304 Not Modified
                val httpConnection = connection as? java.net.HttpURLConnection
                val responseCode = httpConnection?.responseCode ?: 200

                if (responseCode == 304) {
                    Timber.d("cache hit for url: $url")
                    // File not modified, use cached version
                    return@withContext DownloadResult.UseCache
                }

                // Get ETag from response headers
                val etag = connection.getHeaderField("ETag")

                // Download the file content
                val data = connection.getInputStream().use { inputStream ->
                    inputStream.readBytes()
                }
                Timber.d("cache miss for url: $url, new etag: $etag, data size: ${data.size}")

                DownloadResult.Downloaded(
                    data = data,
                    etag = etag
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to download file with ETag from $url")
                null
            }
        }
}