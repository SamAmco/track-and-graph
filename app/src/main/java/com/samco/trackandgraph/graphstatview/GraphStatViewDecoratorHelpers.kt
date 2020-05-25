/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview

import android.content.Context
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.database.GraphOrStat
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dataVisColorList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.ui.GraphLegendItemView
import org.threeten.bp.temporal.TemporalAmount

class RawDataSample(val dataPoints: List<DataPoint>, val plotFrom: Int)

class SampleDataCallback(val callback: (List<DataPoint>) -> Unit) : (List<DataPoint>) -> Unit {
    override fun invoke(dataPoints: List<DataPoint>) {
        callback.invoke(dataPoints)
    }
}

internal suspend fun sampleData(
    dataSource: TrackAndGraphDatabaseDao, featureId: Long, sampleDuration: Duration?,
    averagingDuration: Duration?, plotTotalTime: TemporalAmount?
): RawDataSample {
    return withContext(Dispatchers.IO) {
        if (sampleDuration == null) RawDataSample(
            dataSource.getDataPointsForFeatureAscSync(featureId),
            0
        )
        else {
            val latest = getLatestTimeOrNowForFeature(dataSource, featureId)
            val startDate = latest.minus(sampleDuration)
            val plottingDuration =
                plotTotalTime?.let { Duration.between(latest, latest.plus(plotTotalTime)) }
            val maxSampleDuration = listOf(
                sampleDuration,
                averagingDuration?.plus(sampleDuration),
                plottingDuration?.plus(sampleDuration)
            ).maxBy { d -> d ?: Duration.ZERO }
            val minSampleDate = latest.minus(maxSampleDuration)
            val dataPoints =
                dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
            val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
            RawDataSample(dataPoints, startIndex)
        }
    }
}

private fun getLatestTimeOrNowForFeature(
    dataSource: TrackAndGraphDatabaseDao,
    featureId: Long
): OffsetDateTime {
    val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
    val now = OffsetDateTime.now()
    val latest = lastDataPointList.firstOrNull()?.timestamp?.plusSeconds(1)
    return listOfNotNull(now, latest).max()!!
}

internal fun initHeader(binding: GraphStatViewBinding?, graphOrStat: GraphOrStat?) {
    val headerText = graphOrStat?.name ?: ""
    binding?.headerText?.text = headerText
}

internal fun dataPlottable(
    rawData: RawDataSample,
    minDataPoints: Int = 1
): Boolean {
    return rawData.plotFrom >= 0 && rawData.dataPoints.size - rawData.plotFrom >= minDataPoints
}

internal fun inflateGraphLegendItem(
    binding: GraphStatViewBinding, context: Context,
    colorIndex: Int, label: String
) {
    val colorId = dataVisColorList[colorIndex]
    binding.legendFlexboxLayout.addView(
        GraphLegendItemView(
            context,
            colorId,
            label
        )
    )
}

