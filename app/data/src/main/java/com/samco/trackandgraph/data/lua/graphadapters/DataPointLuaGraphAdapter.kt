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
package com.samco.trackandgraph.data.lua.graphadapters

import com.samco.trackandgraph.data.lua.apiimpl.DataPointParser
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class DataPointLuaGraphAdapter @Inject constructor(
    private val dataPointParser: DataPointParser,
) : LuaGraphAdaptor<LuaGraphResultData.DataPointData> {

    companion object {
        const val IS_DURATION = "isduration"
        const val DATAPOINT = "datapoint"
    }

    override fun process(data: LuaValue): LuaGraphResultData.DataPointData {
        return LuaGraphResultData.DataPointData(
            dataPoint = dataPointParser.parseDataPoint(data[DATAPOINT]),
            isDuration = data[IS_DURATION].optboolean(false),
        )
    }
}
