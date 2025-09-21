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

import com.samco.trackandgraph.data.BuildConfig
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.sampling.RawDataSample
import timber.log.Timber

internal class LuaScriptNodeRawDataSample(
    private val luaScriptNode: FunctionGraphNode.LuaScriptNode,
    private val luaEngine: LuaEngine,
    private val getDataSourceForNodeId: (Int) -> NodeRawDataSample?
) : NodeRawDataSample() {

    private val mergedDataSources: List<RawDataSample> by lazy {
        createMergedDataSources()
    }

    private val luaResult: Sequence<DataPoint> by lazy {
        try {
            luaEngine.runLuaFunctionScript(luaScriptNode.script, mergedDataSources)
        } catch (e: Exception) {
            // Return empty sequence if Lua execution fails
            Timber.e(e, "Failed to run Lua script: ${luaScriptNode.script}")
            if (BuildConfig.DEBUG) throw e
            emptySequence()
        }
    }

    private fun createMergedDataSources(): List<RawDataSample> {
        // Group dependencies by input connector
        val dependenciesByConnector = luaScriptNode.dependencies
            .groupBy { it.connectorIndex }
            .toSortedMap() // Ensure consistent ordering

        // Create a merge node for each input connector
        return (0 until luaScriptNode.inputConnectorCount).map { connectorIndex ->
            val dependencies = dependenciesByConnector[connectorIndex] ?: emptyList()
            val nodeIds = dependencies.map { it.nodeId }

            when {
                nodeIds.isEmpty() -> empty()
                nodeIds.size == 1 -> getDataSourceForNodeId(nodeIds[0]) ?: empty()
                else -> {
                    // Multiple dependencies - create merge node
                    val downStreamSources = nodeIds.associateWith { getDataSourceForNodeId(it) }
                    MergeNode(downStreamSources)
                }
            }
        }
    }

    override fun dispose() {
        mergedDataSources.forEach { it.dispose() }
    }

    override fun iterator(): Iterator<DataPoint> {
        return luaResult.iterator()
    }
}
