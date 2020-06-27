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

package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An abstract factory for generating data for a graph or stat ready to be displayed by an appropriate
 * decorator.
 *
 * I is the store of configuration options that tells the factory how to generate the data
 * T is the type of data produced by this factory
 */
abstract class ViewDataFactory<in I, out T : IGraphStatViewData>(
    protected val dataSource: TrackAndGraphDatabaseDao,
    protected val graphOrStat: GraphOrStat
) {
    protected abstract suspend fun createViewData(onDataSampled: (List<DataPoint>) -> Unit): T

    protected abstract suspend fun createViewData(
        config: I,
        onDataSampled: (List<DataPoint>) -> Unit
    ): T

    suspend fun getViewData(config: I, onDataSampled: (List<DataPoint>) -> Unit = {}): T =
        withContext(Dispatchers.IO) {
            return@withContext createViewData(config, onDataSampled)
        }

    suspend fun getViewData(onDataSampled: (List<DataPoint>) -> Unit = {}): T =
        withContext(Dispatchers.IO) {
            return@withContext createViewData(onDataSampled)
        }
}