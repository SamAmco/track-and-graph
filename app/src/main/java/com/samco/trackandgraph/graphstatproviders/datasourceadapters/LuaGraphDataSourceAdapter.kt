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
package com.samco.trackandgraph.graphstatproviders.datasourceadapters

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.model.DataInteractor
import javax.inject.Inject

class LuaGraphDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<LuaGraphWithFeatures>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStat: GraphOrStat,
        config: LuaGraphWithFeatures,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updateLuaGraph(graphOrStat, config)
        else dataInteractor.insertLuaGraph(graphOrStat, config)
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, LuaGraphWithFeatures>? {
        val luaGraph = dataInteractor.getLuaGraphByGraphStatId(graphOrStatId) ?: return null
        return Pair(luaGraph.id, luaGraph)
    }

    override suspend fun shouldPreen(graphOrStat: GraphOrStat): Boolean {
        return dataInteractor.getLuaGraphByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicateLuaGraph(graphOrStat)
    }
}
