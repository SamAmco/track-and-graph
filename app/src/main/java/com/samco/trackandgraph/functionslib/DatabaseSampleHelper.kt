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

package com.samco.trackandgraph.functionslib

import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.FeatureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount

class DatabaseSampleHelper(
    private val dataSource: TrackAndGraphDatabaseDao
) {
    /**
     * This function will call the dataSource to get a sample of the data points with the given featureId.
     * The end date of this sample is calculated as:
     *  - If an endDate is given then it is used
     *  - If an endDate is not given then the last data point tracked or the current date/time is used (which ever is later)
     * The start date of this sample is calculated as:
     *  - The beginning of time if no sampleDuration is provided
     *  - If a sampleDuration is provided then it is the end date minus the sample duration. However if there is
     *      a plotTotalTime or averagingDuration provided as well then which ever of the two is larger will be added
     *      to the sampleDuration before it is subtracted from the end date such that all relevant information
     *      is contained in the sample.
     *
     * Note: No actual averaging or totalling is performed by this function, it just collects all relevant data.
     */
    suspend fun sampleData(
        featureId: Long, sampleDuration: Duration?, endDate: OffsetDateTime?,
        averagingDuration: Duration?, plotTotalTime: TemporalAmount?
    ): DataSample {
        return withContext(Dispatchers.IO) {
            // try to get the feature type from the db.
            // some tests use a mock DAO which returns null when asking for the feature type
            // this mock database also returns no points thus allowing to add an edge case
            // thus when the list of points is empty we use the DataSample.emptySample factory which provides a fallback featureType
            // if the list isn't empty we are in a "real" setting in which we should always be able to get the featureType
            val featureType: FeatureType? = dataSource.tryGetFeatureByIdSync(featureId)?.featureType
            if (sampleDuration == null && endDate == null) {
                val points = dataSource.getDataPointsForFeatureAscSync(featureId)
                if (points.isNotEmpty()) DataSample(points, featureType!!, featureId)
                else DataSample.emptySample(featureType, featureId) // needed for test where featureType is null
            } else {
                val latest = endDate ?: getLastTrackedTimeOrNow(
                    dataSource,
                    featureId
                )
                val plottingDuration =
                    plotTotalTime?.let { Duration.between(latest, latest.plus(plotTotalTime)) }
                val minSampleDate = sampleDuration?.let {
                    val possibleLongestDurations = listOf(
                        sampleDuration,
                        averagingDuration?.plus(sampleDuration),
                        plottingDuration?.plus(sampleDuration)
                    )
                    latest.minus(possibleLongestDurations.maxBy { d -> d ?: Duration.ZERO })
                } ?: OffsetDateTime.MIN
                val points =
                    dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
                if (points.isNotEmpty()) DataSample(points, featureType!!, featureId)
                else DataSample.emptySample(featureType, featureId) // needed for test where featureType is null
            }
        }
    }

    private fun getLastTrackedTimeOrNow(
        dataSource: TrackAndGraphDatabaseDao,
        featureId: Long
    ): OffsetDateTime {
        val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
        val now = OffsetDateTime.now()
        val latest = lastDataPointList.firstOrNull()?.timestamp?.plusSeconds(1)
        return listOfNotNull(now, latest).max()!!
    }
}