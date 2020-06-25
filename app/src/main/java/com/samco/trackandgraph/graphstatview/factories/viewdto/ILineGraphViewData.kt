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
import com.samco.trackandgraph.database.dto.YRangeType
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.entity.LineGraphFeature
import org.threeten.bp.OffsetDateTime

interface ILineGraphViewData : IGraphStatViewData {
    val durationBasedRange: Boolean
        get() = false
    val yRangeType: YRangeType
        get() = YRangeType.DYNAMIC
    val bounds: RectRegion
        get() = RectRegion()
    val hasPlottableData: Boolean
        get() = false
    val endTime: OffsetDateTime
        get() = OffsetDateTime.MIN
    val plottableData: Map<LineGraphFeature, FastXYSeries?>
        get() = emptyMap()

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object : ILineGraphViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }
}