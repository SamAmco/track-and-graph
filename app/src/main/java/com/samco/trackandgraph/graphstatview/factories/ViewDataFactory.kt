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

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.di.IODispatcher
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * An abstract factory for generating data for a graph or stat ready to be displayed by an appropriate
 * decorator.
 *
 * I is the store of configuration options that tells the factory how to generate the data
 * T is the type of data produced by this factory
 */
abstract class ViewDataFactory<in I, out T : IGraphStatViewData>(
    protected val dataInteractor: DataInteractor,
    protected val ioDispatcher: CoroutineDispatcher
) {
    protected abstract suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): T

    protected abstract suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: I,
        onDataSampled: (List<DataPoint>) -> Unit
    ): T

    @Suppress("UNCHECKED_CAST")
    suspend fun getViewData(
        graphOrStat: GraphOrStat,
        config: Any,
        onDataSampled: (List<DataPoint>) -> Unit = {}
    ): T = withContext(ioDispatcher) {
        return@withContext createViewData(graphOrStat, config as I, onDataSampled)
    }

    suspend fun getViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit = {}
    ): T = withContext(ioDispatcher) {
        return@withContext createViewData(graphOrStat, onDataSampled)
    }
}