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

import com.samco.trackandgraph.data.database.dto.GraphOrStat

interface IAverageTimeBetweenViewData : IGraphStatViewData{
    //The total millis between the first and last data point
    val averageMillis: Double
        get() = 0.0

    val enoughData: Boolean
        get() = false

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object : IAverageTimeBetweenViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }
}