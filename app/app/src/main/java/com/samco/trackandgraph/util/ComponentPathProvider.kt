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

class ComponentPathProvider(groupGraph: GroupGraph) {

    private val separator = "/"

    private val groupPathsById = mutableMapOf<Long, MutableList<List<String>>>()
    private val trackerPathsById = mutableMapOf<Long, MutableList<List<String>>>()
    private val functionPathsById = mutableMapOf<Long, MutableList<List<String>>>()
    private val graphPathsById = mutableMapOf<Long, MutableList<List<String>>>()

    init {
        groupPathsById[groupGraph.group.id] = mutableListOf(emptyList())

        fun walk(graph: GroupGraph, currentPath: List<String>) {
            for (child in graph.children) {
                when (child) {
                    is GroupGraphItem.GroupNode -> {
                        val childGroup = child.groupGraph.group
                        val childPath = currentPath + childGroup.name
                        groupPathsById.getOrPut(childGroup.id) { mutableListOf() }
                            .add(childPath)
                        walk(child.groupGraph, childPath)
                    }
                    is GroupGraphItem.TrackerNode -> {
                        trackerPathsById.getOrPut(child.tracker.id) { mutableListOf() }
                            .add(currentPath + child.tracker.name)
                    }
                    is GroupGraphItem.FunctionNode -> {
                        functionPathsById.getOrPut(child.function.id) { mutableListOf() }
                            .add(currentPath + child.function.name)
                    }
                    is GroupGraphItem.GraphNode -> {
                        graphPathsById.getOrPut(child.graph.id) { mutableListOf() }
                            .add(currentPath + child.graph.name)
                    }
                }
            }
        }

        walk(groupGraph, emptyList())
    }

    fun getAllPathsForGroup(groupId: Long): List<String> {
        return formatPaths(groupPathsById[groupId])
    }

    fun getAllPathsForTracker(trackerId: Long): List<String> {
        return formatPaths(trackerPathsById[trackerId])
    }

    fun getAllPathsForFunction(functionId: Long): List<String> {
        return formatPaths(functionPathsById[functionId])
    }

    fun getAllPathsForGraph(graphId: Long): List<String> {
        return formatPaths(graphPathsById[graphId])
    }

    private fun formatPaths(paths: List<List<String>>?): List<String> {
        if (paths == null) return emptyList()
        return paths.map { segments ->
            if (segments.isEmpty()) separator
            else separator + segments.joinToString(separator)
        }
    }
}
