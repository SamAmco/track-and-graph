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
package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.lua.dto.TranslatedString
import org.luaj.vm2.LuaValue
import javax.inject.Inject

/**
 * Parser for TranslatedString from Lua values
 */
internal class TranslatedStringParser @Inject constructor() {

    /**
     * Parse a Lua value into a TranslatedString
     * @param value The Lua value (can be string or table of translations)
     * @return TranslatedString or null if value is nil or invalid
     */
    fun parse(value: LuaValue): TranslatedString? {
        return when {
            value.isnil() -> null
            value.isstring() -> TranslatedString.Simple(value.checkjstring()!!)
            value.istable() -> {
                val translations = mutableMapOf<String, String>()
                val table = value.checktable()!!
                val keys = table.keys()

                for (key in keys) {
                    if (key.isstring()) {
                        val langCode = key.checkjstring()!!
                        val translation = table[key]
                        if (translation.isstring()) {
                            translations[langCode] = translation.checkjstring()!!
                        }
                    }
                }

                if (translations.isEmpty()) null
                else TranslatedString.Translations(translations)
            }
            else -> null
        }
    }
}
