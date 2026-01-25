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

import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.localisation.TranslatedString
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

    /**
     * Parse a Lua value into a TranslatedString with translation key lookup support
     * @param value The Lua value (can be string or table of translations)
     * @param translations Optional table of translation keys to look up
     * @param usedTranslations Optional mutable map to track which translations were actually looked up
     * @return TranslatedString or null if value is nil or invalid
     */
    fun parseWithLookup(
        value: LuaValue,
        translations: LocalizationsTable?,
        usedTranslations: MutableMap<String, TranslatedString>? = null
    ): TranslatedString? {
        return when {
            value.isnil() -> null
            value.isstring() -> {
                val stringValue = value.checkjstring()!!
                // If we have a translations table, try to look up the string
                val lookedUp = translations?.get(stringValue)
                if (lookedUp != null) {
                    // Track that this translation was used
                    usedTranslations?.put(stringValue, lookedUp)
                    lookedUp
                } else {
                    TranslatedString.Simple(stringValue)
                }
            }
            value.istable() -> parse(value) // Use existing table parsing logic
            else -> null
        }
    }
}
