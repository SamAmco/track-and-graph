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

package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.database.entity.LuaGraph
import com.samco.trackandgraph.data.lua.LuaStateJsonCodec
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot

/** A Lua graph DTO whose persisted configuration is decoded only if it is actually needed. */
class LuaGraph(
    val id: Long,
    val graphStatId: Long,
    val source: LuaGraphSource,
    /** A detached immutable state snapshot; evaluating it may decode the stored JSON. */
    val configuration: Lazy<LuaValueSnapshot>? = null,
) {
    internal fun toEntity() = source.toEntity(id, graphStatId, configuration)

    fun copy(
        id: Long = this.id,
        graphStatId: Long = this.graphStatId,
        source: LuaGraphSource = this.source,
        configuration: Lazy<LuaValueSnapshot>? = this.configuration,
    ) = LuaGraph(id, graphStatId, source, configuration)
}

internal fun LuaGraphSource.toEntity(
    id: Long,
    graphStatId: Long,
    configuration: Lazy<LuaValueSnapshot>? = null,
) = when (this) {
    is LuaGraphSource.Inline -> LuaGraph(
        id = id,
        graphStatId = graphStatId,
        script = script,
        configuration = LuaStateJsonCodec.encode(configuration),
    )
    is LuaGraphSource.Package -> LuaGraph(
        id = id,
        graphStatId = graphStatId,
        script = null,
        packageId = packageData.packageId,
        packageVersion = packageData.version,
        configuration = LuaStateJsonCodec.encode(configuration),
    )
}
