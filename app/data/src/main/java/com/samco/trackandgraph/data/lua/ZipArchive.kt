/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

/** Returns whether these bytes start with the local-file signature of a non-empty ZIP archive. */
fun ByteArray.isZipArchive(): Boolean =
    size >= ZIP_LOCAL_FILE_HEADER.size &&
        ZIP_LOCAL_FILE_HEADER.indices.all { index -> this[index] == ZIP_LOCAL_FILE_HEADER[index] }

private val ZIP_LOCAL_FILE_HEADER = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)
