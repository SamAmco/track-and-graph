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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfig
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import timber.log.Timber
import javax.inject.Inject

/**
 * Provider class that uses the LuaEngine to extract configuration metadata from Lua scripts
 * and returns complete Node.LuaScript instances with proper configuration and error handling.
 */
internal class LuaScriptNodeProvider @Inject constructor(
    private val luaEngine: LuaEngine
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
    fun createLuaScriptNode(
        script: String,
        nodeId: Int,
        inputConnectorCount: Int,
        configuration: List<LuaScriptConfigurationValue>
    ): Node.LuaScript {
        return try {
            val metadata = luaEngine.runLuaFunction(script)

            val configMap = configuration.associateBy { it.id }

            val inputs = metadata.config.associate { config ->
                config.id to createConfigurationInput(config, configMap[config.id])
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
                inputConnectorCount = inputConnectorCount ?: 1,
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
                    createConfigurationInput(config, null)
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

    /**
     * Creates a new configuration input for the given type.
     */
    private fun createConfigurationInput(
        config: LuaFunctionConfig,
        value: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput {
        return when (config.type) {
            LuaFunctionConfigType.TEXT -> createTextConfigurationInput(config.name, value)
            LuaFunctionConfigType.NUMBER -> createNumberConfigurationInput(config.name, value)
        }
    }

    /**
     * Creates a Text configuration input, restoring the saved value if available and type-compatible.
     */
    private fun createTextConfigurationInput(
        name: TranslatedString?,
        value: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Text {
        val textValue = value as? LuaScriptConfigurationValue.Text
        return if (textValue != null) {
            LuaScriptConfigurationInput.Text(
                name = name,
                value = mutableStateOf(TextFieldValue(textValue.value))
            )
        } else {
            LuaScriptConfigurationInput.Text(name = name)
        }
    }

    /**
     * Creates a Number configuration input, restoring the saved value if available and type-compatible.
     */
    private fun createNumberConfigurationInput(
        name: TranslatedString?,
        value: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Number {
        val numberValue = value as? LuaScriptConfigurationValue.Number
        return if (numberValue != null) {
            LuaScriptConfigurationInput.Number(
                name = name,
                value = mutableStateOf(TextFieldValue(numberValue.value.toString()))
            )
        } else {
            LuaScriptConfigurationInput.Number(name = name)
        }
    }
}
