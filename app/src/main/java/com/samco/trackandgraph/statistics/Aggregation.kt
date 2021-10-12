package com.samco.trackandgraph.statistics

import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.database.entity.DataPointInterface
import kotlinx.coroutines.yield
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.math.ceil


/**
 * The default return type of the main aggregation functions. This class represents the initial
 * points clustered into the parent-attribute of the AggregatedDataPoints.
 * To obtain usable values, one of the follow-up functions like sum, average, or max need to be called.
 * Note that some follow-up functions drop data-points without parents. This is supposed to be intuitive :)
 */
class RawAggregatedDatapoints(private val points: List<AggregatedDataPoint>) {
    fun sum() = DataSample(
        this.points.map { it.copy(value = it.parents.sumOf { par -> par.value }) }
    )


    // When using duration based aggregated values, some points can have no parents.
    // The most intuitive solution is probably to just remove these aggregated points
    // There might be cases where it can make sense to define a fallback value, e.g. 0
    fun max() = DataSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.maxOf { par -> par.value }) }
    )

    fun average() = DataSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.map { par -> par.value }.average()) }
    )
}


internal fun getFirstDayOfWeekFromLocale(): DayOfWeek =
    WeekFields.of(Locale.getDefault()).firstDayOfWeek

internal data class AggregationWindowPreferences(
    val firstDayOfWeek: DayOfWeek,  // Weekly windows will start with that day
    val startTimeOfDay: Duration    // Datapoints before this time will be aggregated as if they belong to the previous day
) {
    constructor() : this(getFirstDayOfWeekFromLocale(), Duration.ofSeconds(0))
    constructor(firstDayOfWeek: DayOfWeek) : this(firstDayOfWeek, Duration.ofSeconds(0))
    constructor(startTimeOfDay: Duration) : this(getFirstDayOfWeekFromLocale(), startTimeOfDay)
}

internal fun DataPointInterface.cutoffTimestampForAggregation(startTimeOfDayOrNull: Duration?): OffsetDateTime {
    val startTimeOfDay = startTimeOfDayOrNull ?: Duration.ZERO
    return timestamp - startTimeOfDay
}


/**
 * Add up all data points per plotTotalTime. For example if the plot total time is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample containing 1 point with the value 11.
 *
 * The currently supported plotTotalTime values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * If sampleDuration is provided then totals will be generated at least as far back as now minus the
 * sampleDuration. However if there is more data before this period then it that data will also be
 * totalled. For clipping see clipDataSample.
 *
 * If end time is provided then totals will be generated at least up to this time. However if there
 * is more data after the end time in the input sample then that data will also be totalled. For
 * clipping see clipDataSample.
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */
internal suspend fun calculateDurationAccumulatedValues(
    sampleData: DataSample,
    featureId: Long,
    sampleDuration: Duration?,
    endTime: OffsetDateTime?,
    plotTotalTime: TemporalAmount,
    aggPreferences: AggregationWindowPreferences? = null
): DataSample {
    return fixedBinAggregation(
        sampleData,
        featureId,
        sampleDuration,
        endTime,
        plotTotalTime,
        aggPreferences
    ).sum()
}

