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

import android.content.Context
import com.samco.trackandgraph.data.di.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Implementation of FileStore using Android's cache directory.
 * Files are stored as key.data and ETags as key.etag
 */
class FileCacheImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : FileCache {

    companion object {
        private const val DATA_SUFFIX = ".data"
        private const val ETAG_SUFFIX = ".etag"
    }

    private val cacheDir: File
        get() = File(context.cacheDir, "filestore").apply { mkdirs() }

    override suspend fun storeFile(key: String, data: ByteArray, etag: String?) = withContext(ioDispatcher) {
        try {
            val dataFile = File(cacheDir, "$key$DATA_SUFFIX")
            val etagFile = File(cacheDir, "$key$ETAG_SUFFIX")

            dataFile.parentFile?.mkdirs()
            etagFile.parentFile?.mkdirs()

            // Write data file
            dataFile.writeBytes(data)

            // Write etag file if etag is provided
            if (etag != null) {
                etagFile.writeText(etag.trim(), Charsets.UTF_8)
            } else {
                // Remove etag file if no etag provided
                etagFile.delete()
            }

            Timber.d("Stored file with key: $key, size: ${data.size} bytes, etag: $etag")
        } catch (e: Exception) {
            Timber.e(e, "Failed to store file with key: $key")
            throw e
        }
    }

    override suspend fun getFile(key: String): CachedFile? = withContext(ioDispatcher) {
        try {
            val dataFile = File(cacheDir, "$key$DATA_SUFFIX")
            val etagFile = File(cacheDir, "$key$ETAG_SUFFIX")

            if (!dataFile.exists()) {
                return@withContext null
            }

            val data = dataFile.readBytes()
            val etag = if (etagFile.exists()) etagFile.readText(Charsets.UTF_8).trim() else null

            Timber.d("Retrieved file with key: $key, size: ${data.size} bytes, etag: $etag")
            CachedFile(data, etag)
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve file with key: $key")
            null
        }
    }

    override suspend fun getETag(key: String): String? = withContext(ioDispatcher) {
        try {
            val etagFile = File(cacheDir, "$key$ETAG_SUFFIX")
            if (etagFile.exists()) etagFile.readText(Charsets.UTF_8).trim() else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve ETag for key: $key")
            null
        }
    }

    override suspend fun removeFile(key: String) = withContext(ioDispatcher) {
        try {
            val dataFile = File(cacheDir, "$key$DATA_SUFFIX")
            val etagFile = File(cacheDir, "$key$ETAG_SUFFIX")

            dataFile.delete()
            etagFile.delete()

            Timber.d("Removed file with key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove file with key: $key")
        }
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            Timber.d("Cleared all cached files")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
        }
    }
}
