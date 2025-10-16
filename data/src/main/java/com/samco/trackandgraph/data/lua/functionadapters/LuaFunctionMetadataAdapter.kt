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
package com.samco.trackandgraph.data.lua.functionadapters

import com.samco.trackandgraph.data.lua.apiimpl.TranslatedStringParser
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaFunctionMetadataAdapter @Inject constructor(
    private val translatedStringParser: TranslatedStringParser
) {

    fun process(resolvedScript: LuaValue, originalScript: String): LuaFunctionMetadata {
        // If the script is just a function, use defaults
        if (resolvedScript.isfunction()) {
            return LuaFunctionMetadata(
                script = originalScript,
                id = null,
                inputCount = 1,
                version = null,
                title = null,
                description = null,
                config = emptyList()
            )
        }

        // Otherwise, extract metadata from table
        val idValue = resolvedScript["id"]
        val versionValue = resolvedScript["version"]
        val inputCountValue = resolvedScript["inputCount"]

        return LuaFunctionMetadata(
            script = originalScript,
            id = if (!idValue.isstring()) null else idValue.tojstring(),
            version = if (versionValue.isnil()) null else versionValue.checkjstring()!!.toVersion(),
            title = translatedStringParser.parse(resolvedScript["title"]),
            description = translatedStringParser.parse(resolvedScript["description"]),
            inputCount = if (inputCountValue.isnil()) 1 else inputCountValue.checkint(),
            config = extractConfigs(resolvedScript)
        )
    }

    private fun extractConfigs(resolvedScript: LuaValue): List<LuaFunctionConfigSpec> {
        val configArray = resolvedScript["config"]
        val configs = mutableListOf<LuaFunctionConfigSpec>()

        if (!configArray.isnil()) {
            var i = 1
            while (true) {
                val configItem = configArray[i]
                if (configItem.isnil()) break

                configs.add(parseConfigItem(configItem, i))
                i++
            }
        }

        return configs
    }

    private fun parseConfigItem(configItem: LuaValue, index: Int): LuaFunctionConfigSpec {
        val id = configItem["id"].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have an id")

        val typeString = configItem["type"].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have a type")

        val nameTranslations = translatedStringParser.parse(configItem["name"])

        return when (typeString) {
            "text" -> parseTextConfig(id, nameTranslations, configItem)
            "number" -> parseNumberConfig(id, nameTranslations, configItem)
            "checkbox" -> parseCheckboxConfig(id, nameTranslations, configItem)
            else -> throw IllegalArgumentException("Unknown config type: $typeString")
        }
    }

    private fun parseTextConfig(
        id: String,
        name: TranslatedString?,
        configItem: LuaValue
    ): LuaFunctionConfigSpec.Text {
        val defaultValue = configItem["default"]
            .takeUnless { it.isnil() }?.checkjstring()
        return LuaFunctionConfigSpec.Text(
            id = id,
            name = name,
            defaultValue = defaultValue
        )
    }

    private fun parseNumberConfig(
        id: String,
        name: TranslatedString?,
        configItem: LuaValue
    ): LuaFunctionConfigSpec.Number {
        val defaultValue = configItem["default"]
            .takeUnless { it.isnil() }?.todouble()
        return LuaFunctionConfigSpec.Number(
            id = id,
            name = name,
            defaultValue = defaultValue
        )
    }

    private fun parseCheckboxConfig(
        id: String,
        name: TranslatedString?,
        configItem: LuaValue
    ): LuaFunctionConfigSpec.Checkbox {
        val defaultValue = configItem["default"]
            .takeUnless { it.isnil() }?.toboolean()
        return LuaFunctionConfigSpec.Checkbox(
            id = id,
            name = name,
            defaultValue = defaultValue
        )
    }

}
