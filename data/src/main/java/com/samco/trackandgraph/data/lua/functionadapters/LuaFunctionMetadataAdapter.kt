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
import com.samco.trackandgraph.data.lua.dto.EnumOption
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaFunctionMetadataAdapter @Inject constructor(
    private val translatedStringParser: TranslatedStringParser
) {

    companion object {
        private const val DEFAULT_INPUT_COUNT = 1
        const val ID = "id"
        const val NAME = "name"
        const val CONFIG = "config"
        const val TYPE = "type"
        const val OPTIONS = "options"
        private const val VERSION = "version"
        private const val INPUT_COUNT = "inputCount"
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val DEFAULT = "default"
    }

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
        val idValue = resolvedScript[ID]
        val versionValue = resolvedScript[VERSION]
        val inputCountValue = resolvedScript[INPUT_COUNT]

        return LuaFunctionMetadata(
            script = originalScript,
            id = if (!idValue.isstring()) null else idValue.tojstring(),
            version = if (versionValue.isnil()) null else versionValue.checkjstring()!!.toVersion(),
            title = translatedStringParser.parse(resolvedScript[TITLE]),
            description = translatedStringParser.parse(resolvedScript[DESCRIPTION]),
            inputCount = if (inputCountValue.isnil()) 1 else inputCountValue.checkint(),
            config = extractConfigs(resolvedScript)
        )
    }

    private fun extractConfigs(resolvedScript: LuaValue): List<LuaFunctionConfigSpec> {
        val configArray = resolvedScript[CONFIG]
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
        val id = configItem[ID].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have an id")

        val typeString = configItem[TYPE].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have a type")

        val nameTranslations = translatedStringParser.parse(configItem[NAME])

        return when (typeString) {
            "text" -> parseTextConfig(id, nameTranslations, configItem)
            "number" -> parseNumberConfig(id, nameTranslations, configItem)
            "checkbox" -> parseCheckboxConfig(id, nameTranslations, configItem)
            "enum" -> parseEnumConfig(id, nameTranslations, configItem)
            else -> throw IllegalArgumentException("Unknown config type: $typeString")
        }
    }

    private fun parseTextConfig(
        id: String,
        name: TranslatedString?,
        configItem: LuaValue
    ): LuaFunctionConfigSpec.Text {
        val defaultValue = configItem[DEFAULT]
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
        val defaultValue = configItem[DEFAULT]
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
        val defaultValue = configItem[DEFAULT]
            .takeUnless { it.isnil() }?.toboolean()
        return LuaFunctionConfigSpec.Checkbox(
            id = id,
            name = name,
            defaultValue = defaultValue
        )
    }

    private fun parseEnumConfig(
        id: String,
        name: TranslatedString?,
        configItem: LuaValue
    ): LuaFunctionConfigSpec.Enum {
        val defaultValue = configItem[DEFAULT]
            .takeUnless { it.isnil() }?.checkjstring()

        val optionsValue = configItem[OPTIONS]
        val options = if (optionsValue.isnil() || !optionsValue.istable()) {
            emptyList()
        } else {
            val optionsTable = optionsValue.checktable()!!
            val enumOptions = mutableListOf<EnumOption>()

            // Iterate over integer keys (array format, preserves order)
            var i = 1
            while (true) {
                val option = optionsTable[i]
                if (option.isnil()) break

                if (option.istable()) {
                    val optionTable = option.checktable()!!
                    val enumId = optionTable[ID]
                    val name = optionTable[NAME]

                    if (enumId.isstring()) {
                        val optionId = enumId.checkjstring()!!
                        val displayName = translatedStringParser.parse(name)
                        if (displayName != null) {
                            enumOptions.add(EnumOption(optionId, displayName))
                        }
                    }
                }

                i++
            }

            enumOptions
        }

        return LuaFunctionConfigSpec.Enum(
            id = id,
            name = name,
            options = options,
            defaultValue = defaultValue
        )
    }

}
