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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.lua.dto.LuaUiContent
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaUiRenderResult
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.ui.theming.TngColors
import com.samco.trackandgraph.ui.theming.TngTypography
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.theming.tngTypography
import com.samco.trackandgraph.ui.ui.resolve
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val LocalLuaUiErrorHandler = staticCompositionLocalOf<(Throwable) -> Unit> {
    { throw it }
}

private val LocalLuaUiActionHandler = staticCompositionLocalOf<(LuaValueReference) -> Unit> {
    { error("No Lua UI action handler installed") }
}

private val canvasSpBreakpoints = listOf(8f, 10f, 12f, 14f, 18f, 20f, 24f, 30f, 100f)

@Composable
internal fun LuaUiHost(
    modifier: Modifier,
    content: LuaUiContent,
    errorContent: @Composable (Throwable) -> Unit,
    onStateChanged: (String?) -> Unit = {},
) {
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
        var callbackError by remember(content, context) { mutableStateOf<Throwable?>(null) }
        var renderResult by remember(content, context) {
            mutableStateOf<Result<LuaUiRenderResult>?>(null)
        }
        fun publish(result: Result<LuaUiRenderResult>) {
            renderResult = result
            result.getOrNull()?.let { onStateChanged(it.serializedState) }
        }
        LaunchedEffect(content, context) {
            publish(captureLuaUiResult { content.render(context) })
        }
        val coroutineScope = rememberCoroutineScope()
        val actionHandler: (LuaValueReference) -> Unit = { function ->
            coroutineScope.launch {
                publish(captureLuaUiResult { content.interact(context, function) })
            }
        }
        val error = callbackError ?: renderResult?.exceptionOrNull()
        if (error != null) {
            errorContent(error)
        } else {
            renderResult?.getOrNull()?.ui?.let { tree ->
                CompositionLocalProvider(
                    LocalLuaUiErrorHandler provides { callbackError = it },
                    LocalLuaUiActionHandler provides actionHandler,
                ) {
                    LuaNode(tree, content)
                }
            }
        }
    }
}

