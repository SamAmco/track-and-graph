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

//This is the only dto with the explicit name Dto because Function is too common of a name
// and causes naming conflicts with basic types
data class FunctionDto(
    val id: Long,
    val name: String,
    val groupId: Long,
    val description: String,
    val dataSources: List<DataSourceDescriptor>,
    val script: String
) {
    internal fun toEntity() = com.samco.trackandgraph.base.database.entity.FunctionEntity(
        id,
        name,
        groupId,
        description,
        dataSources.map { it.toEntity() },
        script
    )
}