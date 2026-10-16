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
package com.samco.trackandgraph.data.lua.dto

/**
 * The root Compose environment passed to a Lua UI render function.
 *
 * A render function may return callbacks which close over this context, so environment values do
 * not need to be copied into callback-specific scopes. In particular, a canvas draw scope supplies
 * the exact measured size of that canvas, while density and layout direction remain available from
 * this context through the callback's closure.
 *
 * UI descriptions use stable theme tokens which the app resolves when rendering. [theme] exposes
 * only the current light/dark mode for code which needs to vary its behaviour between them.
 */
data class LuaUiContext(
    /** The size range, in physical pixels, available to the root Lua UI component. */
    val constraints: Constraints,

    /** Physical pixels per density-independent pixel. */
    val density: Float,

    /** The user's preferred font scaling factor. */
    val fontScale: Float,

    /** The layout direction inherited by the Lua UI tree. */
    val layoutDirection: LayoutDirection,

    /** The light/dark theme in effect for the Lua UI tree. */
    val theme: Theme,
) {
    /**
     * Root layout constraints rather than a promised final size. A child such as a canvas receives
     * its own exact measured size only after the component tree has been laid out.
     */
    data class Constraints(
        val minWidth: Int,
        val maxWidth: Int,
        val minHeight: Int,
        val maxHeight: Int,
    )

    enum class LayoutDirection {
        LTR,
        RTL,
    }

    enum class Theme {
        LIGHT,
        DARK,
    }
}
