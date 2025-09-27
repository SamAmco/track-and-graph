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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import com.samco.trackandgraph.data.lua.dto.TranslatedString

/**
 * Sealed class representing different types of configuration inputs for Lua scripts.
 * Each type contains mutable state for UI interaction and knows its corresponding LuaFunctionConfigType.
 */
sealed class LuaScriptConfigurationInput {
    abstract val type: LuaFunctionConfigType
    abstract val name: TranslatedString?
    
    /**
     * Text input configuration with mutable state for text field value
     */
    data class Text(
        override val name: TranslatedString?,
        val value: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    ) : LuaScriptConfigurationInput() {
        override val type: LuaFunctionConfigType = LuaFunctionConfigType.TEXT
    }
    
    /**
     * Number input configuration with mutable state for numeric value
     */
    data class Number(
        override val name: TranslatedString?,
        val value: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    ) : LuaScriptConfigurationInput() {
        override val type: LuaFunctionConfigType = LuaFunctionConfigType.NUMBER
    }
}
