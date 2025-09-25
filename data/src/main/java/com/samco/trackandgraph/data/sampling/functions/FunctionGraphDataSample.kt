/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.data.sampling.functions

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.sampling.RawDataSample

internal class FunctionGraphDataSample(
    private val function: Function,
    private val dataSources: Map<Long, RawDataSample?>,
    private val luaEngine: LuaEngine
) : RawDataSample() {

    private val functionGraph = function.functionGraph
    private val nodesById = function.functionGraph.nodes.associateBy { it.id }
    private val visited = sortedSetOf(compareByDescending<DataPoint> { it.timestamp })

    override fun dispose() {
        dataSources.values.forEach { it?.dispose() }
    }

    override fun getRawDataPoints(): List<DataPoint> {
        return visited.toList()
    }

    private fun getDataSourceForNodeId(nodeId: Int): NodeRawDataSample? {
        val node = nodesById[nodeId] ?: throw IllegalArgumentException("Node $nodeId not found")
        return when (node) {
            is FunctionGraphNode.FeatureNode -> dataSources[node.featureId]?.asNodeRawDataSample()
            is FunctionGraphNode.LuaScriptNode -> LuaScriptNodeRawDataSample(
                luaScriptNode = node,
                luaEngine = luaEngine,
                getDataSourceForNodeId = ::getDataSourceForNodeId
            )
            is FunctionGraphNode.OutputNode -> {
                throw IllegalArgumentException("Found an illegal output node in the function graph: $nodeId")
            }
        }
    }

    private fun mergeNode(nodeIds: List<Int>): NodeRawDataSample {
        val downStreamSources = nodeIds.associateWith { getDataSourceForNodeId(it) }
        return MergeNode(downStreamSources)
    }

    override fun iterator(): Iterator<DataPoint> {
        return mergeNode(functionGraph.outputNode.dependencies.map { it.nodeId })
            .onEach { visited.add(it) }
            .iterator()
    }

    companion object {
        suspend fun create(
            function: Function,
            dataSampler: DataSampler,
            luaEngine: LuaEngine
        ): FunctionGraphDataSample {
            val dataSources = function.inputFeatureIds
                .associateWith { dataSampler.getRawDataSampleForFeatureId(it) }
            return FunctionGraphDataSample(function, dataSources, luaEngine)
        }
    }
}