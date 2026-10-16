/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "lua_graph_packages_table",
    primaryKeys = ["package_id", "version"],
)
internal data class LuaGraphPackage(
    @ColumnInfo(name = "package_id")
    val packageId: String,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "archive", typeAffinity = ColumnInfo.BLOB)
    val archive: ByteArray,
)
