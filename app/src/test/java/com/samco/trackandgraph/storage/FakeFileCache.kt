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

class FakeFileCache : FileCache {
    private val files = mutableMapOf<String, CachedFile>()

    override suspend fun storeFile(key: String, data: ByteArray, etag: String?) {
        files[key] = CachedFile(data, etag)
    }

    override suspend fun getFile(key: String): CachedFile? = files[key]

    override suspend fun getETag(key: String): String? = files[key]?.etag

    override suspend fun removeFile(key: String) {
        files.remove(key)
    }

    override suspend fun clearAll() {
        files.clear()
    }

    fun preloadFile(key: String, data: ByteArray, etag: String? = null) {
        files[key] = CachedFile(data, etag)
    }
}