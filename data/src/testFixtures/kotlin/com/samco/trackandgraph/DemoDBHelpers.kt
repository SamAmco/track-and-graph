package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.model.DataInteractor
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

const val SEC_PER_HOUR = 60.0 * 60.0

class SinTransform(
    val amplitude: Double,
    val wavelength: Double,
    val yOffset: Double = 0.0,
    val xOffset: Double = 0.0
) {
    fun transform(index: Int): Double {
        val xPos = index.toDouble() + xOffset
        val sinTransform = sin((xPos / wavelength) * Math.PI * 2.0)
        return (((sinTransform + 1.0) / 2.0) * amplitude) + yOffset
    }
}

suspend fun createWaveData(
    dataInteractor: DataInteractor,
    trackerId: Long,
    sinTransform: SinTransform = SinTransform(10.0, 3.0),
    randomSeed: Int = 0,
    randomOffsetScalar: Double = 5.0,
    numDataPoints: Int = 500,
    spacing: Duration = Duration.ofDays(1),
    spacingRandomisationHours: Int = 6,
    endPoint: OffsetDateTime = OffsetDateTime.now(),
    roundToInt: Boolean = false,
    clampMin: Double? = null,
    clampMax: Double? = null,
    labels: List<String> = emptyList(),
) {
    val tracker =
        dataInteractor.getTrackerById(trackerId) ?: throw Exception("Tracker not found")
    val featureId = tracker.featureId

    val random = Random(randomSeed)

    for (i in 0 until numDataPoints) {
        val sin = sinTransform.transform(i)
        val randAdjusted = sin + (random.nextDouble() * randomOffsetScalar)
        val rounded = if (roundToInt) randAdjusted.roundToInt().toDouble() else randAdjusted
        val clamped = clamp(
            value = rounded,
            min = clampMin ?: Double.MIN_VALUE,
            max = clampMax ?: Double.MAX_VALUE
        )

        val randDuration = Duration.ofHours(
            random.nextLong(spacingRandomisationHours * 2L) - spacingRandomisationHours
        )

        val time = endPoint - spacing.multipliedBy(i.toLong()) - randDuration

        val labelIndex = labels.getOrNull(clamped.roundToInt()) ?: ""

        val dataPoint = createDataPoint(
            timestamp = time,
            featureId = featureId,
            value = clamped,
            label = labelIndex
        )
        dataInteractor.insertDataPoint(dataPoint)
    }
}

fun clamp(value: Double, min: Double, max: Double): Double {
    if (value < min) {
        return min
    } else if (value > max) {
        return max
    }
    return value
}

fun createGroup(
    name: String = "",
    displayIndex: Int = 0,
    parentGroupId: Long? = null,
    colorIndex: Int = 0,
) = Group(
    id = 0L,
    name = name,
    displayIndex = displayIndex,
    parentGroupId = parentGroupId ?: 0L,
    colorIndex = colorIndex
)

fun createTracker(
    name: String,
    groupId: Long,
    displayIndex: Int = 0,
    description: String = "",
    dataType: DataType = DataType.CONTINUOUS,
    hasDefaultValue: Boolean = false,
    defaultValue: Double = 0.0,
    defaultLabel: String = "",
    suggestionType: TrackerSuggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
    suggestionOrder: TrackerSuggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
) = Tracker(
    id = 0L,
    name = name,
    groupId = groupId,
    featureId = 0L,
    displayIndex = displayIndex,
    description = description,
    dataType = dataType,
    hasDefaultValue = hasDefaultValue,
    defaultValue = defaultValue,
    defaultLabel = defaultLabel,
    suggestionType = suggestionType,
    suggestionOrder = suggestionOrder
)

fun createDataPoint(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    featureId: Long,
    value: Double,
    label: String = "",
    note: String = ""
) = DataPoint(
    timestamp = timestamp,
    featureId = featureId,
    value = value,
    label = label,
    note = note
)
