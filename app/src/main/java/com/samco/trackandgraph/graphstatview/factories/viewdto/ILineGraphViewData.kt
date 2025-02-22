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

import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.androidplot.xy.StepMode
import com.samco.trackandgraph.base.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.base.database.dto.YRangeType
import org.threeten.bp.OffsetDateTime

data class Line(
    val name: String,
    val color: ColorSpec,
    val pointStyle: LineGraphPointStyle,
    val line: FastXYSeries?,
)

interface ILineGraphViewData : IGraphStatViewData {
    /**
     * Whether to show the Y values as durations or not. If they are durations, the Y values
     * represent a number of seconds.
     */
    val durationBasedRange: Boolean
        get() = false

    /**
     * The type of y range to use. If it is fixed, the y axis will use the bounds. If it is dynamic,
     * the y axis may adjust the y axis to fit the data dynamically, or use the bounds still if the
     * graph is being viewed in list mode.
     */
    val yRangeType: YRangeType
        get() = YRangeType.DYNAMIC

    /**
     * The bounds expect the x max to be 0, and the x min to be a negative number of milliseconds
     * representing the full duration of the graph. Sorry, I don't know what I was thinking.
     */
    val bounds: RectRegion
        get() = RectRegion()

    /**
     * If the graph has no plottable data a message will be shown to the user instead of the graph.
     */
    val hasPlottableData: Boolean
        get() = false

    /**
     * The end time of the graph. The far right point on the graph.
     */
    val endTime: OffsetDateTime
        get() = OffsetDateTime.MIN

    /**
     * The x co-ordinate of each point is a negative number of milliseconds representing the
     * time between it and the end time.
     */
    val lines: List<Line>
        get() = emptyList()

    /**
     * Android plot parameters for they y axis divisions.
     */
    val yAxisSubdivides: Int
        get() = 11
}