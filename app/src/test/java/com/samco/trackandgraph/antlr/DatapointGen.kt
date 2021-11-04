package com.samco.trackandgraph.antlr

import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.DataPointInterface
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAdjusters
import kotlin.random.Random


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

fun someDataRandom(): List<DataPoint> {
    val now = OffsetDateTime.now()
    val dataPoints = mutableListOf<DataPoint>()
    for (i in 0..25) {
        dataPoints.add(makedp(Random.nextDouble()*100, now.minusHours(100*Random.nextLong(0,7*24))))
    }

    return dataPoints.sortedBy { it.timestamp }
}

fun generateDataPoints2(
    points: List<Triple<DayOfWeek, Int, Double>>
): List<DataPoint> {
    var currentDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)

    val output = mutableListOf<DataPoint>()

    for (pointData in points) {
        val (dayOfWeek, timeInMinutes, value) = pointData

        currentDay = currentDay.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        val timestamp = currentDay + Duration.ofMinutes(timeInMinutes.toLong())

        output.add(DataPoint(timestamp, 0L, value, "", ""))
    }

    return output
}


fun generateDataPoints2Categorical(
    points: List<Triple<DayOfWeek, Int, Int>>,
    val2str: Map<Int, String>
): List<DataPoint> {
    var currentDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)

    val output = mutableListOf<DataPoint>()

    for (pointData in points) {
        val (dayOfWeek, timeInMinutes, value) = pointData

        currentDay = currentDay.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        val timestamp = currentDay + Duration.ofMinutes(timeInMinutes.toLong())

        output.add(DataPoint(timestamp, 0L, value.toDouble(), val2str[value]!!, ""))
    }

    return output
}