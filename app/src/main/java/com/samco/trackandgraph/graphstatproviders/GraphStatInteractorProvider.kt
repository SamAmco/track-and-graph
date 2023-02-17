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

package com.samco.trackandgraph.graphstatproviders

import android.content.Context
import androidx.compose.runtime.Composable
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatinput.configviews.*
import com.samco.trackandgraph.graphstatinput.configviews.AverageTimeBetweenConfigView
import com.samco.trackandgraph.graphstatinput.configviews.LineGraphConfigView
import com.samco.trackandgraph.graphstatinput.configviews.PieChartConfigView
import com.samco.trackandgraph.graphstatinput.configviews.TimeSinceConfigView
import com.samco.trackandgraph.graphstatinput.datasourceadapters.*
import com.samco.trackandgraph.graphstatview.decorators.*
import com.samco.trackandgraph.graphstatview.factories.*
import javax.inject.Inject

interface GraphStatInteractorProvider {
    fun getDataFactory(type: GraphStatType): ViewDataFactory<*, *>
    //TODO I think the composable function needs to take an interface containing a subset of the
    // functions of GraphStatInputViewModel
    fun getConfigView(type: GraphStatType, context: Context): GraphStatConfigView//@Composable () -> Unit
    fun getDataSourceAdapter(type: GraphStatType): GraphStatDataSourceAdapter<*>
    fun getDecorator(type: GraphStatType, listMode: Boolean): GraphStatViewDecorator<*>
}

class GraphStatInteractorProviderImpl @Inject constructor(
    private val lineGraphDataFactory: LineGraphDataFactory,
    private val lineGraphDataSourceAdapter: LineGraphDataSourceAdapter,
    private val pieChartDataFactory: PieChartDataFactory,
    private val pieChartDataSourceAdapter: PieChartDataSourceAdapter,
    private val averageTimeBetweenDataFactory: AverageTimeBetweenDataFactory,
    private val averageTimeBetweenDataSourceAdapter: AverageTimeBetweenDataSourceAdapter,
    private val timeSinceDataFactory: TimeSinceDataFactory,
    private val timeSinceDataSourceAdapter: TimeSinceDataSourceAdapter,
    private val timeHistogramDataFactory: TimeHistogramDataFactory,
    private val timeHistogramDataSourceAdapter: TimeHistogramDataSourceAdapter
) : GraphStatInteractorProvider {

    override fun getDataFactory(type: GraphStatType): ViewDataFactory<*, *> {
        return when (type) {
            GraphStatType.LINE_GRAPH -> lineGraphDataFactory
            GraphStatType.TIME_SINCE -> timeSinceDataFactory
            GraphStatType.PIE_CHART -> pieChartDataFactory
            GraphStatType.TIME_HISTOGRAM -> timeHistogramDataFactory
            GraphStatType.AVERAGE_TIME_BETWEEN -> averageTimeBetweenDataFactory
        }
    }

    override fun getConfigView(type: GraphStatType, context: Context): GraphStatConfigView {
        return when (type) {
            GraphStatType.LINE_GRAPH -> LineGraphConfigView(context, null, 0)
            GraphStatType.PIE_CHART -> PieChartConfigView(context, null, 0)
            GraphStatType.TIME_HISTOGRAM -> TimeHistogramConfigView(context, null, 0)
            GraphStatType.AVERAGE_TIME_BETWEEN -> AverageTimeBetweenConfigView(context, null, 0)
            GraphStatType.TIME_SINCE -> TimeSinceConfigView(context, null, 0)
        }
    }

    override fun getDataSourceAdapter(type: GraphStatType): GraphStatDataSourceAdapter<*> {
        return when (type) {
            GraphStatType.LINE_GRAPH -> lineGraphDataSourceAdapter
            GraphStatType.PIE_CHART -> pieChartDataSourceAdapter
            GraphStatType.AVERAGE_TIME_BETWEEN -> averageTimeBetweenDataSourceAdapter
            GraphStatType.TIME_SINCE -> timeSinceDataSourceAdapter
            GraphStatType.TIME_HISTOGRAM -> timeHistogramDataSourceAdapter
        }
    }

    override fun getDecorator(type: GraphStatType, listMode: Boolean): GraphStatViewDecorator<*> {
        return when (type) {
            GraphStatType.LINE_GRAPH -> GraphStatLineGraphDecorator(listMode)
            GraphStatType.PIE_CHART -> GraphStatPieChartDecorator(listMode)
            GraphStatType.TIME_HISTOGRAM -> GraphStatTimeHistogramDecorator(listMode)
            GraphStatType.TIME_SINCE -> GraphStatTimeSinceDecorator(listMode)
            GraphStatType.AVERAGE_TIME_BETWEEN -> GraphStatAverageTimeBetweenDecorator(listMode)
        }
    }


}
