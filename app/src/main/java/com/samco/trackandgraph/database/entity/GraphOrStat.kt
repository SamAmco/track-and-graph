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
package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.database.MAX_GRAPH_STAT_NAME_LENGTH
import com.samco.trackandgraph.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.database.splitChars1
import com.samco.trackandgraph.database.splitChars2
import com.samco.trackandgraph.graphstatinput.configviews.*
import com.samco.trackandgraph.graphstatinput.configviews.AverageTimeBetweenConfigView
import com.samco.trackandgraph.graphstatinput.configviews.LineGraphConfigView
import com.samco.trackandgraph.graphstatinput.configviews.PieChartConfigView
import com.samco.trackandgraph.graphstatinput.configviews.TimeSinceConfigView
import com.samco.trackandgraph.graphstatinput.datasourceadapters.*
import com.samco.trackandgraph.graphstatview.decorators.*
import com.samco.trackandgraph.graphstatview.factories.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import org.threeten.bp.Duration
import kotlin.reflect.KClass

enum class GraphStatType {
    LINE_GRAPH,
    PIE_CHART,
    AVERAGE_TIME_BETWEEN,
    TIME_SINCE,
    TIME_HISTOGRAM
}

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

val maxGraphPeriodDurations = listOf(
    null,
    Duration.ofDays(1),
    Duration.ofDays(7),
    Duration.ofDays(31),
    Duration.ofDays(93),
    Duration.ofDays(183),
    Duration.ofDays(365)
)

@Entity(
    tableName = "graphs_and_stats_table2",
    foreignKeys = [ForeignKey(
        entity = GraphStatGroup::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("graph_stat_group_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class GraphOrStat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_group_id", index = true)
    val graphStatGroupId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "graph_stat_type")
    val type: GraphStatType,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int
) {
    companion object {
        fun create(
            id: Long, graphStatGroupId: Long, name: String,
            type: GraphStatType, displayIndex: Int
        ): GraphOrStat {
            val validName = name.take(MAX_GRAPH_STAT_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            return GraphOrStat(
                id,
                graphStatGroupId,
                validName,
                type,
                displayIndex
            )
        }
    }
}