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

import com.samco.trackandgraph.database.DataSamplerImpl
import com.samco.trackandgraph.database.DataSource
import com.samco.trackandgraph.database.IDataSampler
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.functionslib.*
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.exceptions.NotEnoughDataException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration

class AverageTimeBetweenDataFactory :
    ViewDataFactory<AverageTimeBetweenStat, IAverageTimeBetweenViewData>() {

    companion object {
        /**
         * Calculates the average duration between the timestamps of a set of data points. This is
         * simply the duration between first and last divided by the number of points minus 1.
         */
        internal fun calculateAverageTimeBetweenOrNull(
            dataPoints: List<IDataPoint>
        ): Double {
            if (dataPoints.size < 2) throw Exception("Don't call this method with less than 2 data points.")
            //The data points will be in order newest to oldest
            val last = dataPoints.first().timestamp
            val first = dataPoints.last().timestamp
            return Duration.between(first, last).toMillis()
                .toDouble() / (dataPoints.size - 1).toDouble()
        }
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val timeBetweenStat = dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(dataSource, graphOrStat, timeBetweenStat, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: AverageTimeBetweenStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        return try {
            val feature = dataSource.getFeatureById(config.featureId)
            val dataSampler = DataSamplerImpl(dataSource)
            val dataSample = withContext(Dispatchers.IO) {
                getRelevantDataPoints(dataSampler, config, feature)
            }
            val dataPoints = withContext(Dispatchers.IO) {
                dataSample.toList()
            }
            if (dataPoints.size < 2) return notEnoughData(graphOrStat, dataPoints.size)
            val averageMillis = withContext(Dispatchers.Default) {
                calculateAverageTimeBetweenOrNull(dataPoints)
            }
            onDataSampled(dataSample.getRawDataPoints())
            object : IAverageTimeBetweenViewData {
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
                override val averageMillis = averageMillis
            }
        } catch (throwable: Throwable) {
            object : IAverageTimeBetweenViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
        }
    }

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : IAverageTimeBetweenViewData {
            override val error = GraphNotFoundException()
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
        }

    private fun notEnoughData(graphOrStat: GraphOrStat, numDataPoints: Int) =
        object : IAverageTimeBetweenViewData {
            override val error = NotEnoughDataException(numDataPoints)
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
        }

    private suspend fun getRelevantDataPoints(
        dataSampler: IDataSampler,
        config: AverageTimeBetweenStat,
        feature: Feature
    ): DataSample {
        val dataSource = DataSource.FeatureDataSource(feature.id)
        val dataSample = dataSampler.getDataSampleForSource(dataSource)
        val filters = mutableListOf<DataSampleFunction>()
        if (config.filterByLabels) filters.add(FilterLabelFunction(config.labels.toSet()))
        if (config.filterByRange) filters.add(FilterValueFunction(config.fromValue, config.toValue))
        filters.add(DataClippingFunction(config.endDate, config.duration))
        return CompositeFunction(filters).mapSample(dataSample)
    }
}