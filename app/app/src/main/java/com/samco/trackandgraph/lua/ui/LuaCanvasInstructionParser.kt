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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor

internal object LuaCanvasInstructionParser {
    fun draw(drawScope: DrawScope, instructions: LuaValueAccessor, colors: Map<String, Color>) {
        for (index in 1..(instructions.arraySize ?: 0)) {
            val instruction = instructions[index] ?: continue
            val color = color(instruction["color"]?.asString(), colors) ?: continue
            val alpha = instruction["alpha"]?.asDouble()?.toFloat() ?: 1f
            when (instruction["type"]?.asString()) {
                "LINE" -> drawScope.drawLine(
                    color = color,
                    start = point(instruction["start"]),
                    end = point(instruction["finish"]),
                    strokeWidth = instruction["stroke_width"]?.asDouble()?.toFloat() ?: 0f,
                    cap = when (instruction["cap"]?.asString()) {
                        "round" -> StrokeCap.Round
                        "square" -> StrokeCap.Square
                        else -> StrokeCap.Butt
                    },
                    alpha = alpha,
                )
                "CIRCLE" -> drawScope.drawCircle(
                    color = color,
                    radius = instruction["radius"]?.asDouble()?.toFloat() ?: 0f,
                    center = point(instruction["center"]),
                    alpha = alpha,
                    style = if (instruction["style"]?.asString() == "stroke") {
                        Stroke(instruction["stroke_width"]?.asDouble()?.toFloat() ?: 0f)
                    } else Fill,
                )
            }
        }
    }

    private fun point(value: LuaValueAccessor?) = Offset(
        value?.get("x")?.asDouble()?.toFloat() ?: 0f,
        value?.get("y")?.asDouble()?.toFloat() ?: 0f,
    )

    private fun color(value: String?, colors: Map<String, Color>): Color? = value?.let {
        colors[it] ?: runCatching {
            val hex = it.removePrefix("#")
            Color((if (hex.length == 6) "FF$hex" else hex).toLong(16))
        }.getOrNull()
    }
}
