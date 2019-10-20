package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Period

class LineGraphFeature(var featureId: Long, var colorId: Int, var offset: Double, var scale: Double)

@Entity(tableName = "line_graphs_table",
    foreignKeys = [ForeignKey(entity = GraphOrStat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("graph_stat_id"),
        onDelete = ForeignKey.CASCADE)]
)
data class LineGraph(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id")
    val graphStatId: Long,

    @ColumnInfo(name = "features")
    val features: List<LineGraphFeature>,

    @ColumnInfo(name = "period")
    val period: Period?,

    @ColumnInfo(name = "moving_average_period")
    val movingAveragePeriod: Period?
)
