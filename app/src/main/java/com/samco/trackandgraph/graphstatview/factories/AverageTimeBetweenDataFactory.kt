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
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class AverageTimeBetweenDataFactory(
    dataSource: TrackAndGraphDatabaseDao,
    graphOrStat: GraphOrStat
) : ViewDataFactory<IAverageTimeBetweenViewData>(
    dataSource,
    graphOrStat
) {
    fun createViewData(
        timeBetweenStat: AverageTimeBetweenStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val feature = dataSource.getFeatureById(timeBetweenStat.featureId)
        val dataPoints = getRelevantDataPoints(timeBetweenStat, feature)
        if (dataPoints.size < 2) return notEnoughData()
        val totalMillis = Duration.between(
            dataPoints.first().timestamp,
            dataPoints.last().timestamp
        ).toMillis().toDouble()
        val durationMillis = when (feature.featureType) {
            FeatureType.DURATION -> dataPoints.drop(1).sumByDouble { it.value }
            else -> 0.0
        }
        val totalGapMillis =
            if (durationMillis > totalMillis) totalMillis
            else totalMillis - durationMillis
        val averageMillis = totalGapMillis / (dataPoints.size - 1).toDouble()
        onDataSampled(dataPoints)
        return object : IAverageTimeBetweenViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = this@AverageTimeBetweenDataFactory.graphOrStat
            override val averageMillis: Double
                get() = averageMillis
            override val hasEnoughData: Boolean
                get() = true
        }
    }

    override suspend fun createViewData(onDataSampled: (List<DataPoint>) -> Unit): IAverageTimeBetweenViewData {
        val timeBetweenStat = dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id)
            ?: return notEnoughData()
        return createViewData(timeBetweenStat, onDataSampled)
    }

    private fun notEnoughData() = object : IAverageTimeBetweenViewData {
        override val state: IGraphStatViewData.State
            get() = IGraphStatViewData.State.READY
        override val graphOrStat: GraphOrStat
            get() = this@AverageTimeBetweenDataFactory.graphOrStat
    }

    private fun getRelevantDataPoints(
        timeBetweenStat: AverageTimeBetweenStat,
        feature: Feature
    ): List<DataPoint> {
        val endDate = graphOrStat.endDate ?: OffsetDateTime.now()
        val startDate =
            timeBetweenStat.duration?.let { endDate.minus(it) } ?: OffsetDateTime.MIN
        return when (feature.featureType) {
            FeatureType.CONTINUOUS, FeatureType.DURATION -> {
                dataSource.getDataPointsBetweenInTimeRange(
                    feature.id,
                    timeBetweenStat.fromValue,
                    timeBetweenStat.toValue,
                    startDate,
                    endDate
                )
            }
            FeatureType.DISCRETE -> {
                dataSource.getDataPointsWithValueInTimeRange(
                    feature.id,
                    timeBetweenStat.discreteValues,
                    startDate,
                    endDate
                )
            }
        }
    }

}