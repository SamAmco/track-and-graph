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

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.model.DataInteractor

/**
 * An abstract adapter for retrieving and writing graph or stat configs to a database
 *
 * I is the type of object to be stored and retrieved by this adapter
 */
abstract class GraphStatDataSourceAdapter<I> {
    protected abstract suspend fun writeConfigToDatabase(
        dataInteractor: DataInteractor,
        graphOrStatId: Long,
        config: I,
        updateMode: Boolean
    )

    protected abstract suspend fun getConfigDataFromDatabase(
        dataInteractor: DataInteractor,
        graphOrStatId: Long
    ): Pair<Long, I>?

    suspend fun getConfigData(
        dataInteractor: DataInteractor,
        graphOrStatId: Long
    ): Pair<Long, Any>? {
        return (getConfigDataFromDatabase(dataInteractor, graphOrStatId) ?: return null).let {
            Pair(it.first, it.second as Any)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun writeConfig(
        dataInteractor: DataInteractor,
        graphOrStatId: Long,
        config: Any,
        updateMode: Boolean
    ) {
        writeConfigToDatabase(dataInteractor, graphOrStatId, config as I, updateMode)
    }

    protected abstract suspend fun shouldPreen(
        dataInteractor: DataInteractor,
        graphOrStat: GraphOrStat
    ): Boolean

    suspend fun preen(
        dataInteractor: DataInteractor,
        graphOrStat: GraphOrStat
    ) {
        if (shouldPreen(dataInteractor, graphOrStat)) {
            dataInteractor.deleteGraphOrStat(graphOrStat)
        }
    }

    protected abstract suspend fun duplicate(
        dataInteractor: DataInteractor,
        oldGraphId: Long,
        newGraphId: Long
    )

    suspend fun duplicateGraphOrStat(
        dataInteractor: DataInteractor,
        graphOrStat: GraphOrStat
    ) {
        val originalId = graphOrStat.id
        val newId = dataInteractor.insertGraphOrStat(graphOrStat.copy(id = 0))
        duplicate(dataInteractor, originalId, newId)
    }
}