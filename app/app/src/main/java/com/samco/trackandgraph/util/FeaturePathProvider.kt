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

package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.Group

open class FeaturePathProvider(
    features: List<Feature>,
    groups: List<Group>,
) : GroupPathProvider(groups) {

    // TODO: When features can exist in multiple groups, this will need to handle
    // showing the feature in multiple paths
    private val featureGroupMap: Map<Feature, Group> = features.mapNotNull { feature ->
        val groupId = feature.groupIds.firstOrNull() ?: return@mapNotNull null
        val group = groups.firstOrNull { it.id == groupId }
            ?: return@mapNotNull null
        feature to group
    }.toMap()

    fun sortedFeatureMap(): Map<Long, String> = sortedPaths()
        .associate { it.first.featureId to it.second }

    val features get() = featureGroupMap.keys

    fun featureName(featureId: Long) = featureGroupMap.keys
        .firstOrNull { it.featureId == featureId }?.name

    fun sortedPaths(): List<Pair<Feature, String>> = featureGroupMap.keys
        .map { it to getPathForFeature(it.featureId) }
        .sortedBy { it.second }

    fun getPathForFeature(featureId: Long): String {
        val dataSource = featureGroupMap.keys.firstOrNull { it.featureId == featureId } ?: return ""
        val group = featureGroupMap[dataSource] ?: return ""
        val groupPath = getPathForGroup(group.id)
        var path = groupPath
        if (groupPath.lastOrNull() != '/') path += '/'
        return path + dataSource.name
    }
}