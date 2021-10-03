package com.samco.trackandgraph.antlr

import com.samco.trackandgraph.database.entity.DataPoint
import org.threeten.bp.OffsetDateTime


private fun makedp(value: Double, timestamp: OffsetDateTime): DataPoint {
    return DataPoint(
        timestamp,
        0L,
        value,
        "",
        ""
    )
}

fun someData(): List<DataPoint> {
    val now = OffsetDateTime.now()
    val dataPoints = listOf(
        5.0 to 70L,
        0.0 to 50L,
        4.0 to 49L,
        2.0 to 48L,
        0.0 to 43L,
        4.0 to 41L,
        8.0 to 30L,
        7.0 to 20L,
        3.0 to 10L
    ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }

    return dataPoints
}

fun someDataAllTen(): List<DataPoint> {
    val now = OffsetDateTime.now()
    val dataPoints = listOf(
        10.0 to 70L,
        10.0 to 50L,
        10.0 to 49L,
        10.0 to 48L,
        10.0 to 43L,
        10.0 to 41L,
        10.0 to 30L,
        10.0 to 20L,
        10.0 to 10L
    ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }

    return dataPoints
}