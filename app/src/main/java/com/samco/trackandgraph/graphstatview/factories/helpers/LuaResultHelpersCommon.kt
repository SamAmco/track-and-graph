package com.samco.trackandgraph.graphstatview.factories.helpers

import androidx.annotation.ColorInt
import com.samco.trackandgraph.data.lua.dto.ColorSpec
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec as ViewColorSpec

internal fun ColorSpec.toColorSpec(): ViewColorSpec = when (this) {
    is ColorSpec.ColorIndex -> ViewColorSpec.ColorIndex(index)
    is ColorSpec.HexColor -> ViewColorSpec.ColorValue(parseHexColor(hexString))
}

internal fun indexColorSpec(index: Int): ColorSpec =
    ColorSpec.ColorIndex((index * dataVisColorGenerator) % dataVisColorList.size)

@ColorInt
internal fun parseHexColor(hex: String): Int {
    val cleanHex = hex.removePrefix("#")
    val color = cleanHex.toLong(16)
    return when (cleanHex.length) {
        6 -> (0xFF000000 or color).toInt() // Add alpha if missing
        8 -> color.toInt()
        else -> throw IllegalArgumentException("Invalid hex color: $hex")
    }
}
