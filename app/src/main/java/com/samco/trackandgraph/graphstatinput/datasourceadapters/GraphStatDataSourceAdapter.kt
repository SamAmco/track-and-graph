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

package com.samco.trackandgraph.graphstatinput.datasourceadapters

import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.entity.GraphOrStat

/**
 * An abstract adapter for retrieving and writing graph or stat configs to a database
 *
 * I is the type of object to be stored and retrieved by this adapter
 */
abstract class GraphStatDataSourceAdapter<I> {
    protected abstract suspend fun writeConfigToDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: I,
        updateMode: Boolean
    )

    protected abstract suspend fun getConfigDataFromDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, I>?

    @Suppress("UNCHECKED_CAST")
    suspend fun getConfigData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, Any>? {
        return getConfigDataFromDatabase(dataSource, graphOrStatId) as Pair<Long, Any>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun writeConfig(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: Any,
        updateMode: Boolean
    ) {
        writeConfigToDatabase(dataSource, graphOrStatId, config as I, updateMode)
    }

    protected abstract suspend fun shouldPreen(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ): Boolean

    suspend fun preen(dataSource: TrackAndGraphDatabaseDao, graphOrStat: GraphOrStat) {
        if (shouldPreen(dataSource, graphOrStat)) {
            dataSource.deleteGraphOrStat(graphOrStat)
        }
    }

    protected abstract suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    )

    suspend fun duplicateGraphOrStat(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ) {
        val originalId = graphOrStat.id
        val newId = dataSource.insertGraphOrStat(graphOrStat.copy(id = 0))
        duplicate(dataSource, originalId, newId)
    }
}