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

package com.samco.trackandgraph.graphstatconstants

import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.graphstatinput.configviews.*
import com.samco.trackandgraph.graphstatinput.configviews.AverageTimeBetweenConfigView
import com.samco.trackandgraph.graphstatinput.configviews.LineGraphConfigView
import com.samco.trackandgraph.graphstatinput.configviews.PieChartConfigView
import com.samco.trackandgraph.graphstatinput.configviews.TimeSinceConfigView
import com.samco.trackandgraph.graphstatinput.datasourceadapters.*
import com.samco.trackandgraph.graphstatview.decorators.*
import com.samco.trackandgraph.graphstatview.factories.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import kotlin.reflect.KClass

data class GraphStatTypeConfig<
        J : GraphStatConfigView,
        L : Any,
        T : IGraphStatViewData,
        K : GraphStatViewDecorator<T>>(
    val dataSourceAdapter: GraphStatDataSourceAdapter<L>,
    val configViewClass: KClass<J>,
    val configClass: KClass<L>,//TODO Would be real nice to have some type safety on this
    val dataFactory: ViewDataFactory<L, T>,
    val viewDataClass: KClass<T>,
    val decoratorClass: KClass<K>
)

val graphStatTypes = mapOf<GraphStatType, GraphStatTypeConfig<*, *, *, *>>(
    GraphStatType.LINE_GRAPH to GraphStatTypeConfig(
        LineGraphDataSourceAdapter(),
        LineGraphConfigView::class,
        LineGraphWithFeatures::class,
        LineGraphDataFactory(),
        ILineGraphViewData::class,
        GraphStatLineGraphDecorator::class
    ),
    GraphStatType.PIE_CHART to GraphStatTypeConfig(
        PieChartDataSourceAdapter(),
        PieChartConfigView::class,
        PieChart::class,
        PieChartDataFactory(),
        IPieChartViewData::class,
        GraphStatPieChartDecorator::class
    ),
    GraphStatType.AVERAGE_TIME_BETWEEN to GraphStatTypeConfig(
        AverageTimeBetweenDataSourceAdapter(),
        AverageTimeBetweenConfigView::class,
        AverageTimeBetweenStat::class,
        AverageTimeBetweenDataFactory(),
        IAverageTimeBetweenViewData::class,
        GraphStatAverageTimeBetweenDecorator::class
    ),
    GraphStatType.TIME_SINCE to GraphStatTypeConfig(
        TimeSinceDataSourceAdapter(),
        TimeSinceConfigView::class,
        TimeSinceLastStat::class,
        TimeSinceViewDataFactory(),
        ITimeSinceViewData::class,
        GraphStatTimeSinceDecorator::class
    ),
    GraphStatType.TIME_HISTOGRAM to GraphStatTypeConfig(
        TimeHistogramDataSourceAdapter(),
        TimeHistogramConfigView::class,
        TimeHistogram::class,
        TimeHistogramDataFactory(),
        ITimeHistogramViewData::class,
        GraphStatTimeHistogramDecorator::class
    )
)

