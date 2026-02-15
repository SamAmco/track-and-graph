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

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.database.dto.LastValueStatCreateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStatUpdateRequest
import com.samco.trackandgraph.data.interactor.DataInteractor
import javax.inject.Inject

class LastValueDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<LastValueStat>(dataInteractor) {

    override suspend fun createInDatabase(
        name: String,
        groupId: Long,
        config: LastValueStat
    ) {
        dataInteractor.createLastValueStat(
            LastValueStatCreateRequest(
                name = name,
                groupId = groupId,
                config = config.toConfig()
            )
        )
    }

    override suspend fun updateInDatabase(
        graphStatId: Long,
        name: String,
        config: LastValueStat
    ) {
        dataInteractor.updateLastValueStat(
            LastValueStatUpdateRequest(
                graphStatId = graphStatId,
                name = name,
                config = config.toConfig()
            )
        )
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, LastValueStat>? {
        val lvs = dataInteractor.getLastValueStatByGraphStatId(graphOrStatId) ?: return null
        return Pair(lvs.id, lvs)
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat, groupId: Long) {
        dataInteractor.duplicateLastValueStat(graphOrStat.id, groupId)
    }
}