/**
 * Aggregate all data points into binSize length bins. For example if the bin size is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample with all three data-points as its parent.
 *
 * The currently supported binSize values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * If sampleDuration is provided then totals will be generated at least as far back as now minus the
 * sampleDuration. However if there is more data before this period then it that data will also be
 * totalled. For clipping see clipDataSample.
 *
 * If end time is provided then totals will be generated at least up to this time. However if there
 * is more data after the end time in the input sample then that data will also be totalled. For
 * clipping see clipDataSample.
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */
private suspend fun fixedBinAggregation(
    sampleData: DataSample,
    featureId: Long,
    sampleDuration: Duration?,
    endTime: OffsetDateTime?,
    binSize: TemporalAmount,
    aggPreferencesOrNull: AggregationWindowPreferences? = null,
): RawAggregatedDatapoints {
    val aggPref = aggPreferencesOrNull ?: AggregationWindowPreferences()

    val newData = mutableListOf<AggregatedDataPoint>()

    val firstDataPointTime =
        sampleData.dataPoints.firstOrNull()?.cutoffTimestampForAggregation(aggPref.startTimeOfDay)
    val lastDataPointTime =
        sampleData.dataPoints.lastOrNull()?.cutoffTimestampForAggregation(aggPref.startTimeOfDay)

    val latest = getEndTimeNowOrLatest(lastDataPointTime, endTime)
    val earliest = getStartTimeOrFirst(firstDataPointTime, latest, endTime, sampleDuration)
    var currentTimeStamp =
        findBeginningOfTemporal(earliest, binSize, aggPref.firstDayOfWeek).minusNanos(1)
    var index = 0
    while (currentTimeStamp.isBefore(latest)) {
        currentTimeStamp = currentTimeStamp.with { ld -> ld.plus(binSize) }
        val points = sampleData.dataPoints.drop(index)
            .takeWhile { dp ->
                dp.cutoffTimestampForAggregation(aggPref.startTimeOfDay)
                    .isBefore(currentTimeStamp)
            }
        index += points.size
        newData.add(
            AggregatedDataPoint(
                timestamp = currentTimeStamp,
                value = Double.NaN, // this value gets overwritten when calling a function on RawAggregatedDatapoints
                featureId = featureId,
                parents = points
            )
        )
        yield()
    }
    return RawAggregatedDatapoints(newData)
}

/**
 * Calculate the moving averages of all of the data points given over the moving average duration given.
 * A new DataSample will be returned with one data point for every data point in the input set whose
 * timestamp shall be the same but value will be equal to the average of it and all previous data points
 * within the movingAvDuration.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal suspend fun calculateMovingAverages(
    dataSample: DataSample,
    movingAvgDuration: Duration
): DataSample {
    return movingAggregation(dataSample, movingAvgDuration).average()
}

/**
 * Calculate the moving aggregation-parents of all of the data points given over the moving duration given.
 * RawAggregatedDatapoints will be returned containing one data point for every data point in the input set.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal suspend fun movingAggregation(
    dataSample: DataSample,
    movingAggDuration: Duration
): RawAggregatedDatapoints {
    val movingAggregationPointsRaw = mutableListOf<AggregatedDataPoint>()
    val dataPointsReversed = dataSample.dataPoints.reversed()

    for ( (index, current) in dataPointsReversed.mapIndexed { idx, point -> Pair(idx, point) }) {
        yield()
        val parents = dataPointsReversed.drop(index)
            .takeWhile { dp ->
                Duration.between(dp.timestamp, current.timestamp) < movingAggDuration
            }

        movingAggregationPointsRaw.add(
            0,
            AggregatedDataPoint(
                current.timestamp,
                current.featureId,
                value = Double.NaN,
                label = current.label,
                note = current.note,
                parents = parents
            )
        )
    }

    return RawAggregatedDatapoints(movingAggregationPointsRaw)
}


/// TIMING FUNCTIONS


private fun getStartTimeOrFirst(
    firstDataPointTime: OffsetDateTime?,
    latest: OffsetDateTime,
    endTime: OffsetDateTime?,
    sampleDuration: Duration?
): OffsetDateTime {
    val beginningOfDuration = sampleDuration?.let { endTime?.minus(it) }
    val durationBeforeLatest = sampleDuration?.let { latest.minus(it) }
    return listOf(
        firstDataPointTime,
        beginningOfDuration,
        durationBeforeLatest,
        latest
    ).minBy { t -> t ?: OffsetDateTime.MAX }!!
}

private fun getEndTimeNowOrLatest(
    lastDataPointTime: OffsetDateTime?,
    endTime: OffsetDateTime?
): OffsetDateTime {
    val now = OffsetDateTime.now()
    return when {
        //last == null && endTime == null -> now
        endTime == null -> listOf(lastDataPointTime, now).maxBy { t -> t ?: OffsetDateTime.MIN }!!
        else -> listOf(lastDataPointTime, endTime).maxBy { t -> t ?: OffsetDateTime.MIN }!!
    }
}

/**
 * Finds the first ending of temporalAmount before dateTime. For example if temporalAmount is a
 * week then it will find the very end of the sunday before dateTime.
 *
 * temporalAmount supports the use of duration instead of Period.
 * In this case the function will try to approximate the behaviour as closely as possible as though
 * temporalAmount was a period. For example if you pass in a duration of 7 days it will try to find
 * the last day of the week before the week containing dateTime. It is always the largest recognised
 * duration that is used when deciding what the start of the period should be. The recognised durations
 * are:
 *
 *  Duration.ofHours(1)
 *  Duration.ofHours(24)
 *  Duration.ofDays(7)
 *  Duration.ofDays(30)
 *  Duration.ofDays(365 / 4) or 3 months
 *  Duration.ofDays(365 / 2) or 6 months
 *  Duration.ofDays(365) or a year
 *
 */


