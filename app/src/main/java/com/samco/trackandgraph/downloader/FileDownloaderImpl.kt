package com.samco.trackandgraph.downloader

import com.samco.trackandgraph.base.model.di.IODispatcher
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
}