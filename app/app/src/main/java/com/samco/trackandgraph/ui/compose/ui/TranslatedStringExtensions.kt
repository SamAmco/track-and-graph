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
package com.samco.trackandgraph.ui.ui

import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.samco.trackandgraph.data.localisation.TranslatedString
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
    val preferredLocales = List(locales.size()) { locales[it] }
    return resolveTranslated(preferredLocales, LocaleListCompat::matchesLanguageAndScript)
}

/**
 * Selects the value belonging to the supported locale which matches the user's preferences.
 * The matching locale may have a different but equivalent tag, such as zh-CN and zh-Hans.
 */
internal fun TranslatedString.Translations.resolveTranslated(
    preferredLocales: List<Locale>,
    matchesLanguageAndScript: (supported: Locale, preferred: Locale) -> Boolean
): String? {
    if (values.isEmpty()) return ""

    val supported = values.mapNotNull { (tag, value) ->
        val locale = Locale.forLanguageTag(tag)
        locale.takeIf { it.language.isNotEmpty() }?.let { SupportedTranslation(locale, value) }
    }

    for (preferred in preferredLocales) {
        supported.firstOrNull { it.locale == preferred }?.let { return it.value }
        supported.firstOrNull { matchesLanguageAndScript(it.locale, preferred) }
            ?.let { return it.value }
    }

    return values["en"] ?: supported.firstOrNull()?.value
}

private data class SupportedTranslation(
    val locale: Locale,
    val value: String
)
