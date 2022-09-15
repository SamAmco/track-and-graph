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

import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Group

open class FeaturePathProvider(
    private val featureGroupMap: Map<Feature, Group>,
) : GroupPathProvider(featureGroupMap.values) {

    constructor(features: List<Feature>, groups: List<Group>) : this(
        features.mapNotNull { feature ->
            val group = groups.firstOrNull { it.id == feature.groupId }
                ?: return@mapNotNull null
            feature to group
        }.toMap()
    )

    fun sortedAlphabetically() = featureGroupMap.keys.sortedBy { getPathForFeature(it.id) }

    fun getPathForFeature(id: Long): String {
        val dataSource = featureGroupMap.keys.firstOrNull { it.id == id } ?: return ""
        val group = featureGroupMap[dataSource] ?: return ""
        val groupPath = getPathForGroup(group.id)
        var path = groupPath
        if (groupPath.lastOrNull() != '/') path += '/'
        return path + dataSource.name
    }
}