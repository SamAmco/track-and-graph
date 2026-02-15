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

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.interactor.DataInteractor

/**
 * An abstract adapter for retrieving and writing graph or stat configs to a database
 *
 * I is the type of object to be stored and retrieved by this adapter
 */
abstract class GraphStatDataSourceAdapter<I>(
    protected val dataInteractor: DataInteractor
) {
    protected abstract suspend fun createInDatabase(
        name: String,
        groupId: Long,
        config: I
    )

    protected abstract suspend fun updateInDatabase(
        graphStatId: Long,
        name: String,
        config: I
    )

    protected abstract suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, I>?

    suspend fun getConfigData(graphOrStatId: Long): Pair<Long, Any>? {
        return (getConfigDataFromDatabase(graphOrStatId) ?: return null).let {
            Pair(it.first, it.second as Any)
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun create(name: String, groupId: Long, config: Any) {
        createInDatabase(name, groupId, config as I)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun update(graphStatId: Long, name: String, config: Any) {
        updateInDatabase(graphStatId, name, config as I)
    }

    abstract suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat, groupId: Long)
}