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

import java.net.URI

/**
 * Result of a download operation with ETag support
 */
sealed class DownloadResult {
    /**
     * File was downloaded (either first time or modified)
     */
    class Downloaded(
        val data: ByteArray,
        val etag: String?
    ) : DownloadResult()
    
    /**
     * File was not modified, use cached version
     */
    data object UseCache : DownloadResult()
}

interface FileDownloader {
    suspend fun downloadFileToString(url: URI): String?
    suspend fun downloadFileToBytes(url: URI): ByteArray?
    
    /**
     * Downloads a file with ETag support for conditional requests
     * @param url URL to download from
     * @param ifNoneMatch ETag value for conditional request (If-None-Match header)
     * @return DownloadResult with data, etag, and modification status, or null if request failed
     */
    suspend fun downloadFileWithETag(url: URI, ifNoneMatch: String?): DownloadResult?
}