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

package com.samco.trackandgraph.graphstatproviders.datasourceadapters

import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatCreateRequest
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.interactor.DataInteractor
import javax.inject.Inject

class AverageTimeBetweenDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<AverageTimeBetweenStat>(dataInteractor) {

    override suspend fun createInDatabase(
        name: String,
        groupId: Long,
        config: AverageTimeBetweenStat
    ) {
        dataInteractor.createAverageTimeBetweenStat(
            AverageTimeBetweenStatCreateRequest(
                name = name,
                groupId = groupId,
                config = config.toConfig()
            )
        )
    }

    override suspend fun updateInDatabase(
        graphStatId: Long,
        name: String,
        config: AverageTimeBetweenStat
    ) {
        dataInteractor.updateAverageTimeBetweenStat(
            AverageTimeBetweenStatUpdateRequest(
                graphStatId = graphStatId,
                name = name,
                config = config.toConfig()
            )
        )
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, AverageTimeBetweenStat>? {
        val ats =
            dataInteractor.getAverageTimeBetweenStatByGraphStatId(graphOrStatId) ?: return null
        return Pair(ats.id, ats)
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat, groupId: Long) {
        dataInteractor.duplicateAverageTimeBetweenStat(graphOrStat.id, groupId)
    }
}