internal fun findBeginningOfTemporal(
    dateTime: OffsetDateTime,
    temporalAmount: TemporalAmount,
    firstDayOfWeekOrNull: DayOfWeek? = null
): OffsetDateTime {
    // if no firstDayOfWeek is given, use the default one from default constructor
    val firstDayOfWeek = firstDayOfWeekOrNull ?: AggregationWindowPreferences().firstDayOfWeek
    return when (temporalAmount) {
        is Duration -> findBeginningOfDuration(dateTime, temporalAmount, firstDayOfWeek)
        is Period -> findBeginningOfPeriod(dateTime, temporalAmount, firstDayOfWeek)
        else -> dateTime
    }
}

private fun findBeginningOfDuration(
    dateTime: OffsetDateTime,
    duration: Duration,
    firstDayOfWeek: DayOfWeek
): OffsetDateTime {
    val dtHour = dateTime
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    if (duration <= Duration.ofMinutes(60)) return dtHour

    val dt = dtHour.withHour(0)

    return when {
        duration <= Duration.ofDays(1) -> dt
        duration <= Duration.ofDays(7) -> {
            dt.with(
                TemporalAdjusters.previousOrSame(
                    firstDayOfWeek
                )
            )
        }
        duration <= Duration.ofDays(31) -> dt.withDayOfMonth(1)
        duration <= Duration.ofDays(ceil(365.0 / 4).toLong()) -> {
            val month = getQuaterForMonthValue(dt.monthValue)
            dt.withMonth(month).withDayOfMonth(1)
        }
        duration <= Duration.ofDays(ceil(365.0 / 2).toLong()) -> {
            val month = getBiYearForMonthValue(dt.monthValue)
            dt.withMonth(month).withDayOfMonth(1).withHour(0)
        }
        else -> dt.withDayOfYear(1)
    }
}

/**
 * Given a number representing a month in the range 1 to 12, this function will return you the
 * integer value of the month starting the quater of the year containing that month.
 */
internal fun getQuaterForMonthValue(monthValue: Int) = (3 * ((monthValue - 1) / 3)) + 1

/**
 * Given a number representing a month in the range 1 to 12, this function will return you the
 * integer value of the month starting the bi year containing that month.
 */
internal fun getBiYearForMonthValue(monthValue: Int) = if (monthValue < 7) 1 else 7
private fun findBeginningOfPeriod(
    dateTime: OffsetDateTime,
    period: Period,
    firstDayOfWeek: DayOfWeek
): OffsetDateTime {
    val dt = dateTime.withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    return when {
        isPeriodNegativeOrZero(period.minus(Period.ofDays(1))) -> dt
        isPeriodNegativeOrZero(period.minus(Period.ofWeeks(1))) -> {
            dt.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(1))) -> {
            dt.withDayOfMonth(1)
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(3))) -> {
            val month = getQuaterForMonthValue(dateTime.monthValue)
            dt.withMonth(month).withDayOfMonth(1)
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(6))) -> {
            val month = getBiYearForMonthValue(dateTime.monthValue)
            dt.withMonth(month).withDayOfMonth(1)
        }
        else -> dt.withDayOfYear(1)
    }
}

private fun isPeriodNegativeOrZero(period: Period) = period.years < 0
        || (period.years == 0 && period.months < 0)
        || (period.years == 0 && period.months == 0 && period.days <= 0)
