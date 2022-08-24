/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.decorators

import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import org.threeten.bp.OffsetDateTime

interface IGraphStatViewDecorator {
    fun setTimeMarker(time: OffsetDateTime)

    /**
     * This will be called once per second after the initial call to decorate in case the
     * view should be updated in any way.
     */
    fun update() { }
}

abstract class GraphStatViewDecorator<T : IGraphStatViewData>(protected val listMode: Boolean) :
    IGraphStatViewDecorator {
    /**
     * Called as soon as the view and the data are ready to set up and decorate the view
     */
    abstract fun decorate(view: IDecoratableGraphStatView, data: T)
}