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
import javax.inject.Inject

/**
 * Encoder class responsible for converting LuaScript configuration inputs from ViewModel format
 * to database format for serialization and persistence.
 */
internal class LuaScriptConfigurationEncoder @Inject constructor() {

    /**
     * Encodes a map of configuration inputs from ViewModel format to database format.
     * 
     * @param configuration Map of configuration ID to LuaScriptConfigurationInput from ViewModel
     * @return List of LuaScriptConfigurationValue ready for database serialization
     */
    fun encodeConfiguration(
        configuration: Map<String, LuaScriptConfigurationInput>
    ): List<LuaScriptConfigurationValue> {
        return configuration.map { (id, input) ->
            when (input) {
                is LuaScriptConfigurationInput.Text -> {
                    LuaScriptConfigurationValue.Text(
                        id = id,
                        value = input.value.value.text
                    )
                }
                is LuaScriptConfigurationInput.Number -> {
                    // Parse the text field value to double, defaulting to 1.0 if invalid
                    val doubleValue = input.value.value.text.toDoubleOrNull() ?: 1.0
                    LuaScriptConfigurationValue.Number(
                        id = id,
                        value = doubleValue
                    )
                }
                is LuaScriptConfigurationInput.Checkbox -> {
                    LuaScriptConfigurationValue.Checkbox(
                        id = id,
                        value = input.value.value
                    )
                }
                is LuaScriptConfigurationInput.Enum -> {
                    LuaScriptConfigurationValue.Enum(
                        id = id,
                        value = input.value.value
                    )
                }
            }
        }
    }
}
