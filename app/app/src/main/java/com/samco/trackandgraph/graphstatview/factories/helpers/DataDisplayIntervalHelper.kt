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

package com.samco.trackandgraph.graphstatview.factories.helpers

import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.round

class DataDisplayIntervalHelper @Inject constructor() {

    /**
     * Code to calculate nice y-intervals
     */
    // There are two possible interval classes. The reason is that we want to make a large amount of
    // objects of the first class, which then will get filtered. Then we only have to do the more complex
    // calculations for the actually reasonable intervals
    private data class PossibleIntervalProto(
        val interval: Double,
        val preferred: Boolean,
        val base: Double
    )

    data class PossibleInterval(
        val interval: Double,
        val preferred: Boolean,
        val nLines: Int,
        val percentageRangeUsed: Double,
        val boundsMin: Double,
        val boundsMax: Double,
        val base: Double
    )

    data class YAxisParameters(
        val subdivides: Int,
        val boundsMin: Double,
        val boundsMax: Double
    )

    private fun finishProto(
        proto: PossibleIntervalProto,
        yMin: Double,
        yMax: Double,
        fixedBounds: Boolean
    ): PossibleInterval? {
        return when (fixedBounds) {
            true -> finishProtoFixedBounds(proto, yMin, yMax)
            false -> finishProtoDynamicBounds(proto, yMin, yMax)
        }
    }

    private fun finishProtoDynamicBounds(
        proto: PossibleIntervalProto,
        yMin: Double,
        yMax: Double
    ): PossibleInterval {
        val yRange = yMax - yMin
        var boundsMin = floor(yMin / proto.interval) * proto.interval
        var boundsMax = ceil(yMax / proto.interval) * proto.interval

        // check whether there is a large gap between bounds and data and if its larger
        // than half the base on both sides, remove it
        // it can only be this large if the divisor is 1
        val offset = proto.base / 2
        if (yMin - boundsMin >= offset && boundsMax - yMax >= offset) {
            boundsMin += offset
            boundsMax -= offset
        }

        val boundsRange = boundsMax - boundsMin
        val nLines = round(boundsRange / proto.interval).toInt() + 1
        val percentageRangeUsed = yRange / boundsRange

        return PossibleInterval(
            interval = proto.interval,
            preferred = proto.preferred,
            nLines = nLines,
            percentageRangeUsed = percentageRangeUsed,
            boundsMin = boundsMin,
            boundsMax = boundsMax,
            base = proto.base
        )
    }

    private fun finishProtoFixedBounds(
        proto: PossibleIntervalProto,
        yMin: Double,
        yMax: Double
    ): PossibleInterval? {
        val yRange = yMax - yMin
        // if we can't evenly divide the y_range we can't do anything, so return a bad dummy interval, which will get filtered out
        // this is a little more complex than it should be because of floating point errors
        if (yRange.div(proto.interval).rem(1) != 0.0) return null

        return PossibleInterval(
            interval = proto.interval,
            preferred = proto.preferred,
            nLines = round(yRange / proto.interval).toInt() + 1,
            percentageRangeUsed = 1.0,
            boundsMin = yMin,
            boundsMax = yMax,
            base = proto.base
        )
    }

    fun getYParameters(
        yMin: Double,
        yMax: Double,
        isDurationBasedRange: Boolean,
        fixedBounds: Boolean,
    ): YAxisParameters {
        val parameters = getYParametersInternal(yMin, yMax, isDurationBasedRange, fixedBounds)
        if (parameters != null) {
            return YAxisParameters(
                subdivides = parameters.nLines,
                boundsMin = parameters.boundsMin,
                boundsMax = parameters.boundsMax
            )
        }

        // fallback if we don't find any solution. Gets used when all our solutions use to little of the range.
        // this was the default behavior before this new algorithm existed
        return YAxisParameters(
            subdivides = 11,
            boundsMin = yMin,
            boundsMax = yMax
        )
    }

    companion object {
        private const val MIN_INTERVALS = 6
        private const val MAX_INTERVALS = 12
        private val MIN_USED_RANGE_STEPS = listOf(0.99999, 0.9, 0.8, 0.7)
    }

    @VisibleForTesting
    fun getYParametersInternal(
        yMin: Double,
        yMax: Double,
        timeData: Boolean,
        fixedBounds: Boolean
    ): PossibleInterval? {
        val yRange = yMax - yMin

        val (base, preferredDivisors, allDivisors) = when (timeData) {
            false -> Triple(
                10.0,
                setOf(1, 2, 4, 5),
                setOf(1, 2, 3, 4, 5, 8)
            )

            true -> Triple(
                60.0,
                setOf(1, 2, 3, 4, 6, 12, 24),
                setOf(1, 2, 3, 4, 6, 12, 24, 30)
            )
        }

        // We want to treat a y_range of 60 the same way as a range of 600 or 6, just times 10 or 0.1
        val normExponent = round(log(yRange, base))
        val normedBase = base.pow(normExponent)

        val reasonableIntervals = allDivisors
            .flatMap { div ->
                (-1..1).map { expOffset ->
                    PossibleIntervalProto(
                        interval = normedBase * base.pow(expOffset.toDouble()) / div,
                        preferred = div in preferredDivisors,
                        base = normedBase * base.pow(expOffset.toDouble())
                    )
                }
            }
            // so after we generate a lot of possible interval prototypes, in this first filter we get rid of all the
            // ones who just are not plausible at all. We'll do a second check with the exact data further below
            .filter { interval ->
                ceil(yRange / interval.interval) >= MIN_INTERVALS - 1 &&
                        ceil(yRange / interval.interval) <= MAX_INTERVALS + 1
            }
            .mapNotNull { proto -> finishProto(proto, yMin, yMax, fixedBounds = fixedBounds) }
            .filter { interval -> interval.nLines in MIN_INTERVALS..MAX_INTERVALS }

        if (reasonableIntervals.isEmpty()) return null

        for (minUsedRange in MIN_USED_RANGE_STEPS) {
            // gradually have lower expectations in our output

            for (pref in listOf(true, false)) {
                // prefer the 'preferred' intervals when it comes to each used_range step
                reasonableIntervals
                    .filter { it.preferred == pref }
                    .filter { it.percentageRangeUsed >= minUsedRange }
                    .sortedByDescending { it.interval }   // prefer larger intervals
                    .forEach { return it }  // returns the first element, if there is one
            }
        }

        return null
    }
}