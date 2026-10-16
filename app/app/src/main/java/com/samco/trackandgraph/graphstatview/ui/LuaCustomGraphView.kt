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
package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaCustomGraphViewData

/**
 * Planned flow:
 * 1. Obtain the runtime UI context, including the root layout constraints.
 * 2. Call renderer.render(viewData.renderData, context) outside Compose drawing callbacks.
 * 3. Retain the returned custom UI description as Compose state.
 * 4. Render its component tree; each canvas receives its own measured size and delegates to
 *    LuaCanvasInstructionParser.
 */
@Composable
internal fun LuaCustomGraphView(
    modifier: Modifier,
    viewData: ILuaCustomGraphViewData,
) {
    TODO("Compose context, render callback, state, and custom component wiring")
}
