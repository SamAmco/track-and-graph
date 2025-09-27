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

import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfig
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import timber.log.Timber
import javax.inject.Inject

/**
 * Provider class that uses the LuaEngine to extract configuration metadata from Lua scripts
 * and returns complete Node.LuaScript instances with proper configuration and error handling.
 */
internal class LuaScriptConfigurationProvider @Inject constructor(
    private val luaEngine: LuaEngine
) {
    
    /**
     * Creates a new configuration input for the given type.
     */
    private fun createConfigurationInput(config: LuaFunctionConfig): LuaScriptConfigurationInput {
        return when (config.type) {
            LuaFunctionConfigType.TEXT -> LuaScriptConfigurationInput.Text(name = config.name)
            LuaFunctionConfigType.NUMBER -> LuaScriptConfigurationInput.Number(name = config.name)
        }
    }
    
    /**
     * Analyzes a Lua script and returns a complete Node.LuaScript instance.
     * If script analysis fails, returns a fallback node with the provided inputConnectorCount.
     * 
     * @param script The Lua script to analyze
     * @param nodeId The ID for the node
     * @param fallbackInputConnectorCount Optional input connector count to use if script analysis fails.
     *        If not provided, defaults to 1 when script analysis fails.
     */
    fun createLuaScriptNode(
        script: String, 
        nodeId: Int, 
        fallbackInputConnectorCount: Int? = null
    ): Node.LuaScript {
        return try {
            val metadata = luaEngine.runLuaFunction(script)
            
            val inputs = metadata.config.associate { config ->
                config.id to createConfigurationInput(config)
            }
            
            // Always prefer the script metadata inputCount when script analysis succeeds
            Node.LuaScript(
                id = nodeId,
                inputConnectorCount = metadata.inputCount,
                script = script,
                configuration = inputs
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze Lua script for node $nodeId, using fallback")
            // Return fallback node with provided inputConnectorCount or default to 1
            Node.LuaScript(
                id = nodeId,
                inputConnectorCount = fallbackInputConnectorCount ?: 1,
                script = script,
                configuration = emptyMap()
            )
        }
    }
    
    /**
     * Updates an existing LuaScript node with a new script while preserving existing configuration inputs
     * where possible. If a configuration input exists in both old and new with the same type, the old
     * value is preserved. New inputs are created for new configurations, and old inputs that no longer
     * exist in the new script are dropped.
     */
    fun updateLuaScriptNode(existingNode: Node.LuaScript, newScript: String): Node.LuaScript {
        return try {
            val metadata = luaEngine.runLuaFunction(newScript)
            
            // Create new configuration map by iterating through new metadata
            val newConfiguration = metadata.config.associate { config ->
                val existingInput = existingNode.configuration[config.id]
                val input = if (existingInput?.type == config.type) {
                    // Use existing input if it exists and has the same type
                    existingInput
                } else {
                    // Create new input if type changed or didn't exist
                    createConfigurationInput(config)
                }
                config.id to input
            }
            
            existingNode.copy(
                script = newScript,
                inputConnectorCount = metadata.inputCount,
                configuration = newConfiguration
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update Lua script for node ${existingNode.id}, using fallback")
            // Return fallback node with inputConnectorCount = 1 on error
            existingNode.copy(
                script = newScript,
                inputConnectorCount = 1,
                configuration = emptyMap() // Clear config on error since we can't validate it
            )
        }
    }
}
