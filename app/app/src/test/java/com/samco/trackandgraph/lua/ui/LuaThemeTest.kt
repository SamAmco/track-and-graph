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
package com.samco.trackandgraph.lua.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.theming.TngTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LuaThemeTest {
    @Test
    fun `resolves every text style token`() {
        val material = Typography()
        val code = TextStyle(fontSize = 99.sp)
        val typography = TngTypography(material, code)
        val fallback = TextStyle(fontSize = 101.sp)
        val expected = mapOf(
            "TEXT_STYLE_DISPLAY_LARGE" to material.displayLarge,
            "TEXT_STYLE_DISPLAY_MEDIUM" to material.displayMedium,
            "TEXT_STYLE_DISPLAY_SMALL" to material.displaySmall,
            "TEXT_STYLE_HEADLINE_LARGE" to material.headlineLarge,
            "TEXT_STYLE_HEADLINE_MEDIUM" to material.headlineMedium,
            "TEXT_STYLE_HEADLINE_SMALL" to material.headlineSmall,
            "TEXT_STYLE_TITLE_LARGE" to material.titleLarge,
            "TEXT_STYLE_TITLE_MEDIUM" to material.titleMedium,
            "TEXT_STYLE_TITLE_SMALL" to material.titleSmall,
            "TEXT_STYLE_BODY_LARGE" to material.bodyLarge,
            "TEXT_STYLE_BODY_MEDIUM" to material.bodyMedium,
            "TEXT_STYLE_BODY_SMALL" to material.bodySmall,
            "TEXT_STYLE_LABEL_LARGE" to material.labelLarge,
            "TEXT_STYLE_LABEL_MEDIUM" to material.labelMedium,
            "TEXT_STYLE_LABEL_SMALL" to material.labelSmall,
            "TEXT_STYLE_CODE" to code,
        )

        expected.forEach { (token, style) ->
            assertEquals(token, style, luaTextStyle(token, typography, fallback))
        }
        assertEquals(fallback, luaTextStyle(null, typography, fallback))
        assertEquals(fallback, luaTextStyle("UNKNOWN", typography, fallback))
    }

    @Test
    fun `resolves core color indices and string colors`() {
        val namedColor = Color.Magenta

        assertEquals(dataVisColorList[7], resolveLuaColor(number(7.0), emptyMap()))
        assertEquals(namedColor, resolveLuaColor(string("THEME_PRIMARY"), mapOf("THEME_PRIMARY" to namedColor)))
        assertEquals(Color(0xFF112233), resolveLuaColor(string("#112233"), emptyMap()))
        assertNull(resolveLuaColor(number(7.5), emptyMap()))
        assertNull(resolveLuaColor(number(-1.0), emptyMap()))
        assertNull(resolveLuaColor(number(dataVisColorList.size.toDouble()), emptyMap()))
    }

    private fun number(value: Double): LuaValueAccessor = mock<LuaValueAccessor>().also {
        whenever(it.asDouble()).thenReturn(value)
    }

    private fun string(value: String): LuaValueAccessor = mock<LuaValueAccessor>().also {
        whenever(it.asDouble()).thenReturn(null)
        whenever(it.asString()).thenReturn(value)
    }
}
