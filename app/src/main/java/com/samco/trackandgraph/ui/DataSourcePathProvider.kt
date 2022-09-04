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

package com.samco.trackandgraph.ui

import com.samco.trackandgraph.base.database.dto.DataSourceDescriptor
import com.samco.trackandgraph.base.database.dto.DataSourceType
import com.samco.trackandgraph.base.database.dto.Group

open class DataSourcePathProvider(
    private val dataSources: Map<DataSourceDescriptor, Group>,
) : GroupPathProvider(dataSources.values) {
    fun sortedAlphabetically() = dataSources.keys.sortedBy { getPathForDataSource(it.id, it.type) }

    fun getPathForDataSource(id: Long, type: DataSourceType): String {
        val dataSource = dataSources.keys.firstOrNull { it.id == id && it.type == type } ?: return ""
        val group = dataSources[dataSource] ?: return ""
        val groupPath = getPathForGroup(group.id)
        var path = groupPath
        if (groupPath.lastOrNull() != '/') path += '/'
        return path + dataSource.name
    }
}