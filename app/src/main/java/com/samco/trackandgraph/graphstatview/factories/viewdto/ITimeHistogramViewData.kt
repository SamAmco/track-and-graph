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

package com.samco.trackandgraph.graphstatview.factories.viewdto

import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow

interface ITimeHistogramViewData : IGraphStatViewData {
    data class BarValue(val label: String, val values: List<Double>)

    val window: TimeHistogramWindowData
        get() = TimeHistogramWindowData.getWindowData(TimeHistogramWindow.WEEK)

    val barValues: List<BarValue>?
        get() = null

    val maxDisplayHeight: Double
        get() = 0.0
}