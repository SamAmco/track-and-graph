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

import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem

class ComponentPathProvider(private val groupGraph: GroupGraph) {

    private enum class ComponentType { GROUP, TRACKER, FUNCTION, GRAPH }

    private val separator = "/"

    private val pathsByKey: Map<Pair<ComponentType, Long>, List<String>> by lazy {
        val entries = listOf(
            Triple(ComponentType.GROUP, groupGraph.group.id, emptyList<String>())
        ) + walk(groupGraph, emptyList())

        entries.groupBy(
            keySelector = { (type, id, _) -> type to id },
            valueTransform = { (_, _, segments) -> formatPath(segments) }
        ).mapValues { (_, paths) -> paths.distinct() }
    }

    fun getAllPathsForGroup(groupId: Long): List<String> =
        pathsByKey[ComponentType.GROUP to groupId] ?: emptyList()

    fun getAllPathsForTracker(trackerId: Long): List<String> =
        pathsByKey[ComponentType.TRACKER to trackerId] ?: emptyList()

    fun getAllPathsForFunction(functionId: Long): List<String> =
        pathsByKey[ComponentType.FUNCTION to functionId] ?: emptyList()

    fun getAllPathsForGraph(graphId: Long): List<String> =
        pathsByKey[ComponentType.GRAPH to graphId] ?: emptyList()

    private fun walk(
        graph: GroupGraph,
        currentPath: List<String>
    ): List<Triple<ComponentType, Long, List<String>>> =
        graph.children.flatMap { child ->
            when (child) {
                is GroupGraphItem.GroupNode -> {
                    val childPath = currentPath + child.groupGraph.group.name
                    listOf(Triple(ComponentType.GROUP, child.groupGraph.group.id, childPath)) +
                        walk(child.groupGraph, childPath)
                }
                is GroupGraphItem.TrackerNode ->
                    listOf(Triple(ComponentType.TRACKER, child.tracker.id, currentPath + child.tracker.name))
                is GroupGraphItem.FunctionNode ->
                    listOf(Triple(ComponentType.FUNCTION, child.function.id, currentPath + child.function.name))
                is GroupGraphItem.GraphNode ->
                    listOf(Triple(ComponentType.GRAPH, child.graph.id, currentPath + child.graph.name))
            }
        }

    private fun formatPath(segments: List<String>): String =
        if (segments.isEmpty()) separator
        else separator + segments.joinToString(separator)
}
