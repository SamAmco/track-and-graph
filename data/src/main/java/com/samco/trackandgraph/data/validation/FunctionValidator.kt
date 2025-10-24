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

package com.samco.trackandgraph.data.validation

import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyserProvider
import javax.inject.Inject

/**
 * Validates functions for consistency and correctness before database operations.
 * Performs both inter-function cycle detection (cycles between different functions/features)
 * and intra-function cycle detection (cycles within a function's own node graph).
 */
internal class FunctionValidator @Inject constructor(
    private val dependencyAnalyserProvider: DependencyAnalyserProvider
) {
    /**
     * Validates a function before insert or update.
     *
     * @param function The function to validate
     * @throws IllegalStateException if validation fails
     */
    suspend fun validateFunction(function: Function) {
        // Validate inter-function dependencies (cycles between different features)
        validateInterFunctionDependencies(function)

        // Validate intra-function dependencies (cycles within the function's node graph)
        validateIntraFunctionDependencies(function.functionGraph.nodes)
    }

    /**
     * Validates that the function doesn't create circular dependencies with other features.
     * Checks if any of the function's declared dependencies already depend on this function
     * (either directly or transitively).
     */
    private suspend fun validateInterFunctionDependencies(function: Function) {
        val featureNodeDependencies = function.functionGraph.nodes.mapNotNull {
            when (it) {
                is FunctionGraphNode.FeatureNode -> it.featureId
                is FunctionGraphNode.OutputNode -> null
                is FunctionGraphNode.LuaScriptNode -> null
            }
        }.toSet()

        val declaredDependencies = function.inputFeatureIds.toSet()

        if (featureNodeDependencies != declaredDependencies) {
            error("Feature node dependencies do not match declared dependencies")
        }

        // Skip validation if there are no dependencies
        if (declaredDependencies.isEmpty()) return

        val dependencyAnalyser = dependencyAnalyserProvider.create()
        val dependentFeatures = dependencyAnalyser
            .getFeaturesDependingOn(function.featureId)
            .featureIds

        if (declaredDependencies.intersect(dependentFeatures).isNotEmpty()) {
            error("Function cycle detected: One or more of this function's dependencies already depends on this function")
        }
    }

    /**
     * Validates that there are no cyclic dependencies within the function's node graph.
     * Uses depth-first search with a three-color algorithm (white-gray-black) to detect cycles.
     *
     * @param nodes All nodes in the function graph
     * @throws IllegalStateException if a cycle is detected
     */
    private fun validateIntraFunctionDependencies(nodes: List<FunctionGraphNode>) {
        // Skip validation if there are no nodes
        if (nodes.isEmpty()) {
            return
        }

        // Create a map of node ID to node for quick lookup
        val nodeMap = nodes.associateBy { it.id }

        // Track node states: 0 = white (unvisited), 1 = gray (visiting), 2 = black (visited)
        val nodeStates = mutableMapOf<Int, Int>()

        // Initialize all nodes as white (unvisited)
        nodes.forEach { node ->
            nodeStates[node.id] = 0
        }

        // Perform DFS from each unvisited node
        nodes.forEach { node ->
            if (nodeStates[node.id] == 0) {
                if (hasCycleDFS(node.id, nodeMap, nodeStates)) {
                    error("Cyclic dependency detected in function graph starting from node ${node.id}")
                }
            }
        }
    }

    /**
     * Performs depth-first search to detect cycles starting from the given node.
     *
     * @param nodeId The current node ID being visited
     * @param nodeMap Map of node ID to FunctionGraphNode for quick lookup
     * @param nodeStates Map tracking the state of each node (0=white, 1=gray, 2=black)
     * @return true if a cycle is detected, false otherwise
     */
    private fun hasCycleDFS(
        nodeId: Int,
        nodeMap: Map<Int, FunctionGraphNode>,
        nodeStates: MutableMap<Int, Int>
    ): Boolean {
        // Mark current node as gray (being visited)
        nodeStates[nodeId] = 1

        val currentNode = nodeMap[nodeId] ?: return false

        // Visit all dependencies (adjacent nodes)
        for (dependency in currentNode.dependencies) {
            val dependencyNodeId = dependency.nodeId

            when (nodeStates[dependencyNodeId]) {
                1 -> {
                    // Gray node found - cycle detected!
                    return true
                }

                0 -> {
                    // White node - recursively visit
                    if (hasCycleDFS(dependencyNodeId, nodeMap, nodeStates)) {
                        return true
                    }
                }

                2 -> {
                    // Black node (2) - already processed, safe to ignore
                }
            }
        }

        // Mark current node as black (fully processed)
        nodeStates[nodeId] = 2
        return false
    }
}
