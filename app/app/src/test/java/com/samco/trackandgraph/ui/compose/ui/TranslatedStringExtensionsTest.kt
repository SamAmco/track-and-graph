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

import com.samco.trackandgraph.data.localisation.TranslatedString
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslatedStringExtensionsTest {

    private val translations = TranslatedString.Translations(
        linkedMapOf(
            "fr" to "Français",
            "en" to "English",
            "zh-Hans" to "简体中文",
            "zh-Hant" to "繁體中文"
        )
    )

    @Test
    fun `resolves an exact locale`() {
        assertEquals("Français", translations.resolveTranslated(listOf(locale("fr")), ::matches))
    }

    @Test
    fun `resolves a regional locale to its supported language`() {
        assertEquals("Français", translations.resolveTranslated(listOf(locale("fr-CA")), ::matches))
    }

    @Test
    fun `resolves Chinese regions to the matching script`() {
        assertEquals("简体中文", translations.resolveTranslated(listOf(locale("zh-CN")), ::matches))
        assertEquals("繁體中文", translations.resolveTranslated(listOf(locale("zh-TW")), ::matches))
        assertEquals("繁體中文", translations.resolveTranslated(listOf(locale("zh-HK")), ::matches))
    }

    @Test
    fun `uses the first supported preferred locale`() {
        assertEquals(
            "Français",
            translations.resolveTranslated(listOf(locale("ar"), locale("fr-CA")), ::matches)
        )
    }

    @Test
    fun `falls back explicitly to English`() {
        assertEquals("English", translations.resolveTranslated(listOf(locale("ar")), ::matches))
    }

    private fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    private fun matches(supported: Locale, preferred: Locale): Boolean {
        if (supported.language != preferred.language) return false
        if (supported.language != "zh") return true
        return chineseScript(supported) == chineseScript(preferred)
    }

    private fun chineseScript(locale: Locale): String = when {
        locale.script.isNotEmpty() -> locale.script
        locale.country in setOf("TW", "HK", "MO") -> "Hant"
        else -> "Hans"
    }
}
