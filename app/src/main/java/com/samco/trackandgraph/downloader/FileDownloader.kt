package com.samco.trackandgraph.downloader

import java.net.URI

interface FileDownloader {
    suspend fun downloadFileToString(url: URI): String?
}