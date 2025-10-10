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
package com.samco.trackandgraph.storage

/**
 * Data class representing a cached file with its ETag
 */
class CachedFile(
    val data: ByteArray,
    val etag: String?
)

/**
 * Simple file store interface for caching files with ETag support.
 * Files are stored in the app's private cache directory.
 */
interface FileCache {
    /**
     * Stores a file with its ETag for caching
     * @param key Unique identifier for the file
     * @param data File content as bytes
     * @param etag ETag header value for cache validation
     */
    suspend fun storeFile(key: String, data: ByteArray, etag: String?)

    /**
     * Retrieves a cached file if it exists
     * @param key Unique identifier for the file
     * @return CachedFile containing data and ETag, or null if not found
     */
    suspend fun getFile(key: String): CachedFile?

    /**
     * Gets the ETag for a cached file without loading the full content
     * @param key Unique identifier for the file
     * @return ETag string or null if file doesn't exist or has no ETag
     */
    suspend fun getETag(key: String): String?

    /**
     * Removes a cached file
     * @param key Unique identifier for the file
     */
    suspend fun removeFile(key: String)

    /**
     * Clears all cached files
     */
    suspend fun clearAll()
}
