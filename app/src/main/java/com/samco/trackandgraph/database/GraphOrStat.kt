package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Duration

enum class GraphStatType { LINE_GRAPH, PIE_CHART, AVERAGE_TIME_BETWEEN, TIME_SINCE }

val maxGraphPeriodDurations = listOf(
    null,
    Duration.ofDays(1),
    Duration.ofDays(7),
    Duration.ofDays(31),
    Duration.ofDays(93),
    Duration.ofDays(183),
    Duration.ofDays(365)
)

@Entity(tableName = "graphs_and_stats_table",
    foreignKeys = [ForeignKey(entity = GraphStatGroup::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("graph_stat_group_id"),
    onDelete = ForeignKey.CASCADE)]
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
        fun create(id: Long, graphStatGroupId: Long, name: String,
                   type: GraphStatType, displayIndex: Int): GraphOrStat {
            val validName = name.take(MAX_GRAPH_STAT_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            return GraphOrStat(id, graphStatGroupId, validName, type, displayIndex)
        }
    }
}