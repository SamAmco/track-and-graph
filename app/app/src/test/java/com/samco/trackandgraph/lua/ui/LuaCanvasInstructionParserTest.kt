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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LuaCanvasInstructionParserTest {
    @Test
    fun `draws instructions in Lua array order`() {
        val scope: DrawScope = mock()
        val instructions = accessor(listOf(
            mapOf(
                "type" to "LINE", "color" to "THEME_PRIMARY",
                "start" to mapOf("x" to 1.0, "y" to 2.0),
                "finish" to mapOf("x" to 3.0, "y" to 4.0),
                "stroke_width" to 5.0, "cap" to "round", "alpha" to 0.5,
            ),
            mapOf(
                "type" to "CIRCLE", "color" to "#FF0000",
                "center" to mapOf("x" to 6.0, "y" to 7.0), "radius" to 8.0,
            ),
        ))

        LuaCanvasInstructionParser.draw(scope, instructions, mapOf("THEME_PRIMARY" to Color.Blue))

        verify(scope).drawLine(
            eq(Color.Blue), eq(Offset(1f, 2f)), eq(Offset(3f, 4f)), eq(5f),
            eq(StrokeCap.Round), isNull(), eq(0.5f), isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawCircle(
            eq(Color.Red), eq(8f), eq(Offset(6f, 7f)), eq(1f), eq(Fill),
            isNull(), eq(BlendMode.SrcOver),
        )
    }

    private fun accessor(value: Any): LuaValueAccessor = TestAccessor(value)

    private class TestAccessor(private val value: Any) : LuaValueAccessor {
        override fun get(key: String) = (value as? Map<*, *>)?.get(key)?.let(::TestAccessor)
        override fun get(index: Int) = (value as? List<*>)?.getOrNull(index - 1)?.let(::TestAccessor)
        override val arraySize get() = (value as? List<*>)?.size
        override fun asDouble() = (value as? Number)?.toDouble()
        override fun asString() = value as? String
        override fun asBoolean() = value as? Boolean
        override fun asFunction(): LuaValueReference? = null
    }
}
