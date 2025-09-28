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

import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfig
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaFunctionMetadataAdapter @Inject constructor() {

    fun process(resolvedScript: LuaValue, originalScript: String): LuaFunctionMetadata {
        // If the script is just a function, use defaults
        if (resolvedScript.isfunction()) {
            return LuaFunctionMetadata(
                script = originalScript,
                inputCount = 1,
                version = null,
                title = null,
                config = emptyList()
            )
        }

        // Otherwise, extract metadata from table
        return LuaFunctionMetadata(
            script = originalScript,
            version = extractVersion(resolvedScript),
            title = extractTitle(resolvedScript),
            inputCount = extractInputCount(resolvedScript),
            config = extractConfigs(resolvedScript)
        )
    }

    private fun extractTitle(resolvedScript: LuaValue): TranslatedString? {
        val titleValue = resolvedScript["title"]
        return if (titleValue.isnil()) null
        else parseTranslatedString(titleValue)
    }

    private fun extractVersion(resolvedScript: LuaValue): Version? {
        val versionValue = resolvedScript["version"]
        return if (versionValue.isnil()) null
        else versionValue.checkjstring()!!.toVersion()
    }

    private fun extractInputCount(resolvedScript: LuaValue): Int {
        val inputCountValue = resolvedScript["inputCount"]
        return if (inputCountValue.isnil()) 1 else inputCountValue.checkint()
    }

    private fun extractConfigs(resolvedScript: LuaValue): List<LuaFunctionConfig> {
        val configArray = resolvedScript["config"]
        val configs = mutableListOf<LuaFunctionConfig>()

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

    private fun parseConfigItem(configItem: LuaValue, index: Int): LuaFunctionConfig {
        val id = configItem["id"].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have an id")

        val type = parseConfigType(configItem, index)
        val nameTranslations = parseTranslatedString(configItem["name"])

        return LuaFunctionConfig(
            id = id,
            type = type,
            name = nameTranslations
        )
    }

    private fun parseConfigType(configItem: LuaValue, index: Int): LuaFunctionConfigType {
        val typeString = configItem["type"].checkjstring()
            ?: throw IllegalArgumentException("Config item $index must have a type")

        return when (typeString) {
            "text" -> LuaFunctionConfigType.TEXT
            "number" -> LuaFunctionConfigType.NUMBER
            else -> throw IllegalArgumentException("Unknown config type: $typeString")
        }
    }

    private fun parseTranslatedString(translatedString: LuaValue): TranslatedString? {
        if (translatedString.isnil()) {
            return null
        }

        return if (translatedString.isstring()) {
            TranslatedString.Simple(translatedString.checkjstring()!!)
        } else if (translatedString.istable()) {
            // Handle translation table
            val translations = mutableMapOf<String, String>()
            val keys = translatedString.checktable()!!.keys()
            for (key in keys) {
                val langCode = key.checkjstring()!!
                val translation = translatedString[key].checkjstring()!!
                translations[langCode] = translation
            }
            TranslatedString.Translations(translations)
        } else {
            null
        }
    }
}
