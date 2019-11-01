package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class GraphStatType { LINE_GRAPH, PIE_CHART, AVERAGE_TIME_BETWEEN, TIME_SINCE }

@Entity(tableName = "graphs_and_stats_table",
    foreignKeys = [ForeignKey(entity = GraphStatGroup::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("graph_stat_group_id"),
    onDelete = ForeignKey.CASCADE)]
)
data class GraphOrStat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = -1L,

    @ColumnInfo(name = "graph_stat_group_id", index = true)
    val graphStatGroupId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "graph_stat_type")
    val type: GraphStatType
)