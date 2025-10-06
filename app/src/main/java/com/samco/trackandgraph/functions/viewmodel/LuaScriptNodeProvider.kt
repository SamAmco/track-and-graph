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
package com.samco.trackandgraph.functions.viewmodel

import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import timber.log.Timber
import javax.inject.Inject

/**
 * Provider class that uses the LuaEngine to extract configuration metadata from Lua scripts
 * and returns complete Node.LuaScript instances with proper configuration and error handling.
 */
internal class LuaScriptNodeProvider @Inject constructor(
    private val luaEngine: LuaEngine,
    private val configInputFactory: LuaScriptConfigurationInputFactory
) {
    /**
     * Analyzes a Lua script and returns a complete Node.LuaScript instance.
     * If script analysis fails, returns a fallback node with the provided inputConnectorCount.
     *
     * @param script The Lua script to analyze
     * @param nodeId The ID for the node
     * @param inputConnectorCount Input connector count stored in the database for this node.
     *     We fall back to this if script analysis fails, but prefer the script metadata result.
     * @param configuration The configuration values stored in the database for this node.
     */
    suspend fun createLuaScriptNode(
        script: String,
        nodeId: Int,
        inputConnectorCount: Int,
        configuration: List<LuaScriptConfigurationValue>
    ): Node.LuaScript {
        var vmLock: LuaVMLock? = null
        return try {
            vmLock = luaEngine.acquireVM()
            val metadata = luaEngine.runLuaFunction(vmLock, script)
            createLuaScriptNode(metadata, nodeId, configuration)
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze Lua script for node $nodeId, using fallback")
            // Return fallback node with provided inputConnectorCount or default to 1
            Node.LuaScript(
                id = nodeId,
                inputConnectorCount = inputConnectorCount,
                script = script,
                showEditTools = true,
                configuration = emptyMap()
            )
        } finally {
            vmLock?.let { luaEngine.releaseVM(vmLock) }
        }
    }

    /**
     * Creates a LuaScript node directly from already-available metadata (no parsing).
     * Useful when metadata is fetched from a repository or cache.
     */
    fun createLuaScriptNode(
        metadata: LuaFunctionMetadata,
        nodeId: Int,
        configuration: List<LuaScriptConfigurationValue> = emptyList()
    ): Node.LuaScript {
        val configMap = configuration.associateBy { it.id }
        val inputs = metadata.config.associate { config ->
            config.id to configInputFactory.createConfigurationInput(config, configMap[config.id])
        }

        return Node.LuaScript(
            id = nodeId,
            inputConnectorCount = metadata.inputCount,
            script = metadata.script,
            showEditTools = metadata.version == null,
            configuration = inputs,
            title = metadata.title,
        )
    }

    /**
     * Updates an existing LuaScript node with a new script while preserving existing configuration inputs
     * where possible. If a configuration input exists in both old and new with the same type, the old
     * value is preserved. New inputs are created for new configurations, and old inputs that no longer
     * exist in the new script are dropped.
     */
    suspend fun updateLuaScriptNode(
        existingNode: Node.LuaScript,
        newScript: String
    ): Node.LuaScript {
        var vmLock: LuaVMLock? = null
        return try {
            vmLock = luaEngine.acquireVM()
            val metadata = luaEngine.runLuaFunction(vmLock, newScript)

            // Create new configuration map by iterating through new metadata
            val newConfiguration = metadata.config.associate { config ->
                config.id to recoverConfigOrNew(config, existingNode)
            }

            existingNode.copy(
                script = newScript,
                inputConnectorCount = metadata.inputCount,
                showEditTools = metadata.version == null,
                configuration = newConfiguration,
                title = metadata.title,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update Lua script for node ${existingNode.id}, using fallback")
            // Return fallback node with inputConnectorCount = 1 on error
            existingNode.copy(
                script = newScript,
                inputConnectorCount = 1,
            )
        } finally {
            vmLock?.let { luaEngine.releaseVM(it) }
        }
    }

    /**
     * Checks if the existing input is compatible with the new config spec type.
     */
    private fun recoverConfigOrNew(
        config: LuaFunctionConfigSpec,
        existingNode: Node.LuaScript,
    ): LuaScriptConfigurationInput {
        val existingInput = existingNode.configuration[config.id]
        return configInputFactory.createOrRecoverInput(config, existingInput)
    }
}
