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

import io.github.z4kn4fein.semver.Version

sealed class TranslatedString {
    data class Simple(val value: String) : TranslatedString()

    /**
     * A map of BCP 47 language codes to translations
     */
    data class Translations(val values: Map<String, String>) : TranslatedString()
}

/**
 * Represents the specification/metadata for a Lua function configuration parameter.
 * This sealed class defines the structure and constraints for different types of
 * configuration inputs that can be defined in Lua script metadata.
 */
sealed class LuaFunctionConfigSpec {
    /**
     * The unique identifier for this configuration parameter.
     * Must match the ID used in the Lua script metadata.
     */
    abstract val id: String

    /**
     * The display name for this configuration parameter.
     * Can be a simple string or translations for multiple languages.
     */
    abstract val name: TranslatedString?
    
    data class Text(
        override val id: String,
        override val name: TranslatedString?,
        val defaultValue: String? = null,
    ) : LuaFunctionConfigSpec()
    
    data class Number(
        override val id: String,
        override val name: TranslatedString?,
        val defaultValue: Double? = null,
    ) : LuaFunctionConfigSpec()
    
    data class Checkbox(
        override val id: String,
        override val name: TranslatedString?,
        val defaultValue: Boolean? = null
    ) : LuaFunctionConfigSpec()
}

data class LuaFunctionMetadata(
    val script: String,
    val version: Version?,
    val title: TranslatedString?,
    val inputCount: Int,
    val config: List<LuaFunctionConfigSpec>,
)
