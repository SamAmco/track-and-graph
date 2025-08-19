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

import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.AverageTimeBetweenDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.BarChartDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.GraphStatDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.LastValueDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.LineGraphDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.LuaGraphDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.PieChartDataSourceAdapter
import com.samco.trackandgraph.graphstatproviders.datasourceadapters.TimeHistogramDataSourceAdapter
import com.samco.trackandgraph.graphstatview.factories.AverageTimeBetweenDataFactory
import com.samco.trackandgraph.graphstatview.factories.BarChartDataFactory
import com.samco.trackandgraph.graphstatview.factories.LastValueDataFactory
import com.samco.trackandgraph.graphstatview.factories.LineGraphDataFactory
import com.samco.trackandgraph.graphstatview.factories.LuaGraphDataFactory
import com.samco.trackandgraph.graphstatview.factories.PieChartDataFactory
import com.samco.trackandgraph.graphstatview.factories.TimeHistogramDataFactory
import com.samco.trackandgraph.graphstatview.factories.ViewDataFactory
import javax.inject.Inject

interface GraphStatInteractorProvider {
    fun getDataFactory(type: GraphStatType): ViewDataFactory<*, *>
    fun getDataSourceAdapter(type: GraphStatType): GraphStatDataSourceAdapter<*>
}

class GraphStatInteractorProviderImpl @Inject constructor(
    private val lineGraphDataFactory: LineGraphDataFactory,
    private val lineGraphDataSourceAdapter: LineGraphDataSourceAdapter,
    private val pieChartDataFactory: PieChartDataFactory,
    private val pieChartDataSourceAdapter: PieChartDataSourceAdapter,
    private val averageTimeBetweenDataFactory: AverageTimeBetweenDataFactory,
    private val averageTimeBetweenDataSourceAdapter: AverageTimeBetweenDataSourceAdapter,
    private val timeHistogramDataFactory: TimeHistogramDataFactory,
    private val timeHistogramDataSourceAdapter: TimeHistogramDataSourceAdapter,
    private val lastValueDataFactory: LastValueDataFactory,
    private val lastValueDataSourceAdapter: LastValueDataSourceAdapter,
    private val barChartDataFactory: BarChartDataFactory,
    private val barChartDataSourceAdapter: BarChartDataSourceAdapter,
    private val luaGraphDataFactory: LuaGraphDataFactory,
    private val luaGraphDataSourceAdapter: LuaGraphDataSourceAdapter,
) : GraphStatInteractorProvider {

    override fun getDataFactory(type: GraphStatType): ViewDataFactory<*, *> {
        return when (type) {
            GraphStatType.LINE_GRAPH -> lineGraphDataFactory
            GraphStatType.PIE_CHART -> pieChartDataFactory
            GraphStatType.TIME_HISTOGRAM -> timeHistogramDataFactory
            GraphStatType.AVERAGE_TIME_BETWEEN -> averageTimeBetweenDataFactory
            GraphStatType.LAST_VALUE -> lastValueDataFactory
            GraphStatType.BAR_CHART -> barChartDataFactory
            GraphStatType.LUA_SCRIPT -> luaGraphDataFactory
        }
    }

    override fun getDataSourceAdapter(type: GraphStatType): GraphStatDataSourceAdapter<*> {
        return when (type) {
            GraphStatType.LINE_GRAPH -> lineGraphDataSourceAdapter
            GraphStatType.PIE_CHART -> pieChartDataSourceAdapter
            GraphStatType.AVERAGE_TIME_BETWEEN -> averageTimeBetweenDataSourceAdapter
            GraphStatType.TIME_HISTOGRAM -> timeHistogramDataSourceAdapter
            GraphStatType.LAST_VALUE -> lastValueDataSourceAdapter
            GraphStatType.BAR_CHART -> barChartDataSourceAdapter
            GraphStatType.LUA_SCRIPT -> luaGraphDataSourceAdapter
        }
    }
}
