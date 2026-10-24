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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

internal object LuaUiModifierParser {
    @Composable
    fun parse(
        initial: Modifier,
        modifiers: LuaValueHandle?,
        onClick: (LuaValueHandle) -> Unit,
    ): Modifier {
        var result = initial
        modifiers.toHandleList().forEach { item ->
            result = when (item["type"]?.asString()) {
                "WIDTH" -> dimension(item["value"])?.let(result::width) ?: result
                "HEIGHT" -> dimension(item["value"])?.let(result::height) ?: result
                "FILL_WIDTH" -> result.fillMaxWidth(item["fraction"]?.asDouble()?.toFloat() ?: 1f)
                "FILL_HEIGHT" -> result.fillMaxHeight(item["fraction"]?.asDouble()?.toFloat() ?: 1f)
                "WRAP_WIDTH" -> result.wrapContentWidth()
                "WRAP_HEIGHT" -> result.wrapContentHeight()
                "MIN_WIDTH" -> result.widthIn(min = dimension(item["value"]) ?: Dp.Unspecified)
                "MAX_WIDTH" -> result.widthIn(max = dimension(item["value"]) ?: Dp.Unspecified)
                "MIN_HEIGHT" -> result.heightIn(min = dimension(item["value"]) ?: Dp.Unspecified)
                "MAX_HEIGHT" -> result.heightIn(max = dimension(item["value"]) ?: Dp.Unspecified)
                "ASPECT_RATIO" -> result.aspectRatio(item["value"]?.asDouble()?.toFloat() ?: 1f)
                "PADDING" -> padding(result, item)
                "BACKGROUND" -> luaColor(item["color"])?.let {
                    result.background(it, shape(item["shape"]?.asString()))
                } ?: result
                "BORDER" -> luaColor(item["color"])?.let {
                    result.border(dimension(item["width"]) ?: 0.dp, it, shape(item["shape"]?.asString()))
                } ?: result
                "CLIP" -> result.clip(shape(item["shape"]?.asString()))
                "ELEVATION" -> result.shadow(dimension(item["value"]) ?: 0.dp)
                "ON_CLICK" -> item["callback"]?.asFunction()?.let { callback ->
                    result.clickable { onClick(callback) }
                } ?: result
                else -> result
            }
        }
        return result
    }

    fun rowChild(scope: RowScope, node: LuaValueAccessor): Modifier = with(scope) {
        var result: Modifier = Modifier
        entry(node, "WEIGHT")?.let {
            result = result.weight(it["value"]?.asDouble()?.toFloat() ?: 1f, it["fill"]?.asBoolean() ?: true)
        }
        when (entry(node, "ALIGN")?.get("value")?.asString()) {
            "START" -> result.align(Alignment.Top)
            "CENTER" -> result.align(Alignment.CenterVertically)
            "END" -> result.align(Alignment.Bottom)
            "STRETCH" -> result.fillMaxHeight()
            else -> result
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    fun flowRowChild(scope: FlowRowScope, node: LuaValueAccessor): Modifier = with(scope) {
        var result: Modifier = Modifier
        entry(node, "WEIGHT")?.let {
            result = result.weight(it["value"]?.asDouble()?.toFloat() ?: 1f, it["fill"]?.asBoolean() ?: true)
        }
        when (entry(node, "ALIGN")?.get("value")?.asString()) {
            "START" -> result = result.align(Alignment.Top)
            "CENTER" -> result = result.align(Alignment.CenterVertically)
            "END" -> result = result.align(Alignment.Bottom)
            "STRETCH" -> result = result.fillMaxRowHeight()
        }
        entry(node, "FILL_MAX_ROW_HEIGHT")?.let {
            result = result.fillMaxRowHeight(it["fraction"]?.asDouble()?.toFloat() ?: 1f)
        }
        result
    }

    fun columnChild(scope: ColumnScope, node: LuaValueAccessor): Modifier = with(scope) {
        var result: Modifier = Modifier
        entry(node, "WEIGHT")?.let {
            result = result.weight(it["value"]?.asDouble()?.toFloat() ?: 1f, it["fill"]?.asBoolean() ?: true)
        }
        when (entry(node, "ALIGN")?.get("value")?.asString()) {
            "START" -> result.align(Alignment.Start)
            "CENTER" -> result.align(Alignment.CenterHorizontally)
            "END" -> result.align(Alignment.End)
            "STRETCH" -> result.fillMaxWidth()
            else -> result
        }
    }

    fun boxChild(scope: BoxScope, node: LuaValueAccessor): Modifier = with(scope) {
        entry(node, "ALIGN")?.get("value")?.asString()?.let { Modifier.align(boxAlignment(it)) } ?: Modifier
    }

    @Composable
    fun dimension(value: LuaValueAccessor?): Dp? {
        value?.asString()?.let { return spacing(it) }
        val number = value?.get("value")?.asDouble() ?: return null
        return when (value["unit"]?.asString()) {
            "PX" -> with(LocalDensity.current) { number.toFloat().toDp() }
            "SP" -> with(LocalDensity.current) { number.sp.toDp() }
            else -> number.dp
        }
    }

    @Composable
    fun shape(token: String?): Shape = when (token) {
        "SHAPE_SMALL" -> RoundedCornerShape(4.dp)
        "SHAPE_LARGE" -> RoundedCornerShape(16.dp)
        "SHAPE_CIRCLE" -> CircleShape
        else -> RoundedCornerShape(8.dp)
    }

    @Composable
    private fun padding(modifier: Modifier, item: LuaValueAccessor): Modifier {
        dimension(item["all"])?.let { return modifier.padding(it) }
        val horizontal = dimension(item["horizontal"]) ?: 0.dp
        val vertical = dimension(item["vertical"]) ?: 0.dp
        return modifier.padding(
            start = dimension(item["start"]) ?: horizontal,
            top = dimension(item["top"]) ?: vertical,
            end = dimension(item["end"]) ?: horizontal,
            bottom = dimension(item["bottom"]) ?: vertical,
        )
    }

    private fun entry(node: LuaValueAccessor, type: String): LuaValueAccessor? =
        node["modifier"].toList().firstOrNull { it["type"]?.asString() == type }

    private fun spacing(token: String?): Dp? = when (token) {
        "SPACING_X_SMALL" -> 4.dp
        "SPACING_SMALL" -> 5.dp
        "SPACING_MEDIUM" -> 10.dp
        "SPACING_LARGE" -> 20.dp
        "SPACING_X_LARGE" -> 50.dp
        else -> null
    }
}
