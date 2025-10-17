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

package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.lua.dto.TranslatedString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Database-serializable version of TranslatedString.
 * This keeps database concerns separate from Lua DTO types.
 */
@Serializable
sealed class SerializableTranslatedString {

    @Serializable
    @SerialName("Simple")
    data class Simple(val value: String) : SerializableTranslatedString()

    @Serializable
    @SerialName("Translations")
    data class Translations(val translations: Map<String, String>) : SerializableTranslatedString()
}

/**
 * Convert from Lua DTO TranslatedString to database SerializableTranslatedString
 */
fun TranslatedString.toSerializable(): SerializableTranslatedString = when (this) {
    is TranslatedString.Simple -> SerializableTranslatedString.Simple(value)
    is TranslatedString.Translations -> SerializableTranslatedString.Translations(values)
}

/**
 * Convert from database SerializableTranslatedString to Lua DTO TranslatedString
 */
fun SerializableTranslatedString.toTranslatedString(): TranslatedString = when (this) {
    is SerializableTranslatedString.Simple -> TranslatedString.Simple(value)
    is SerializableTranslatedString.Translations -> TranslatedString.Translations(translations)
}

/**
 * Convert a map of TranslatedStrings to SerializableTranslatedStrings
 */
fun Map<String, TranslatedString>.toSerializable(): Map<String, SerializableTranslatedString> =
    mapValues { it.value.toSerializable() }

/**
 * Convert a map of SerializableTranslatedStrings to TranslatedStrings
 */
fun Map<String, SerializableTranslatedString>.toTranslatedStrings(): Map<String, TranslatedString> =
    mapValues { it.value.toTranslatedString() }
