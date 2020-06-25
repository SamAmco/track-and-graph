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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import org.threeten.bp.OffsetDateTime

class GraphStatAverageTimeBetweenDecorator :
    IGraphStatViewDecorator<IAverageTimeBetweenViewData> {
    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var data: IAverageTimeBetweenViewData? = null

    override suspend fun decorate(
        view: IDecoratableGraphStatView,
        data: IAverageTimeBetweenViewData
    ) {
        this.data = data
        binding = view.getBinding()
        context = view.getContext()

        binding!!.statMessage.visibility = View.INVISIBLE
        initAverageTimeBetweenStatBody()
    }

    override fun setTimeMarker(time: OffsetDateTime) {}

    @SuppressLint("SetTextI18n")
    private fun initAverageTimeBetweenStatBody() {
        if (data!!.hasEnoughData) {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_stat)
        } else {
            val days = "%.1f".format((data!!.averageMillis / 86400000).toFloat())
            binding!!.statMessage.text = "$days ${context!!.getString(R.string.days)}"
        }
        binding!!.statMessage.visibility = View.VISIBLE
    }
}