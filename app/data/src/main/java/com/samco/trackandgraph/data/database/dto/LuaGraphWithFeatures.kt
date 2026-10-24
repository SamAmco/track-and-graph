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

import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot

class LuaGraphWithFeatures(
    val id: Long,
    val graphStatId: Long,
    val features: List<LuaGraphFeature>,
    val source: LuaGraphSource,
    val configuration: Lazy<LuaValueSnapshot>? = null,
) {
    fun toConfig() = LuaGraphConfig(
        features = features.map { it.toFeatureConfig() },
        source = source,
        configuration = configuration,
    )

    fun copy(
        id: Long = this.id,
        graphStatId: Long = this.graphStatId,
        features: List<LuaGraphFeature> = this.features,
        source: LuaGraphSource = this.source,
        configuration: Lazy<LuaValueSnapshot>? = this.configuration,
    ) = LuaGraphWithFeatures(id, graphStatId, features, source, configuration)
}
