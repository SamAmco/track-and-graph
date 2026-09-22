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

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocaleSelectionTest {

    private val translations = linkedMapOf(
        "fr" to "Français",
        "en" to "English",
        "zh-Hans" to "简体中文",
        "zh-Hant" to "繁體中文"
    )

    @Test
    fun `selects an exact locale`() {
        val selected = translations.selectLocalizedValue(listOf(locale("fr")), ::matches)

        assertEquals("fr", selected?.localeTag)
        assertEquals("Français", selected?.value)
    }

    @Test
    fun `selects a regional locale's supported language`() {
        assertEquals(
            "Français",
            translations.selectLocalizedValue(listOf(locale("fr-CA")), ::matches)?.value
        )
    }

    @Test
    fun `selects the Chinese translation with the matching script`() {
        assertEquals(
            "zh-Hans",
            translations.selectLocalizedValue(listOf(locale("zh-CN")), ::matches)?.localeTag
        )
        assertEquals(
            "zh-Hant",
            translations.selectLocalizedValue(listOf(locale("zh-TW")), ::matches)?.localeTag
        )
        assertEquals(
            "zh-Hant",
            translations.selectLocalizedValue(listOf(locale("zh-HK")), ::matches)?.localeTag
        )
    }

    @Test
    fun `uses the first supported preferred locale`() {
        assertEquals(
            "Français",
            translations.selectLocalizedValue(
                listOf(locale("ar"), locale("fr-CA")),
                ::matches
            )?.value
        )
    }

    @Test
    fun `falls back explicitly to English for an unsupported locale`() {
        assertEquals(
            "English",
            translations.selectLocalizedValue(listOf(locale("ar")), ::matches)?.value
        )
    }

    @Test
    fun `falls back to English when there are no preferred locales`() {
        assertEquals(
            "English",
            translations.selectLocalizedValue(emptyList(), ::matches)?.value
        )
    }

    @Test
    fun `returns null when neither a match nor English exists`() {
        assertNull(
            mapOf("fr" to "Français")
                .selectLocalizedValue(listOf(locale("ar")), ::matches)
        )
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
