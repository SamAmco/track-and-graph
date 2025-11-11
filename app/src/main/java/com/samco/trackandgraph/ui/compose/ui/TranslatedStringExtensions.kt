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

/**
 * Non-Compose version that resolves using the provided LocaleList.
 */
fun TranslatedString?.resolve(locales: LocaleList): String? {
    if (this == null) return null
    return when (this) {
        is TranslatedString.Simple -> value
        is TranslatedString.Translations -> resolveTranslated(locales)
    }
}

@Composable
private fun TranslatedString.Translations?.resolveTranslated(): String? {
    val configuration = LocalConfiguration.current
    return remember(this, configuration.locales) {
        this.resolveTranslated(configuration.locales)
    }
}

/**
 * Non-Compose core resolver for TranslatedString.Translations using the provided LocaleList.
 */
fun TranslatedString.Translations?.resolveTranslated(locales: LocaleList): String? {
    if (this == null) return null
    if (values.isEmpty()) return ""

    // Normalize keys to BCP-47 tags for matching
    val supported = values.keys
        .map { Locale.forLanguageTag(it).toLanguageTag() }
        .toTypedArray()

    // Ask Android for the best match given user's locale prefs
    val best: Locale? = locales.getFirstMatch(supported)

    // Exact match?
    best?.toLanguageTag()?.let { tag ->
        values[tag]?.let { return it }
    }

    // Language-only fallback
    best?.language?.let { lang ->
        values[lang]?.let { return it }
    }

    // Last-ditch fallback
    return values.values.firstOrNull()
}
