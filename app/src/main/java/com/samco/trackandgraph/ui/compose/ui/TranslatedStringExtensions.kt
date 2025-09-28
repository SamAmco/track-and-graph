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
package com.samco.trackandgraph.ui.compose.ui

import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import java.util.Locale

/**
 * Extension function to resolve a TranslatedString to the best matching localized text
 * based on the user's locale preferences.
 */
@Composable
fun TranslatedString?.resolve(): String? {
    if (this == null) return null
    return when (this) {
        is TranslatedString.Simple -> value
        is TranslatedString.Translations -> resolveTranslated()
    }
}

@Composable
private fun TranslatedString.Translations?.resolveTranslated(): String? {
    if (this == null) return null
    if (values.isEmpty()) return ""

    val configuration = LocalConfiguration.current

    return remember(this) {
        // Normalize keys to BCP-47 tags
        val supported = values.keys
            .map { Locale.forLanguageTag(it).toLanguageTag() }
            .toTypedArray()

        // Ask Android for the best match given user's locale prefs
        val userLocales: LocaleList = configuration.locales
        val best: Locale? = userLocales.getFirstMatch(supported)

        // Exact match?
        best?.toLanguageTag()?.let { tag ->
            values[tag]?.let { return@remember it }
        }

        // Language-only fallback
        best?.language?.let { lang ->
            values[lang]?.let { return@remember it }
        }

        // Last-ditch fallback
        values.values.firstOrNull()
    }
}
