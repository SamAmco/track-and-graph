/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.ui.theming.tngColors

private val canvasSpBreakpoints = listOf(8f, 10f, 12f, 14f, 18f, 20f, 24f, 30f, 100f)

@Composable
internal fun LuaCanvas(
    node: LuaValueHandle,
    content: LuaUiContent,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    val draw = node["draw"]?.asFunction() ?: return
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var size by remember(draw) { mutableStateOf(IntSize.Zero) }
    val result by produceState<LuaValueHandle?>(null, content, draw, size, density) {
        value = if (size == IntSize.Zero) null else {
            captureLuaUiResult {
                content.call(
                    draw,
                    LuaValueSnapshot.TableValue(mapOf(
                        "bounds" to LuaValueSnapshot.TableValue(mapOf(
                            "width" to LuaValueSnapshot.NumberValue(size.width.toDouble()),
                            "height" to LuaValueSnapshot.NumberValue(size.height.toDouble()),
                        )),
                        "density" to LuaValueSnapshot.NumberValue(density.density.toDouble()),
                        "font_scale" to LuaValueSnapshot.NumberValue(density.fontScale.toDouble()),
                        "sp_to_px" to LuaValueSnapshot.ArrayValue(canvasSpBreakpoints.map { sp ->
                            LuaValueSnapshot.TableValue(mapOf(
                                "sp" to LuaValueSnapshot.NumberValue(sp.toDouble()),
                                "px" to LuaValueSnapshot.NumberValue(
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
