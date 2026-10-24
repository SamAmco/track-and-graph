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

import androidx.compose.ui.graphics.Color
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.ui.dataVisColorList

/** Resolves theme tokens, hex strings, and the zero-based indices exposed by `tng.core.COLOR`. */
internal fun resolveLuaColor(
    value: LuaValueAccessor?,
    namedColors: Map<String, Color>,
): Color? {
    if (value == null) return null
    value.asDouble()?.let { number ->
        val index = number.toInt()
        return if (number == index.toDouble()) {
            dataVisColorList.getOrNull(index) ?: Color.Magenta
        } else Color.Magenta
    }
    val string = value.asString() ?: return Color.Magenta
    namedColors[string]?.let { return it }
    val hex = string.removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return Color.Magenta
    val encoded = (if (hex.length == 6) "FF$hex" else hex).toLongOrNull(16)
    return encoded?.let(::Color) ?: Color.Magenta
}
