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

package com.samco.trackandgraph.data.dependencyanalyser

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao

/**
 * Result containing graphs that need updating when a feature changes.
 */
internal data class DependentGraphs(
    val graphStatIds: Set<Long>
)

/**
 * Result containing graphs that are orphaned and should be deleted.
 */
internal data class OrphanedGraphs(
    val graphStatIds: Set<Long>
)

/**
 * Result containing features that depend on a given feature.
 */
internal data class DependentFeatures(
    val featureIds: Set<Long>
)

internal class DependencyAnalyser private constructor(
    private val featureNodes: Map<Long, DependencyNode.Feature>,
    private val graphNodes: Map<Long, DependencyNode.Graph>
) {

    /**
     * Node in the dependency graph - properly typed to avoid ID collisions
     */
    private sealed class DependencyNode {
        class Feature(
            val featureId: Long,
            val dependents: MutableSet<DependencyNode> = mutableSetOf(),
            val dependencies: MutableSet<DependencyNode> = mutableSetOf()
        ) : DependencyNode() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Feature) return false
                return featureId == other.featureId
            }

            override fun hashCode(): Int = featureId.hashCode()

            override fun toString(): String = "Feature(featureId=$featureId)"
        }

        class Graph(
            val graphStatId: Long,
            val dependencies: MutableSet<DependencyNode> = mutableSetOf()
        ) : DependencyNode() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Graph) return false
                return graphStatId == other.graphStatId
            }

            override fun hashCode(): Int = graphStatId.hashCode()

            override fun toString(): String = "Graph(graphStatId=$graphStatId)"
        }
    }

    /**
     * Gets all graph stat IDs that may need updating when a feature is updated.
     * This includes graphs that depend transitively on the given feature.
     */
    fun getDependentGraphs(featureId: Long): DependentGraphs {
        val visited = mutableSetOf<DependencyNode>()
        val result = mutableSetOf<Long>()
        val toVisit = mutableListOf<DependencyNode>()

        // Start with the initial feature's dependents
        featureNodes[featureId]?.let { featureNode ->
            toVisit.addAll(featureNode.dependents)
        }

        while (toVisit.isNotEmpty()) {
            val node = toVisit.removeAt(0)

            if (node in visited) continue
            visited.add(node)

            when (node) {
                is DependencyNode.Graph -> result.add(node.graphStatId)
                is DependencyNode.Feature -> toVisit.addAll(node.dependents)
            }
        }

        return DependentGraphs(result)
    }

    /**
     * Gets all graph stat IDs that are orphaned (need deleting) in the current database state.
     * Call this after features/groups have already been deleted to find orphaned graphs.
     */
    fun getOrphanedGraphs(): OrphanedGraphs {
        val orphanedGraphs = graphNodes.values.filter { graphNode ->
            graphNode.dependencies.isEmpty() ||
                    graphNode.dependencies.all { dep ->
                        when (dep) {
                            is DependencyNode.Feature -> !featureNodes.containsKey(dep.featureId)
                            is DependencyNode.Graph -> false // graphs don't depend on other graphs
                        }
                    }
        }.map { it.graphStatId }

        return OrphanedGraphs(orphanedGraphs.toSet())
    }

    /**
     * Gets all features that depend on a given feature, either directly or transitively.
     * Used for cycle detection to prevent circular dependencies. This includes cycles
     * to the feature itself.
     */
    fun getFeaturesDependingOn(featureId: Long): DependentFeatures {
        val result = mutableSetOf(featureId)
        val visited = mutableSetOf<DependencyNode>()
        val toVisit = mutableListOf<DependencyNode>()

        // Start with the initial feature's dependents
        featureNodes[featureId]?.let { featureNode ->
            toVisit.addAll(featureNode.dependents)
        }

        while (toVisit.isNotEmpty()) {
            val node = toVisit.removeAt(0)

            if (node !is DependencyNode.Feature || node in visited) continue
            visited.add(node)

            result.add(node.featureId)
            toVisit.addAll(node.dependents)
        }

        return DependentFeatures(result)
    }

    /**
     * Checks if all the given feature IDs exist in the dependency graph.
     * Returns true if all features exist, false otherwise.
     */
    fun allFeaturesExist(featureIds: Set<Long>): Boolean {
        return featureNodes.keys.containsAll(featureIds)
    }

    companion object {
        /**
         * Creates a new DependencyAnalyzer by building the current dependency tree from the database.
         */
        suspend fun create(dao: TrackAndGraphDatabaseDao): DependencyAnalyser {
            val dependencyData = fetchDependencyData(dao)
            return buildDependencyModel(dependencyData)
        }

        /**
         * Fetches all dependency data from the database.
         */
        private suspend fun fetchDependencyData(dao: TrackAndGraphDatabaseDao): DependencyData {
            val allGraphDependencies = mutableMapOf<Long, Set<Long>>()

            // Fetch all single-dependency graphs with unified query
            dao.getAllSingleDependencyGraphs().forEach { graphDep ->
                allGraphDependencies[graphDep.graphStatId] = setOf(graphDep.featureId)
            }

            // Fetch multi-dependency graphs
            val lineGraphGraphStatIds = dao.getLineGraphGraphStatIds()
            lineGraphGraphStatIds.forEach { graphStatId ->
                val featureIds = dao.getLineGraphFeatureIds(graphStatId).toSet()
                allGraphDependencies[graphStatId] = featureIds
            }

            val luaGraphGraphStatIds = dao.getLuaGraphGraphStatIds()
            luaGraphGraphStatIds.forEach { graphStatId ->
                val featureIds = dao.getLuaGraphFeatureIds(graphStatId).toSet()
                allGraphDependencies[graphStatId] = featureIds
            }

            // Fetch function dependencies
            val functionFeatureIds = dao.getFunctionFeatureIds()
            val functionDependencies = functionFeatureIds.associateWith { functionFeatureId ->
                dao.getFunctionInputFeatureIds(functionFeatureId).toSet()
            }

            return DependencyData(
                graphDependencies = allGraphDependencies,
                featureDependencies = functionDependencies
            )
        }

        /**
         * Raw data fetched from the database before building the dependency model
         */
        private data class DependencyData(
            // graphStatId -> Set<featureId>
            val graphDependencies: Map<Long, Set<Long>>,
            // featureId -> Set<featureId>
            val featureDependencies: Map<Long, Set<Long>>
        )


        /**
         * Builds the dependency model from raw data.
         */
        private fun buildDependencyModel(data: DependencyData): DependencyAnalyser {
            val featureNodes = mutableMapOf<Long, DependencyNode.Feature>()
            val graphNodes = mutableMapOf<Long, DependencyNode.Graph>()

            // Helper to get or create feature node
            fun getOrCreateFeatureNode(featureId: Long): DependencyNode.Feature {
                return featureNodes.getOrPut(featureId) { DependencyNode.Feature(featureId) }
            }

            // Helper to get or create graph node
            fun getOrCreateGraphNode(graphStatId: Long): DependencyNode.Graph {
                return graphNodes.getOrPut(graphStatId) { DependencyNode.Graph(graphStatId) }
            }

            // Build all graph dependencies
            data.graphDependencies.forEach { (graphStatId, featureIds) ->
                val graphNode = getOrCreateGraphNode(graphStatId)

                featureIds.forEach { featureId ->
                    val featureNode = getOrCreateFeatureNode(featureId)
                    featureNode.dependents.add(graphNode)
                    graphNode.dependencies.add(featureNode)
                }
            }

            // Build function dependencies (feature -> feature)
            data.featureDependencies.forEach { (functionFeatureId, inputFeatureIds) ->
                val functionNode = getOrCreateFeatureNode(functionFeatureId)

                inputFeatureIds.forEach { inputFeatureId ->
                    val inputNode = getOrCreateFeatureNode(inputFeatureId)
                    inputNode.dependents.add(functionNode)
                    functionNode.dependencies.add(inputNode)
                }
            }

            return DependencyAnalyser(
                featureNodes = featureNodes,
                graphNodes = graphNodes
            )
        }
    }
}
