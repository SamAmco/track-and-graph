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

package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.R
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount

enum class TimeHistogramWindow(
    val duration: Duration,
    val period: TemporalAmount,
    val numBins: Int,
    val subTitleId: Int
) {
    HOUR(Duration.ofHours(1), Duration.ofHours(1), 60, R.string.minutes),
    DAY(Duration.ofDays(1), Duration.ofDays(1), 24, R.string.hours),
    WEEK(Duration.ofDays(7), Period.ofWeeks(1), 7, R.string.days),
    MONTH(Duration.ofDays(30), Period.ofMonths(1), 30, R.string.days),
    THREE_MONTHS(Duration.ofDays(365 / 4), Period.ofMonths(3), 13, R.string.weeks),
    SIX_MONTHS(Duration.ofDays(365 / 2), Period.ofMonths(6), 26, R.string.weeks),
    YEAR(Duration.ofDays(365), Period.ofYears(1), 12, R.string.months)
}

@Entity(
    tableName = "time_histograms_table",
    foreignKeys = [
        ForeignKey(
            entity = GraphOrStat::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("graph_stat_id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimeHistogram(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "duration")
    val duration: Duration?,

    @ColumnInfo(name = "window")
    val window: TimeHistogramWindow,

    @ColumnInfo(name = "sum_by_count")
    val sumByCount: Boolean,

    @ColumnInfo(name = "end_date")
    val endDate: OffsetDateTime?
)