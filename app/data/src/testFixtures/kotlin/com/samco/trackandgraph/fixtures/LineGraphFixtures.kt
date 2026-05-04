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

package com.samco.trackandgraph.fixtures

import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle

fun testLineGraphFeature(
    id: Long = 1L,
    lineGraphId: Long = 1L,
    featureId: Long = id,
    name: String = "Feature",
    colorIndex: Int = 0,
    averagingMode: LineGraphAveragingModes = LineGraphAveragingModes.NO_AVERAGING,
    plottingMode: LineGraphPlottingModes = LineGraphPlottingModes.WHEN_TRACKED,
    pointStyle: LineGraphPointStyle = LineGraphPointStyle.NONE,
    offset: Double = 0.0,
    scale: Double = 1.0,
    durationPlottingMode: DurationPlottingMode = DurationPlottingMode.NONE,
) = LineGraphFeature(
    id = id,
    lineGraphId = lineGraphId,
    featureId = featureId,
    name = name,
    colorIndex = colorIndex,
    averagingMode = averagingMode,
    plottingMode = plottingMode,
    pointStyle = pointStyle,
    offset = offset,
    scale = scale,
    durationPlottingMode = durationPlottingMode,
)
