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
package com.samco.trackandgraph.localisation

import androidx.core.os.LocaleListCompat
import java.util.Locale

internal data class LocalizedValue<T>(
    val localeTag: String,
    val value: T
)

internal fun <T> Map<String, T>.selectLocalizedValue(
    preferredLocales: List<Locale>
): LocalizedValue<T>? = selectLocalizedValue(
    preferredLocales,
    LocaleListCompat::matchesLanguageAndScript
)

internal fun <T> Map<String, T>.selectLocalizedValue(
    preferredLocales: List<Locale>,
    matchesLanguageAndScript: (supported: Locale, preferred: Locale) -> Boolean
): LocalizedValue<T>? {
    val supported = mapNotNull { (tag, value) ->
        val locale = Locale.forLanguageTag(tag)
        locale.takeIf { it.language.isNotEmpty() }
            ?.let { SupportedValue(tag, it, value) }
    }

    for (preferred in preferredLocales) {
        supported.firstOrNull { it.locale == preferred }
            ?.let { return LocalizedValue(it.tag, it.value) }
        supported.firstOrNull { matchesLanguageAndScript(it.locale, preferred) }
            ?.let { return LocalizedValue(it.tag, it.value) }
    }

    return this["en"]?.let { LocalizedValue("en", it) }
}

private data class SupportedValue<T>(
    val tag: String,
    val locale: Locale,
    val value: T
)
