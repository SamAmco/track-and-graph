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

package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.FunctionEntity

//This is the only dto with the explicit name Dto because Function is too common of a name
// and causes naming conflicts with basic types
data class FunctionDto(
    override val id: Long,
    override val name: String,
    val featureId: Long,
    val dataSources: List<Feature>,
    val script: String,
    override val groupId: Long,
    override val displayIndex: Int,
    override val description: String,
) : Feature {

    companion object {
        internal fun fromEntities(
            functionEntity: FunctionEntity,
            feature: com.samco.trackandgraph.base.database.entity.Feature
        ) = FunctionDto(
            id = functionEntity.id,
            name = feature.name,
            featureId = feature.id,
            dataSources = functionEntity.dataSources.map { it.toDto() },
            script = functionEntity.script,
            groupId = feature.groupId,
            displayIndex = feature.displayIndex,
            description = feature.description
        )
    }

    internal fun toEntity() = com.samco.trackandgraph.base.database.entity.FunctionEntity(
        id = id,
        featureId = featureId,
        dataSources = dataSources.map { it.toEntity() },
        script = script
    )
}