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
package com.samco.trackandgraph.data.lua.dto

enum class LuaFunctionConfigType {
    TEXT,
    NUMBER
}

sealed class TranslatedString {
    data class Simple(val value: String) : TranslatedString()

    /**
     * A map of BCP 47 language codes to translations
     */
    data class Translations(val values: Map<String, String>) : TranslatedString()
}

data class LuaFunctionConfig(
    val id: String,
    val type: LuaFunctionConfigType,
    val name: TranslatedString?,
)

data class LuaFunctionMetadata(
    val script: String,
    val inputCount: Int,
    val config: List<LuaFunctionConfig>
)
