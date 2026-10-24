/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.ui.theming.TngTypography
import com.samco.trackandgraph.ui.theming.tngTypography
import com.samco.trackandgraph.ui.ui.resolve

@Composable
internal fun LuaText(node: LuaValueHandle, modifier: Modifier) {
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
        maxLines = node.positiveInt("max_lines"),
        overflow = if (node["overflow"]?.asString() == "ELLIPSIS") {
            TextOverflow.Ellipsis
        } else TextOverflow.Clip,
    )
}

@Composable
internal fun luaText(value: LuaValueAccessor?): String =
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
    val number = value?.get("value")?.asDouble()?.toFloat()
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: return TextUnit.Unspecified
    return when (value["unit"]?.asString()) {
        "PX" -> with(LocalDensity.current) { number.toDp().toSp() }
        "DP" -> with(LocalDensity.current) { number.dp.toSp() }
        else -> number.sp
    }
}

internal fun LuaValueAccessor.positiveInt(key: String): Int =
    this[key]?.asDouble()
        ?.takeIf { it.isFinite() && it >= 1.0 }
        ?.coerceAtMost(Int.MAX_VALUE.toDouble())
        ?.toInt()
        ?: Int.MAX_VALUE
