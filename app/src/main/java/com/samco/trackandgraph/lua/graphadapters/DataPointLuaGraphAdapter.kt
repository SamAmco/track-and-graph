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
package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.apiimpl.DateTimeParser
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class DataPointLuaGraphAdapter @Inject constructor(
    private val dateTimeParser: DateTimeParser,
) : LuaGraphAdaptor<LuaGraphResultData.DataPointData> {
    override fun process(data: LuaValue): LuaGraphResultData.DataPointData {
        if (data == LuaValue.NIL) return LuaGraphResultData.DataPointData(
            dataPoint = null,
            isDuration = false
        )

        val timestamp = dateTimeParser.parseDateTime(data)

        return LuaGraphResultData.DataPointData(
            dataPoint = DataPoint(
                timestamp = timestamp.toOffsetDateTime(),
                featureId = data[LuaEngine.FEATURE_ID].optlong(0L),
                value = data[LuaEngine.VALUE].optdouble(0.0),
                label = data[LuaEngine.LABEL].optjstring("") ?: "",
                note = data[LuaEngine.NOTE].optjstring("") ?: ""
            ),
            isDuration = data[LuaEngine.IS_DURATION].optboolean(false),
        )
    }
}
