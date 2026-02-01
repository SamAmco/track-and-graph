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

        val pathSegments = featureGroups.map { group ->
            getPathSegmentsForGroup(group.id) + feature.name
        }

        if (pathSegments.size == 1) {
            return pathSegments.first().joinToString(separator, prefix = separator)
        }

        return computeCollapsedPath(pathSegments)
    }

    private fun computeCollapsedPath(paths: List<List<String>>): String {
        if (paths.isEmpty()) return ""
        if (paths.size == 1) return paths.first().joinToString(separator, prefix = separator)

        val prefix = findLongestCommonPrefix(paths)
        val suffix = findLongestCommonSuffix(paths)

        val minPathLength = paths.minOf { it.size }
        if (prefix.size + suffix.size >= minPathLength) {
            return paths.first().joinToString(separator, prefix = separator)
        }

        val prefixStr = if (prefix.isEmpty()) "" else prefix.joinToString(separator, prefix = separator)
        val suffixStr = suffix.joinToString(separator, prefix = separator)

        return "$prefixStr$separator...$suffixStr"
    }

    private fun findLongestCommonPrefix(paths: List<List<String>>): List<String> {
        if (paths.isEmpty()) return emptyList()

        val firstPath = paths.first()
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()

        for (i in 0 until minLength) {
            val segment = firstPath[i]
            if (paths.all { it[i] == segment }) {
                result.add(segment)
            } else {
                break
            }
        }

        return result
    }

    private fun findLongestCommonSuffix(paths: List<List<String>>): List<String> {
        if (paths.isEmpty()) return emptyList()

        val firstPath = paths.first()
        val firstPathSize = firstPath.size
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()

        for (i in 1..minLength) {
            val segment = firstPath[firstPathSize - i]
            if (paths.all { it[it.size - i] == segment }) {
                result.add(0, segment)
            } else {
                break
            }
        }

        return result
    }

    private fun sortedPaths(): List<Pair<Feature, String>> = featureGroupMap.keys
        .map { it to getPathForFeature(it.featureId) }
        .sortedBy { it.second }

    fun sortedFeatureMap(): Map<Long, String> = sortedPaths()
        .associate { it.first.featureId to it.second }
}