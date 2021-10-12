package com.samco.trackandgraph.statistics

import com.androidplot.xy.StepMode
import java.lang.Exception
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.round

/**
 * Code to calculate nice y-intervals
 */
// There are two possible interval classes. The reason is that we want to make a large amount of
// objects of the first class, which then will get filtered. Then we only have to do the more complex
// calculations for the actually reasonable intervals
data class PossibleIntervalProto(
    val interval: Double, val preferred: Boolean,
    val base: Double
)

data class PossibleInterval(
    val interval: Double, val preferred: Boolean,
    val n_lines: Int, val percentage_range_used: Double,
    val bounds_min: Double, val bounds_max: Double,
    val base: Double
)

data class YAxisParameters(
    val step_mode: StepMode, val n_intervals: Double,
    val bounds_min: Double, val bounds_max: Double,
)

private fun finishProto(
    proto: PossibleIntervalProto,
    y_min: Double,
    y_max: Double,
    fixedBounds: Boolean
): PossibleInterval? {
    return when (fixedBounds) {
        true -> finishProtoFixedBounds(proto, y_min, y_max)
        false -> finishProtoDynamicBounds(proto, y_min, y_max)
    }
}

private fun finishProtoDynamicBounds(
    proto: PossibleIntervalProto,
    y_min: Double,
    y_max: Double
): PossibleInterval {
    val y_range = y_max - y_min
    var bounds_min = floor(y_min / proto.interval) * proto.interval
    var bounds_max = ceil(y_max / proto.interval) * proto.interval

    // check whether there is a large gap between bounds and data and if its larger
    // than half the base on both sides, remove it
    // it can only be this large if the divisor is 1
    val offset = proto.base / 2
    if (y_min - bounds_min >= offset && bounds_max - y_max >= offset) {
        bounds_min += offset
        bounds_max -= offset
    }

    val bounds_range = bounds_max - bounds_min
    val n_lines = round(bounds_range / proto.interval).toInt() + 1
    val percentage_range_used = y_range / bounds_range

    return PossibleInterval(
        proto.interval, proto.preferred,
        n_lines, percentage_range_used,
        bounds_min, bounds_max,
        proto.base
    )
}

private fun finishProtoFixedBounds(
    proto: PossibleIntervalProto,
    y_min: Double,
    y_max: Double
): PossibleInterval? {
    val y_range = y_max - y_min
    // if we can't evenly divide the y_range we can't do anything, so return a bad dummy interval, which will get filtered out
    // this is a little more complex than it should be because of floating point errors
    if (y_range.div(proto.interval)
            .rem(1) != 0.0
    ) return null //PossibleInterval(0.0, false, 999, 0.0, 0.0, 0.0, 1.0)

    return PossibleInterval(
        proto.interval, proto.preferred, round(y_range / proto.interval).toInt() + 1,
        1.0, y_min, y_max, proto.base
    )
}

fun getYParameters(
    y_min: Double, y_max: Double, time_data: Boolean,
    fixedBounds: Boolean,
    throw_exc_if_non_found: Boolean = false
): YAxisParameters {


    val parameters =
        getYParametersInternal(y_min, y_max, time_data, fixedBounds, throw_exc_if_non_found)
    if (parameters != null) {
        return YAxisParameters(
            StepMode.SUBDIVIDE,
            parameters.n_lines.toDouble(),
            parameters.bounds_min,
            parameters.bounds_max
        )
    }

    // fallback if we don't find any solution. Gets used when all our solutions use to little of the range.
    // this was the default behavior before this new algorithm existed
    val fallback = YAxisParameters(StepMode.SUBDIVIDE, 11.0, y_min, y_max)
    return fallback

}

fun getYParametersInternal(
    y_min: Double, y_max: Double, time_data: Boolean,
    fixedBounds: Boolean, throw_exc_if_non_found: Boolean
): PossibleInterval? {
    val MIN_INTERVALS = 6
    val MAX_INTERVALS = 12
    val MIN_USED_RANGE_STEPS = listOf<Double>(0.849, 0.79)//, 0.749, 0.7)

    val y_range = y_max - y_min

    val (base, preferred_divisors, all_divisors) = when (time_data) {
        false -> Triple(
            10, setOf(1, 2, 4, 5),
            setOf(1, 2, 3, 4, 5, 8)
        )
        true -> Triple(
            60, setOf(1, 2, 3, 4, 6, 12, 24),
            setOf(1, 2, 3, 4, 6, 12, 24, 30)
        )
    }

    // We want to treat a y_range of 60 the same way as a range of 600 or 6, just times 10 or 0.1
    val norm_exponent = round(log(y_range, base.toDouble()))
    val normed_base = Math.pow(base.toDouble(), norm_exponent.toDouble())

    val reasonable_intervals = all_divisors
        .flatMap { div ->
            (-1..1).map { exp_offset ->
                PossibleIntervalProto(
                    normed_base * Math.pow(base.toDouble(), exp_offset.toDouble()) / div,
                    div in preferred_divisors,
                    base = normed_base * Math.pow(base.toDouble(), exp_offset.toDouble())
                )
            }
        }
        // so after we generate a lot of possible interval prototypes, in this first filter we get rid of all the
        // ones who just are not plausible at all. We'll do a second check with the exact data further below
        .filter { interval ->
            ceil(y_range / interval.interval) >= MIN_INTERVALS - 1 &&
                    ceil(y_range / interval.interval) <= MAX_INTERVALS + 1
        }
        .map { proto -> finishProto(proto, y_min, y_max, fixedBounds = fixedBounds) }
        .filterNotNull()
        .filter { interval ->
            interval.n_lines >= MIN_INTERVALS &&
                    interval.n_lines <= MAX_INTERVALS
        }

    if (reasonable_intervals.isEmpty()) {
        if (throw_exc_if_non_found) throw Exception("No solution found! No intervals passed the initial filtering. ymin: $y_min, ymax: $y_max")
        return null

    }

    for (MIN_USED_RANGE in MIN_USED_RANGE_STEPS) {
        // gradually have lower expectations in our output

        for (pref in listOf(true, false)) {
            // prefer the 'preferred' intervals when it comes to each used_range step
            reasonable_intervals
                .filter { it.preferred == pref }
                .filter { it.percentage_range_used >= MIN_USED_RANGE }
                .sortedByDescending { it.interval }   // prefer larger intervals
                .forEach { return it }  // returns the first element, if there is one
        }
    }

    if (throw_exc_if_non_found) throw Exception("No solution found! No intervals passed the range_used check. ymin: $y_min, ymax: $y_max")
    return null

}