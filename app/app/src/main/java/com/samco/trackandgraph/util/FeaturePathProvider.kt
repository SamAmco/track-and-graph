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
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem

fun GroupGraph.allFeatureIds(): List<Long> {
    val result = mutableListOf<Long>()
    fun walk(graph: GroupGraph) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> walk(child.groupGraph)
                is GroupGraphItem.TrackerNode -> result.add(child.tracker.featureId)
                is GroupGraphItem.FunctionNode -> result.add(child.function.featureId)
                is GroupGraphItem.GraphNode -> Unit
            }
        }
    }
    walk(this)
    return result
}

open class FeaturePathProvider(groupGraph: GroupGraph) {

    private val separator = "/"

    /**
     * For each group ID, all paths from root to that group.
     * A group appearing under multiple parents in the tree will have multiple paths.
     */
    private val groupPathsById: Map<Long, List<List<String>>>

    /**
     * For each feature ID, all paths from root to that feature (including the feature name).
     */
    private val featurePathsById: Map<Long, List<List<String>>>
    private val featureNames: Map<Long, String>

    val features: Set<Feature>

    init {
        val groupPaths = mutableMapOf<Long, MutableList<List<String>>>()
        val featurePaths = mutableMapOf<Long, MutableList<List<String>>>()
        val namesById = mutableMapOf<Long, String>()
        val featuresSet = mutableSetOf<Feature>()

        fun walk(graph: GroupGraph, currentPath: List<String>) {
            for (child in graph.children) {
                when (child) {
                    is GroupGraphItem.GroupNode -> {
                        val childGroup = child.groupGraph.group
                        val childPath = currentPath + childGroup.name
                        groupPaths.getOrPut(childGroup.id) { mutableListOf() }.add(childPath)
                        walk(child.groupGraph, childPath)
                    }
                    is GroupGraphItem.TrackerNode -> {
                        val f = child.tracker
                        featuresSet.add(f)
                        namesById[f.featureId] = f.name
                        featurePaths.getOrPut(f.featureId) { mutableListOf() }
                            .add(currentPath + f.name)
                    }
                    is GroupGraphItem.FunctionNode -> {
                        val f = child.function
                        featuresSet.add(f)
                        namesById[f.featureId] = f.name
                        featurePaths.getOrPut(f.featureId) { mutableListOf() }
                            .add(currentPath + f.name)
                    }
                    is GroupGraphItem.GraphNode -> Unit
                }
            }
        }

        // Root group has empty path
        groupPaths[groupGraph.group.id] = mutableListOf(emptyList())
        walk(groupGraph, emptyList())

        groupPathsById = groupPaths.mapValues { (_, paths) -> paths.distinct() }
        featurePathsById = featurePaths.mapValues { (_, paths) -> paths.distinct() }
        featureNames = namesById
        features = featuresSet
    }

    private val groupPathCache = mutableMapOf<Long, String>()
    private val featurePathCache = mutableMapOf<Long, String>()

    fun featureName(featureId: Long) = featureNames[featureId]

    fun getPathForGroup(id: Long): String = groupPathCache.getOrPut(id) {
        val allPaths = groupPathsById[id] ?: return@getOrPut separator
        val nonEmpty = allPaths.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return@getOrPut separator
        if (nonEmpty.size == 1) return@getOrPut separator + nonEmpty.first().joinToString(separator)
        collapsePaths(nonEmpty)
    }

    fun getPathForFeature(featureId: Long): String = featurePathCache.getOrPut(featureId) {
        val allPaths = featurePathsById[featureId] ?: return@getOrPut ""
        if (allPaths.size == 1) return@getOrPut separator + allPaths.first().joinToString(separator)
        collapsePaths(allPaths)
    }

    fun sortedFeatureMap(): Map<Long, String> {
        return featurePathsById.keys
            .associateWith { getPathForFeature(it) }
            .entries
            .sortedBy { it.value }
            .associate { it.key to it.value }
    }

    private fun collapsePaths(paths: List<List<String>>): String {
        if (paths.isEmpty()) return ""
        if (paths.size == 1) return separator + paths.first().joinToString(separator)
        if (paths.all { it == paths.first() }) {
            return separator + paths.first().joinToString(separator)
        }

        val prefix = findLongestCommonPrefix(paths)
        val suffix = findLongestCommonSuffix(paths)
        val suffixStr = separator + suffix.joinToString(separator)

        return if (prefix.isEmpty()) {
            "...$suffixStr"
        } else {
            separator + prefix.joinToString(separator) + separator + "..." + suffixStr
        }
    }

    private fun findLongestCommonPrefix(paths: List<List<String>>): List<String> {
        if (paths.isEmpty()) return emptyList()
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()
        for (i in 0 until minLength) {
            val segment = paths.first()[i]
            if (paths.all { it[i] == segment }) result.add(segment)
            else break
        }
        return result
    }

    private fun findLongestCommonSuffix(paths: List<List<String>>): List<String> {
        if (paths.isEmpty()) return emptyList()
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()
        for (i in 1..minLength) {
            val segment = paths.first()[paths.first().size - i]
            if (paths.all { it[it.size - i] == segment }) result.add(0, segment)
            else break
        }
        return result
    }
}
