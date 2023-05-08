package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.BarChart
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

data class BarChart(
    val id: Long,
    val graphStatId: Long,
    val featureId: Long,
    val endDate: OffsetDateTime?,
    val duration: Duration?,
    val barPeriod: BarChartBarPeriod,
    val sumByCount: Boolean
) {
    internal fun toEntity() = BarChart(
        id = id,
        graphStatId = graphStatId,
        featureId = featureId,
        endDate = endDate,
        duration = duration,
        barPeriod = barPeriod,
        sumByCount = sumByCount
    )
}