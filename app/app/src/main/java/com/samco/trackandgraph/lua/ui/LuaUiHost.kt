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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.lua.dto.LuaUiContent
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.ui.theming.tngColors

@Composable
internal fun LuaUiHost(modifier: Modifier, content: LuaUiContent) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val darkTheme = MaterialTheme.tngColors.isDark
    BoxWithConstraints(modifier) {
        val context = LuaUiContext(
            constraints = LuaUiContext.Constraints(
                constraints.minWidth, constraints.maxWidth,
                constraints.minHeight, constraints.maxHeight,
            ),
            density = density.density,
            fontScale = density.fontScale,
            layoutDirection = if (direction == LayoutDirection.Ltr) {
                LuaUiContext.LayoutDirection.LTR
            } else LuaUiContext.LayoutDirection.RTL,
            theme = if (darkTheme) LuaUiContext.Theme.DARK else LuaUiContext.Theme.LIGHT,
        )
        val tree by produceState<LuaValueAccessor?>(null, content, context) {
            value = runCatching { content.render(context) }.getOrNull()
        }
        tree?.let { LuaNode(it, content) }
    }
}

@Composable
private fun LuaNode(node: LuaValueAccessor, content: LuaUiContent, parentModifier: Modifier = Modifier) {
    key(node["key"]?.asString() ?: node) {
        val modifier = LuaUiModifierParser.parse(parentModifier, node["modifier"])
        when (node["type"]?.asString()) {
            "ROW" -> Row(
                modifier = modifier,
                horizontalArrangement = horizontalArrangement(node),
                verticalAlignment = verticalAlignment(node["alignment"]?.asString()),
            ) { LuaRowChildren(node["children"], node["alignment"]?.asString(), content) }
            "COLUMN" -> Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement(node),
                horizontalAlignment = horizontalAlignment(node["alignment"]?.asString()),
            ) { LuaColumnChildren(node["children"], node["alignment"]?.asString(), content) }
            "BOX" -> Box(modifier, contentAlignment = boxAlignment(node["alignment"]?.asString())) {
                luaChildren(node["children"]).forEach { child ->
                    LuaNode(child, content, LuaUiModifierParser.boxChild(this, child))
                }
            }
            "SPACER" -> Spacer(modifier)
            "TEXT" -> LuaText(node, modifier)
            "SURFACE" -> LuaSurface(node, content, modifier)
            "CANVAS" -> LuaCanvas(node, content, modifier)
        }
    }
}

@Composable
private fun RowScope.LuaRowChildren(
    children: LuaValueAccessor?,
    alignment: String?,
    content: LuaUiContent,
) {
    luaChildren(children).forEach {
        var parentModifier = LuaUiModifierParser.rowChild(this, it)
        if (alignment == "STRETCH") parentModifier = parentModifier.fillMaxHeight()
        LuaNode(it, content, parentModifier)
    }
}

@Composable
private fun ColumnScope.LuaColumnChildren(
    children: LuaValueAccessor?,
    alignment: String?,
    content: LuaUiContent,
) {
    luaChildren(children).forEach {
        var parentModifier = LuaUiModifierParser.columnChild(this, it)
        if (alignment == "STRETCH") parentModifier = parentModifier.fillMaxWidth()
        LuaNode(it, content, parentModifier)
    }
}

@Composable
private fun LuaText(node: LuaValueAccessor, modifier: Modifier) {
    Text(
        text = node["text"]?.asString().orEmpty(),
        modifier = modifier,
        color = luaColor(node["color"]?.asString()) ?: Color.Unspecified,
        fontSize = textDimension(node["font_size"]),
        fontWeight = when (node["font_weight"]?.asString()) {
            "MEDIUM" -> FontWeight.Medium
            "BOLD" -> FontWeight.Bold
            else -> FontWeight.Normal
        },
        textAlign = when (node["text_align"]?.asString()) {
            "CENTER" -> TextAlign.Center
            "END" -> TextAlign.End
            else -> TextAlign.Start
        },
        maxLines = node["max_lines"]?.asDouble()?.toInt() ?: Int.MAX_VALUE,
        overflow = if (node["overflow"]?.asString() == "ELLIPSIS") {
            TextOverflow.Ellipsis
        } else TextOverflow.Clip,
    )
}

@Composable
private fun textDimension(value: LuaValueAccessor?): TextUnit {
    val number = value?.get("value")?.asDouble()?.toFloat() ?: return TextUnit.Unspecified
    return when (value["unit"]?.asString()) {
        "PX" -> with(LocalDensity.current) { number.toDp().toSp() }
        "DP" -> with(LocalDensity.current) { number.dp.toSp() }
        else -> number.sp
    }
}

@Composable
private fun LuaSurface(node: LuaValueAccessor, content: LuaUiContent, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = luaColor(node["color"]?.asString()) ?: MaterialTheme.colorScheme.surface,
        contentColor = luaColor(node["content_color"]?.asString()) ?: MaterialTheme.colorScheme.onSurface,
        shape = LuaUiModifierParser.shape(node["shape"]?.asString()),
        tonalElevation = LuaUiModifierParser.dimension(node["tonal_elevation"]) ?: 0.dp,
        shadowElevation = LuaUiModifierParser.dimension(node["shadow_elevation"]) ?: 0.dp,
    ) { node["child"]?.let { LuaNode(it, content) } }
}

