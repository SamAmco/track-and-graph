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

package com.samco.trackandgraph.graphstatview.decorators

import android.content.Context
import android.view.View
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData
import kotlinx.coroutines.delay
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatTimeSinceDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ITimeSinceViewData>(listMode) {
    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var data: ITimeSinceViewData? = null

    override suspend fun decorate(view: IDecoratableGraphStatView, data: ITimeSinceViewData) {
        binding = view.getBinding()
        context = view.getContext()
        this.data = data

        binding!!.statMessage.visibility = View.INVISIBLE
        initTimeSinceStatBody()
    }

    override fun setTimeMarker(time: OffsetDateTime) {}

    private suspend fun initTimeSinceStatBody() {
        binding!!.progressBar.visibility = View.VISIBLE
        binding!!.statMessage.visibility = View.VISIBLE
        binding!!.progressBar.visibility = View.GONE
        val dataPoint = data!!.lastDataPoint
        if (dataPoint == null) {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_stat)
        } else while (true) {
            setTimeSinceStatText(Duration.between(dataPoint.timestamp, OffsetDateTime.now()))
            delay(1000)
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