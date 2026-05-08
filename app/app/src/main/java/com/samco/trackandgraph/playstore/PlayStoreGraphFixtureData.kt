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
package com.samco.trackandgraph.playstore

import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

internal fun lineGraphViewData(
    id: Long,
    name: String,
    lines: List<PreviewLine>,
    yFrom: Double = 0.0,
    yTo: Double? = null,
): ILineGraphViewData {
    val pointCount = lines.maxOf { it.values.size }
    val xValues = List(pointCount) { index -> (index - (pointCount - 1L)) * MILLIS_PER_WEEK }
    val yMax = lines
        .flatMap { it.values }
        .maxOrNull()
        ?.let { if (it <= 0.0) 1.0 else it * 1.15 }
        ?: 1.0
    val yMaxBound = yTo ?: yMax

    return object : ILineGraphViewData {
        override val state = IGraphStatViewData.State.READY
        override val graphOrStat = graphOrStat(id, name, GraphStatType.LINE_GRAPH)
        override val yRangeType = YRangeType.FIXED
        override val bounds = RectRegion(xValues.first(), xValues.last(), yFrom, yMaxBound)
        override val hasPlottableData = true
        override val endTime = PREVIEW_END_TIME
        override val lines = lines.map { it.toLine(xValues) }
    }
}

internal fun PreviewLine.toLine(xValues: List<Long>): Line {
    val yNumbers = values.map { it as Number }
    val series = object : FastXYSeries {
        internal val minMax = RectRegion(
            xValues.minOrNull(),
            xValues.maxOrNull(),
            values.minOrNull(),
            values.maxOrNull(),
        )

        override fun minMax(): RectRegion = minMax
        override fun getX(index: Int): Number = xValues[index]
        override fun getY(index: Int): Number = yNumbers[index]
        override fun getTitle(): String = name
        override fun size(): Int = values.size
    }

    return Line(
        name = name,
        color = ColorSpec.ColorIndex(colorIndex),
        pointStyle = pointStyle,
        line = series,
    )
}

internal fun graphOrStat(
    id: Long,
    name: String,
    type: GraphStatType,
) = GraphOrStat(
    id = id,
    name = name,
    type = type,
    unique = true,
)

internal data class TrackerSpec(
    val name: String,
    val dataType: DataType,
    val hasDefaultValue: Boolean,
    val defaultLabel: String,
)

internal data class GroupSpec(
    val name: String,
    val colorIndex: Int,
)

internal data class PreviewLine(
    val name: String,
    val colorIndex: Int,
    val pointStyle: LineGraphPointStyle,
    val values: List<Double>,
)

internal data class WavePoint(
    val timestamp: OffsetDateTime,
    val value: Double,
)

internal class PreviewSinTransform(
    internal val amplitude: Double,
    internal val wavelength: Double,
    internal val yOffset: Double = 0.0,
    internal val xOffset: Double = 0.0,
) {
    fun transform(index: Int): Double {
        val xPos = index.toDouble() + xOffset
        val sinTransform = sin((xPos / wavelength) * Math.PI * 2.0)
        return (((sinTransform + 1.0) / 2.0) * amplitude) + yOffset
    }
}

internal fun exerciseWavePoints(): List<WavePoint> = createPreviewWaveData(
    sinTransform = PreviewSinTransform(amplitude = 1.0, wavelength = 5.0, yOffset = -1.0),
    randomOffsetScalar = 1.0,
    roundToInt = true,
    clampMin = 0.0,
    clampMax = 1.0,
)

internal fun illnessWavePoints(): List<WavePoint> = createPreviewWaveData(
    sinTransform = PreviewSinTransform(
        amplitude = 3.0,
        wavelength = 160.0,
        yOffset = -2.0,
        xOffset = 80.0,
    ),
    randomOffsetScalar = 1.0,
    roundToInt = true,
    clampMin = 0.0,
    clampMax = 7.0,
)

internal fun createPreviewWaveData(
    sinTransform: PreviewSinTransform,
    randomSeed: Int = 0,
    randomOffsetScalar: Double = 5.0,
    numDataPoints: Int = 500,
    spacing: Duration = Duration.ofDays(1),
    spacingRandomisationHours: Int = 6,
    endPoint: OffsetDateTime = PREVIEW_END_TIME,
    roundToInt: Boolean = false,
    clampMin: Double? = null,
    clampMax: Double? = null,
): List<WavePoint> {
    val random = Random(randomSeed)

    return List(numDataPoints) { index ->
        val sin = sinTransform.transform(index)
        val randAdjusted = sin + (random.nextDouble() * randomOffsetScalar)
        val rounded = if (roundToInt) randAdjusted.roundToInt().toDouble() else randAdjusted
        val clamped = rounded.coerceIn(clampMin ?: Double.MIN_VALUE, clampMax ?: Double.MAX_VALUE)
        val randDuration = Duration.ofHours(
            random.nextLong(spacingRandomisationHours * 2L) - spacingRandomisationHours
        )

        WavePoint(
            timestamp = endPoint - spacing.multipliedBy(index.toLong()) - randDuration,
            value = clamped,
        )
    }
}

internal fun weeklyTotals(points: List<WavePoint>, numberOfWeeks: Int): List<Double> {
    val totals = DoubleArray(numberOfWeeks)
    points.forEach { point ->
        val weekIndex = Duration.between(point.timestamp, PREVIEW_END_TIME).toDays().toInt() / 7
        if (weekIndex in 0 until numberOfWeeks) {
            totals[weekIndex] += point.value
        }
    }
    return totals.asList().asReversed()
}

internal fun sampledMovingAverage(
    points: List<WavePoint>,
    windowDays: Int,
    numberOfWeeks: Int,
): List<Double> {
    return (numberOfWeeks - 1 downTo 0).map { weekIndex ->
        val sampleTime = PREVIEW_END_TIME - Duration.ofDays(weekIndex * 7L)
        val startTime = sampleTime - Duration.ofDays(windowDays.toLong())
        val window = points.filter { it.timestamp > startTime && it.timestamp <= sampleTime }
        if (window.isEmpty()) 0.0 else window.sumOf { it.value } / window.size
    }
}

internal val PREVIEW_END_TIME: OffsetDateTime =
    OffsetDateTime.of(2026, 5, 8, 12, 0, 0, 0, ZoneOffset.UTC)
internal const val MILLIS_PER_WEEK = 7L * 24L * 60L * 60L * 1000L
