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
import android.view.View
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatTimeSinceDecorator(
    private val graphOrStat: GraphOrStat,
    private val timeSinceLastStat: TimeSinceLastStat,
    private val onSampledDataCallback: SampleDataCallback?
) : IGraphStatViewDecorator {
    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var dataSource: TrackAndGraphDatabaseDao? = null

    override suspend fun decorate(view: IDecoratableGraphStatView) {
        binding = view.getBinding()
        context = view.getContext()
        dataSource = view.getDataSource()

        binding!!.statMessage.visibility = View.INVISIBLE
        initHeader(binding, graphOrStat)
        initTimeSinceStatBody()
    }

    override fun setTimeMarker(time: OffsetDateTime) { }

    private suspend fun initTimeSinceStatBody() {
        binding!!.progressBar.visibility = View.VISIBLE
        val lastDataPoint = withContext(Dispatchers.IO) { getLastDataPoint() }
        binding!!.statMessage.visibility = View.VISIBLE
        binding!!.progressBar.visibility = View.GONE
        if (lastDataPoint == null) {
            onSampledDataCallback?.invoke(emptyList())
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_stat)
        } else while (true) {
            onSampledDataCallback?.invoke(listOf(lastDataPoint))
            setTimeSinceStatText(
                Duration.between(
                    lastDataPoint.timestamp,
                    graphOrStat.endDate ?: OffsetDateTime.now()
                )
            )
            delay(1000)
        }
    }

    private fun getLastDataPoint(): DataPoint? {
        val feature = dataSource!!.getFeatureById(timeSinceLastStat.featureId)
        val endDate = graphOrStat.endDate ?: OffsetDateTime.now()
        return if (feature.featureType == FeatureType.CONTINUOUS) {
            dataSource!!.getLastDataPointBetween(
                timeSinceLastStat.featureId,
                timeSinceLastStat.fromValue,
                timeSinceLastStat.toValue,
                endDate
            )
        } else {
            dataSource!!.getLastDataPointWithValue(
                timeSinceLastStat.featureId,
                timeSinceLastStat.discreteValues,
                endDate
            )
        }
    }

    private fun setTimeSinceStatText(duration: Duration) {
        val totalSeconds = duration.toMillis() / 1000.toDouble()
        val daysNum = (totalSeconds / 86400).toInt()
        val days = daysNum.toString()
        val hours = "%02d".format(((totalSeconds % 86400) / 3600).toInt())
        val minutes = "%02d".format(((totalSeconds % 3600) / 60).toInt())
        val seconds = "%02d".format((totalSeconds % 60).toInt())
        val hms = "$hours:$minutes:$seconds"
        binding!!.statMessage.text = when {
            daysNum == 1 -> "$days ${context!!.getString(R.string.day)}\n$hms"
            daysNum > 0 -> "$days ${context!!.getString(R.string.days)}\n$hms"
            else -> hms
        }
    }
}