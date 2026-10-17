/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.database.dto

/** A versioned, ZIP-backed Lua graph package stored once and referenced by graph instances. */
data class LuaGraphPackage(
    val packageId: String,
    val version: String,
    val archive: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is LuaGraphPackage &&
            packageId == other.packageId &&
            version == other.version &&
            archive.contentEquals(other.archive)

    override fun hashCode(): Int {
        var result = packageId.hashCode()
        result = 31 * result + version.hashCode()
        return 31 * result + archive.contentHashCode()
    }
}