@Composable
private fun LuaNode(node: LuaValueAccessor, content: LuaUiContent, parentModifier: Modifier = Modifier) {
    key(node["key"]?.asString() ?: node) {
        val modifier = LuaUiModifierParser.parse(
            parentModifier,
            node["modifier"],
            LocalLuaUiActionHandler.current,
        )
        when (node["type"]?.asString()) {
            "ROW" -> Row(
                modifier = modifier,
                horizontalArrangement = horizontalArrangement(node),
                verticalAlignment = verticalAlignment(node["alignment"]?.asString()),
            ) { LuaRowChildren(node["children"], node["alignment"]?.asString(), content) }
            "FLOW_ROW" -> FlowRow(
                modifier = modifier,
                horizontalArrangement = flowHorizontalArrangement(node),
                verticalArrangement = flowVerticalArrangement(node),
                itemVerticalAlignment = verticalAlignment(node["item_vertical_alignment"]?.asString()),
                maxItemsInEachRow = node["max_items_in_each_row"]?.asDouble()?.toInt() ?: Int.MAX_VALUE,
                maxLines = node["max_lines"]?.asDouble()?.toInt() ?: Int.MAX_VALUE,
            ) { LuaFlowRowChildren(node["children"], content) }
            "COLUMN" -> Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement(node),
                horizontalAlignment = horizontalAlignment(node["alignment"]?.asString()),
            ) { LuaColumnChildren(node["children"], node["alignment"]?.asString(), content) }
            "BOX" -> Box(modifier, contentAlignment = boxAlignment(node["alignment"]?.asString())) {
                node["children"].toList().forEach { child ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.LuaFlowRowChildren(
    children: LuaValueAccessor?,
    content: LuaUiContent,
) {
    children.toList().forEach {
        LuaNode(it, content, LuaUiModifierParser.flowRowChild(this, it))
    }
}

@Composable
private fun RowScope.LuaRowChildren(
    children: LuaValueAccessor?,
    alignment: String?,
    content: LuaUiContent,
) {
    children.toList().forEach {
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
    children.toList().forEach {
        var parentModifier = LuaUiModifierParser.columnChild(this, it)
        if (alignment == "STRETCH") parentModifier = parentModifier.fillMaxWidth()
        LuaNode(it, content, parentModifier)
    }
}

@Composable
private fun LuaText(node: LuaValueAccessor, modifier: Modifier) {
    Text(
        text = luaText(node["text"]),
        modifier = modifier,
        color = luaColor(node["color"]) ?: Color.Unspecified,
        style = luaTextStyle(
            node["style"]?.asString(),
            MaterialTheme.tngTypography,
            LocalTextStyle.current,
        ),
        fontSize = textDimension(node["font_size"]),
        fontWeight = when (node["font_weight"]?.asString()) {
            "NORMAL" -> FontWeight.Normal
            "MEDIUM" -> FontWeight.Medium
            "BOLD" -> FontWeight.Bold
            else -> null
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
private fun luaText(value: LuaValueAccessor?): String =
    value?.asString()
        ?: value?.asStringMap()?.let { TranslatedString.Translations(it).resolve() }
        .orEmpty()

internal fun luaTextStyle(
    token: String?,
    typography: TngTypography,
    fallback: TextStyle,
): TextStyle = when (token) {
    "TEXT_STYLE_DISPLAY_LARGE" -> typography.materialTypography.displayLarge
    "TEXT_STYLE_DISPLAY_MEDIUM" -> typography.materialTypography.displayMedium
    "TEXT_STYLE_DISPLAY_SMALL" -> typography.materialTypography.displaySmall
    "TEXT_STYLE_HEADLINE_LARGE" -> typography.materialTypography.headlineLarge
    "TEXT_STYLE_HEADLINE_MEDIUM" -> typography.materialTypography.headlineMedium
    "TEXT_STYLE_HEADLINE_SMALL" -> typography.materialTypography.headlineSmall
    "TEXT_STYLE_TITLE_LARGE" -> typography.materialTypography.titleLarge
    "TEXT_STYLE_TITLE_MEDIUM" -> typography.materialTypography.titleMedium
    "TEXT_STYLE_TITLE_SMALL" -> typography.materialTypography.titleSmall
    "TEXT_STYLE_BODY_LARGE" -> typography.materialTypography.bodyLarge
    "TEXT_STYLE_BODY_MEDIUM" -> typography.materialTypography.bodyMedium
    "TEXT_STYLE_BODY_SMALL" -> typography.materialTypography.bodySmall
    "TEXT_STYLE_LABEL_LARGE" -> typography.materialTypography.labelLarge
    "TEXT_STYLE_LABEL_MEDIUM" -> typography.materialTypography.labelMedium
    "TEXT_STYLE_LABEL_SMALL" -> typography.materialTypography.labelSmall
    "TEXT_STYLE_CODE" -> typography.code
    else -> fallback
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
        color = luaColor(node["color"]) ?: MaterialTheme.colorScheme.surface,
        contentColor = luaColor(node["content_color"]) ?: MaterialTheme.colorScheme.onSurface,
        shape = LuaUiModifierParser.shape(node["shape"]?.asString()),
        tonalElevation = LuaUiModifierParser.dimension(node["tonal_elevation"]) ?: 0.dp,
        shadowElevation = LuaUiModifierParser.dimension(node["shadow_elevation"]) ?: 0.dp,
    ) { node["child"]?.let { LuaNode(it, content) } }
}

@Composable
private fun LuaCanvas(node: LuaValueAccessor, content: LuaUiContent, modifier: Modifier) {
    val draw = node["draw"]?.asFunction() ?: return
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val onError = LocalLuaUiErrorHandler.current
    var size by remember(draw) { mutableStateOf(IntSize.Zero) }
    val result by produceState<LuaValueAccessor?>(null, content, draw, size, density) {
        value = if (size == IntSize.Zero) null else {
            captureLuaUiResult {
                content.call(
                    draw,
                    LuaValueData.TableValue(mapOf(
                        "bounds" to LuaValueData.TableValue(mapOf(
                            "width" to LuaValueData.NumberValue(size.width.toDouble()),
                            "height" to LuaValueData.NumberValue(size.height.toDouble()),
                        )),
                        "density" to LuaValueData.NumberValue(density.density.toDouble()),
                        "font_scale" to LuaValueData.NumberValue(density.fontScale.toDouble()),
                        "sp_to_px" to LuaValueData.ArrayValue(canvasSpBreakpoints.map { sp ->
                            LuaValueData.TableValue(mapOf(
                                "sp" to LuaValueData.NumberValue(sp.toDouble()),
                                "px" to LuaValueData.NumberValue(
                                    with(density) { sp.sp.toPx().toDouble() },
                                ),
                            ))
                        }),
                    )),
                )
            }.fold(
                onSuccess = { it },
                onFailure = { onError(it); null },
            )
        }
    }
    val colors = luaThemeColors(MaterialTheme.tngColors)
    Canvas(modifier.onSizeChanged { size = it }) {
        result?.get("instructions")?.let {
            LuaCanvasInstructionParser.draw(this, it, colors, textMeasurer)
        }
    }
}

private suspend fun <T> captureLuaUiResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}

@Composable
private fun horizontalArrangement(node: LuaValueAccessor): Arrangement.Horizontal {
    LuaUiModifierParser.dimension(node["spacing"])?.let {
        if (it > 0.dp) return Arrangement.spacedBy(it)
    }
    return horizontalArrangement(node["arrangement"]?.asString())
}

@Composable
private fun flowHorizontalArrangement(node: LuaValueAccessor): Arrangement.Horizontal {
    val arrangement = node["horizontal_arrangement"]?.asString()
    val spacing = LuaUiModifierParser.dimension(node["horizontal_spacing"])
    if (spacing != null && spacing > 0.dp && arrangement !in spaceDistributionArrangements) {
        return Arrangement.spacedBy(spacing, horizontalAlignment(arrangement))
    }
    return horizontalArrangement(arrangement)
}

@Composable
private fun flowVerticalArrangement(node: LuaValueAccessor): Arrangement.Vertical {
    val arrangement = node["vertical_arrangement"]?.asString()
    val spacing = LuaUiModifierParser.dimension(node["vertical_spacing"])
    if (spacing != null && spacing > 0.dp && arrangement !in spaceDistributionArrangements) {
        return Arrangement.spacedBy(spacing, verticalAlignment(arrangement))
    }
    return verticalArrangement(arrangement)
}

private val spaceDistributionArrangements = setOf("SPACE_BETWEEN", "SPACE_AROUND", "SPACE_EVENLY")

private fun horizontalArrangement(value: String?): Arrangement.Horizontal = when (value) {
    "CENTER" -> Arrangement.Center
    "END" -> Arrangement.End
    "SPACE_BETWEEN" -> Arrangement.SpaceBetween
    "SPACE_AROUND" -> Arrangement.SpaceAround
    "SPACE_EVENLY" -> Arrangement.SpaceEvenly
    else -> Arrangement.Start
}

@Composable
private fun verticalArrangement(node: LuaValueAccessor): Arrangement.Vertical {
    LuaUiModifierParser.dimension(node["spacing"])?.let {
        if (it > 0.dp) return Arrangement.spacedBy(it)
    }
    return verticalArrangement(node["arrangement"]?.asString())
}

private fun verticalArrangement(value: String?): Arrangement.Vertical = when (value) {
    "CENTER" -> Arrangement.Center
    "END" -> Arrangement.Bottom
    "SPACE_BETWEEN" -> Arrangement.SpaceBetween
    "SPACE_AROUND" -> Arrangement.SpaceAround
    "SPACE_EVENLY" -> Arrangement.SpaceEvenly
    else -> Arrangement.Top
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
internal fun luaColor(value: LuaValueAccessor?): Color? =
    resolveLuaColor(value, luaThemeColors(MaterialTheme.tngColors))

internal fun luaThemeColors(colors: TngColors): Map<String, Color> = colors.let {
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