@Composable
private fun LuaCanvas(node: LuaValueAccessor, content: LuaUiContent, modifier: Modifier) {
    val draw = node["draw"]?.asFunction() ?: return
    var size by remember(draw) { mutableStateOf(IntSize.Zero) }
    val result by produceState<LuaValueAccessor?>(null, content, draw, size) {
        value = if (size == IntSize.Zero) null else runCatching {
            content.call(
                draw,
                LuaValueData.TableValue(mapOf(
                    "bounds" to LuaValueData.TableValue(mapOf(
                        "width" to LuaValueData.NumberValue(size.width.toDouble()),
                        "height" to LuaValueData.NumberValue(size.height.toDouble()),
                    )),
                )),
            )
        }.getOrNull()
    }
    val colors = luaThemeColors()
    Canvas(modifier.onSizeChanged { size = it }) {
        result?.get("instructions")?.let { LuaCanvasInstructionParser.draw(this, it, colors) }
    }
}

internal fun luaChildren(value: LuaValueAccessor?): List<LuaValueAccessor> =
    (1..(value?.arraySize ?: 0)).mapNotNull { value?.get(it) }

@Composable
private fun horizontalArrangement(node: LuaValueAccessor): Arrangement.Horizontal {
    LuaUiModifierParser.dimension(node["spacing"])?.let {
        if (it > 0.dp) return Arrangement.spacedBy(it)
    }
    return when (node["arrangement"]?.asString()) {
        "CENTER" -> Arrangement.Center
        "END" -> Arrangement.End
        "SPACE_BETWEEN" -> Arrangement.SpaceBetween
        "SPACE_AROUND" -> Arrangement.SpaceAround
        "SPACE_EVENLY" -> Arrangement.SpaceEvenly
        else -> Arrangement.Start
    }
}

@Composable
private fun verticalArrangement(node: LuaValueAccessor): Arrangement.Vertical {
    LuaUiModifierParser.dimension(node["spacing"])?.let {
        if (it > 0.dp) return Arrangement.spacedBy(it)
    }
    return when (node["arrangement"]?.asString()) {
        "CENTER" -> Arrangement.Center
        "END" -> Arrangement.Bottom
        "SPACE_BETWEEN" -> Arrangement.SpaceBetween
        "SPACE_AROUND" -> Arrangement.SpaceAround
        "SPACE_EVENLY" -> Arrangement.SpaceEvenly
        else -> Arrangement.Top
    }
}

private fun verticalAlignment(value: String?) = when (value) {
    "CENTER" -> Alignment.CenterVertically
    "END" -> Alignment.Bottom
    else -> Alignment.Top
}

private fun horizontalAlignment(value: String?) = when (value) {
    "CENTER" -> Alignment.CenterHorizontally
    "END" -> Alignment.End
    else -> Alignment.Start
}

internal fun boxAlignment(value: String?) = when (value) {
    "TOP_CENTER" -> Alignment.TopCenter
    "TOP_END" -> Alignment.TopEnd
    "CENTER_START" -> Alignment.CenterStart
    "CENTER" -> Alignment.Center
    "CENTER_END" -> Alignment.CenterEnd
    "BOTTOM_START" -> Alignment.BottomStart
    "BOTTOM_CENTER" -> Alignment.BottomCenter
    "BOTTOM_END" -> Alignment.BottomEnd
    else -> Alignment.TopStart
}

@Composable
internal fun luaColor(value: String?): Color? = value?.let { luaThemeColors()[it] ?: parseColor(it) }

@Composable
private fun luaThemeColors(): Map<String, Color> = MaterialTheme.tngColors.let {
    mapOf(
        "THEME_PRIMARY" to it.primary, "THEME_ON_PRIMARY" to it.onPrimary,
        "THEME_SECONDARY" to it.secondary, "THEME_ON_SECONDARY" to it.onSecondary,
        "THEME_SURFACE" to it.surface, "THEME_ON_SURFACE" to it.onSurface,
        "THEME_SURFACE_VARIANT" to it.surfaceVariant, "THEME_ON_SURFACE_VARIANT" to it.onSurfaceVariant,
        "THEME_OUTLINE" to it.outline, "THEME_ERROR" to it.error, "THEME_ON_ERROR" to it.onError,
        "THEME_SELECTOR_BUTTON" to it.selectorButtonColor, "THEME_SECONDARY_TEXT" to it.textColorSecondary,
        "THEME_TOOLBAR_BACKGROUND" to it.toolbarBackgroundColor, "THEME_HYPERLINK" to it.hyperlinkColor,
        "THEME_GOOD" to it.goodColor,
    )
}

private fun parseColor(value: String): Color? = runCatching {
    val hex = value.removePrefix("#")
    Color((if (hex.length == 6) "FF$hex" else hex).toLong(16))
}.getOrNull()
