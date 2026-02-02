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

    private val groupsById: Map<Long, Group> = groups.associateBy { it.id }

    private val featureGroupMap: Map<Feature, List<Group>> = features.mapNotNull { feature ->
        val featureGroups = feature.groupIds.mapNotNull { groupId ->
            groupsById[groupId]
        }
        if (featureGroups.isEmpty()) return@mapNotNull null
        feature to featureGroups
    }.toMap()

    val features get() = featureGroupMap.keys

    fun featureName(featureId: Long) = featureGroupMap.keys
        .firstOrNull { it.featureId == featureId }?.name

    fun getPathForFeature(featureId: Long): String {
        val feature = featureGroupMap.keys.firstOrNull { it.featureId == featureId } ?: return ""
        val featureGroups = featureGroupMap[feature] ?: return ""

        val allPaths = featureGroups.flatMap { group ->
            getAllPathSegmentsForGroup(group.id).map { pathSegments ->
                pathSegments + feature.name
            }
        }

        if (allPaths.size == 1) {
            return allPaths.first().joinToString(separator, prefix = separator)
        }

        return computeCollapsedPath(allPaths)
    }

    private fun sortedPaths(): List<Pair<Feature, String>> = featureGroupMap.keys
        .map { it to getPathForFeature(it.featureId) }
        .sortedBy { it.second }

    fun sortedFeatureMap(): Map<Long, String> = sortedPaths()
        .associate { it.first.featureId to it.second }
}