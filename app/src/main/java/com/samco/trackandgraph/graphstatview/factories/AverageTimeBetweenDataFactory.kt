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
import com.samco.trackandgraph.functionslib.CompositeFunction
import com.samco.trackandgraph.functionslib.DataClippingFunction
import com.samco.trackandgraph.functionslib.DataSample
import com.samco.trackandgraph.functionslib.FilterValueFunction
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import kotlin.math.max

class AverageTimeBetweenDataFactory :
    ViewDataFactory<AverageTimeBetweenStat, IAverageTimeBetweenViewData>() {

    companion object {
        /**
         * Calculates the average duration between the timestamps of a set of data points. This is
         * simply the duration divided by the number of points plus 1. The duration is calculated
         * as start -> end. Start is end minus [duration] if it exists and first [dataPoints]
         * timestamp otherwise. End is [endDate] if given and last [dataPoints] timestamp or [now]
         * (whichever is later) otherwise. If there are not 2 or more elements in [dataPoints] with
         * timestamps falling in this range then null is returned.
         */
        internal fun calculateAverageTimeBetweenOrNull(
            now: OffsetDateTime,
            endDate: OffsetDateTime?,
            duration: Duration?,
            dataPoints: List<IDataPoint>
        ): Double? {
            if (duration == null && dataPoints.size < 2) return null
            val last = dataPoints.lastOrNull()?.timestamp
            val first = dataPoints.firstOrNull()?.timestamp
            val latest = endDate ?: last?.let { listOf(now, it).maxOrNull() } ?: now
            val start = duration?.let { latest.minus(it) } ?: first
            //Although we will have only sampled points likely to be in the duration it is possible that
            // we could have been passed points that start before (latest - duration). So this is a good
            // final check.
            val clippedPoints = dataPoints
                .dropWhile { it.timestamp.isBefore(start) }
                .dropLastWhile { it.timestamp.isAfter(latest) }
            val totalMillis = Duration.between(start, latest).toMillis().toDouble()
            var divisor = clippedPoints.size + 1
            if (latest == last) divisor -= 1
            if (start == first) divisor -= 1
            return totalMillis / max(1.0, divisor.toDouble())
        }
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val timeBetweenStat = dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id)
            ?: return notEnoughData(graphOrStat)
        return createViewData(dataSource, graphOrStat, timeBetweenStat, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: AverageTimeBetweenStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val feature = dataSource.getFeatureById(config.featureId)
        val dataSampler = DataSamplerImpl(dataSource)
        val dataSample = withContext(Dispatchers.IO) {
            getRelevantDataPoints(dataSampler, config, feature)
        }
        val dataPoints = dataSample.toList()
        val averageMillis = withContext(Dispatchers.Default) {
            calculateAverageTimeBetweenOrNull(
                OffsetDateTime.now(),
                config.endDate,
                config.duration,
                dataPoints
            )
        } ?: return notEnoughData(graphOrStat)
        onDataSampled(dataSample.getRawDataPoints())
        return object : IAverageTimeBetweenViewData {
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat
            override val averageMillis = averageMillis
            override val hasEnoughData = true
        }
    }

    private fun notEnoughData(graphOrStat: GraphOrStat) = object : IAverageTimeBetweenViewData {
        override val state = IGraphStatViewData.State.READY
        override val graphOrStat = graphOrStat
    }

    private suspend fun getRelevantDataPoints(
        dataSampler: IDataSampler,
        config: AverageTimeBetweenStat,
        feature: Feature
    ): DataSample {
        val dataSource = DataSource.FeatureDataSource(feature.id)
        val dataSample = dataSampler.getDataPointsForDataSource(dataSource)
        return CompositeFunction(
            FilterValueFunction(
                config.fromValue.toDouble(),
                config.toValue.toDouble(),
                config.discreteValues
            ),
            DataClippingFunction(config.endDate, config.duration),
        ).mapSample(dataSample)
    }
}