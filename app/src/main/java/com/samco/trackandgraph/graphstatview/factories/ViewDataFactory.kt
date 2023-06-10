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
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.util.Stopwatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

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

    /**
     * If the config has been written to the database already it can be retrieved against the
     * given graph or stat. This should just get the config and call the other createViewData
     * function.
     *
     * @see createViewData(graphOrStat: GraphOrStat, config: I, onDataSampled: (List<DataPoint>) -> Unit)
     */
    protected abstract suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): T

    /**
     * Reads the given graph or stat from the database and generates the data to be displayed.
     * [onDataSampled] will be called at some point with a list of data points that have been
     * sampled from the database. No guarantees are made about the sorting or uniqueness of the
     * data points.
     */
    protected abstract suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: I,
        onDataSampled: (List<DataPoint>) -> Unit
    ): T

    /**
     * Should return true if any change in the data for the given feature might affect the data
     * produced by this factory.
     */
    abstract suspend fun affectedBy(graphOrStatId: Long, featureId: Long): Boolean

    @Suppress("UNCHECKED_CAST")
    suspend fun getViewData(
        graphOrStat: GraphOrStat,
        config: Any,
        onDataSampled: (List<DataPoint>) -> Unit = {}
    ): T = timeCreateViewData(graphOrStat) {
        createViewData(graphOrStat, config as I, onDataSampled)
    }

    suspend fun getViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit = {}
    ): T = timeCreateViewData(graphOrStat) {
        createViewData(graphOrStat, onDataSampled)
    }

    private suspend fun timeCreateViewData(
        graphOrStat: GraphOrStat,
        createDelegate: suspend () -> T
    ): T = withContext(ioDispatcher) {
        val stopwatch = Stopwatch().apply { start() }
        val viewData = createDelegate()
        stopwatch.stop()
        Timber.i("Took ${stopwatch.elapsedMillis}ms to generate view data for ${graphOrStat.id}:${graphOrStat.name}")
        return@withContext viewData
    }
}