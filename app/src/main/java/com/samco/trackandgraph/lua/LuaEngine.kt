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
package com.samco.trackandgraph.lua

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.lua.dto.LuaGraphResult

interface LuaEngine {
    companion object {
        const val OFFSET = "offset"
        const val DATA = "data"
        const val TNG = "Tng"
        const val GRAPH = "graph"
        const val NEXT_DP = "nextdp"
        const val NEXT_DP_BATCH = "nextdpbatch"
        const val TYPE = "type"
        const val DATAPOINT = "datapoint"
        const val IS_DURATION = "isDuration"
        const val TIMESTAMP = "timestamp"
        const val FEATURE_ID = "featureId"
        const val VALUE = "value"
        const val LABEL = "label"
        const val NOTE = "note"
    }


    fun runLuaGraphScript(
        script: String,
        next: (String, Int) -> List<DataPoint>
    ): LuaGraphResult
}
