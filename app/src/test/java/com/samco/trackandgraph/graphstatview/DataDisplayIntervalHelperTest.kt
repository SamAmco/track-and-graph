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

package com.samco.trackandgraph.graphstatview

import com.samco.trackandgraph.graphstatview.factories.DataDisplayIntervalHelper
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class DataDisplayIntervalHelperTest {

    private val uut = DataDisplayIntervalHelper()

    private fun getYParametersInternalWithFallback(
        y_min: Double,
        y_max: Double,
        time_data: Boolean,
        fixedBounds: Boolean,
    ): DataDisplayIntervalHelper.PossibleInterval {
        val parameters =
            uut.getYParametersInternal(y_min, y_max, time_data, fixedBounds)
        if (parameters != null) {
            return parameters
        }
        return DataDisplayIntervalHelper.PossibleInterval(
            (y_max - y_min) / 10.0, false, 11, 1.0,
            y_min, y_max, 1.0
        )
    }


    // these first four ones aren't really tests, but declaring them as tests makes it easier to run them

    @Test
    fun manuallyTestValuesNumerical() {
        runBlocking {
            val y_min = 39.2
            val y_max = 309.2

            printExampleNumerical(y_min, y_max)
        }
    }

    @Test
    @ExperimentalTime
    fun manuallyTestValuesTime() {
        runBlocking {
            val y_min = 1 * 60 * 60 + 53 * 60 // in seconds
            val y_max = 8 * 60 * 60 + 25 * 60

            printExampleTime(y_min.toDouble(), y_max.toDouble())
        }
    }

    @Test
    @ExperimentalTime
    fun printExamplesTime() {
        val RANGE_USED_BELOW = 0.82
        for (q_ in 0 until 32) {

            for (qq_ in 0 until 200) {
                val start = Random.nextInt(0, 3 * 60 * 60).toDouble()
                val length = Random.nextInt(1, 10 * 60 * 60).toDouble()
                val end = start + length

                val interval = getYParametersInternalWithFallback(
                    y_min = start,
                    y_max = end,
                    time_data = true,
                    fixedBounds = false
                )

                if (interval.percentage_range_used > RANGE_USED_BELOW) continue

                printExampleTime(start, end)
                break
            }
        }
    }

    @Test
    fun printExamplesNumerical() {
        val RANGE_USED_BELOW = 0.82
        for (q_ in 0 until 32) {

            for (qq_ in 0 until 200) {
                // generate new numbers until you get in the range_below regime. 201 tries should be enough.
                val start = Random.nextInt(0, 1000).toDouble() / 10
                val length = Random.nextInt(1, 500).toDouble() / 10
                val end = start + length

                val interval = uut.getYParameters(
                    y_min = start,
                    y_max = end,
                    time_data = false,
                    fixedBounds = false
                )

                val range_used = length / (interval.bounds_max - interval.bounds_min)
                if (range_used > RANGE_USED_BELOW) continue

                printExampleNumerical(start, end)
                break
            }

        }
    }

    fun find_solution_for_everything(time_data: Boolean, long_test: Boolean) {
        var errors = 0
        val allowed_error_percentage = when (time_data) {
            false -> 0.1 // it's ok if one   in 1000 doesn't have a good solution
            true -> 0.7 // it's ok if seven in 1000 don't   have a good solution
        }
        val no_solution_vals = mutableListOf<Pair<Double, Double>>()
        val intervalList = mutableListOf<DataDisplayIntervalHelper.PossibleInterval>()

        val startValues = when (time_data) {
            false -> (0..10000).map { it.toDouble() / 10 }
            true -> (0..36000).map { it.toDouble() } // zero seconds to 10 hours
        }
        val lengthValues = when (time_data) {
            false -> (1..7500).map { it.toDouble() / 10 }
            true -> (1..72000).map { it.toDouble() } // one second to 20 hours
        }

        val (numberOfStartValues, numberOfLengthValues) = when (long_test) {
            true -> Pair(10000, 500)
            false -> Pair(1000, 50)
        }

        for (start in startValues.shuffled().slice(0..numberOfStartValues)) {
            for (length in lengthValues.shuffled().slice(0..numberOfLengthValues)) {
                val interval = uut.getYParametersInternal(
                    yMin = start,
                    yMax = start + length,
                    timeData = time_data,
                    fixedBounds = false
                )

                if (interval != null) {
                    val range_used = length / (interval.bounds_max - interval.bounds_min)
                    assert(range_used - interval.percentage_range_used < 0.001)
                    intervalList.add(interval)
                } else {
                    no_solution_vals.add(Pair(start, start + length))
                    //throw Exception("min: $start, max: ${start + length}")
                    errors += 1
                }

            }
        }
        val range_used_list = intervalList.map { it.percentage_range_used }
        val nRuns = range_used_list.count()

        println("Tested $nRuns combinations.")

        print("minimum range used: ${range_used_list.minOrNull()}\n\n")

        for (i in 0..9) {
            val top = 1 - i * 0.03
            val bot = 1 - (i + 1) * 0.03
            val nInRange = range_used_list.filter { top >= it && it > bot }.count()
            println(
                "%.2f".format(top) + " -> " + "%.2f".format(bot) +
                        ":" + "%.2f".format(100 * nInRange.toDouble() / nRuns.toDouble())
                    .padStart(6, ' ') + "%"
            )

        }
        println("Didn't find a good solution for ${100 * errors.toDouble() / nRuns.toDouble()}%")
        //println("$no_solution_vals")

        println("How many lines are drawn how often:")
        intervalList
            .map { it.n_lines }.groupingBy { it }
            .eachCount().mapValues { 100 * it.value.toDouble() / nRuns }
            .entries.sortedBy { it.key }
            .forEach {
                println(
                    "${it.key}:".padStart(3, ' ') + "%.2f".format(it.value).padStart(6, ' ') + "%"
                )
            }

        println("Which divisors are chosen how often:")
        intervalList
            .map { round(it.base / it.interval).toInt() }.groupingBy { it }
            .eachCount().mapValues { 100 * it.value.toDouble() / nRuns }
            .entries.sortedBy { it.key }
            .forEach {
                println(
                    "${it.key}:".padStart(3, ' ') + "%.2f".format(it.value).padStart(6, ' ') + "%"
                )
            }
        //assertEquals( 0, errors)
        println()
        print("Some of the combinations where the algorithm did not fine a good solution (start, end): ")
        println(no_solution_vals.slice(0..15))
        assertTrue(100 * errors.toDouble() / nRuns.toDouble() <= allowed_error_percentage)
    }

    @Test
    fun find_solution_for_everything_numerical() {
        find_solution_for_everything(time_data = false, long_test = false)
        /**
         * Output from 2021.02.19:
        minimum range used: 0.79

        1.00 -> 0.97:  3.15%
        0.97 -> 0.94:  9.67%
        0.94 -> 0.91: 23.16%
        0.91 -> 0.88: 29.49%
        0.88 -> 0.85: 26.87%
        0.85 -> 0.82:  6.02%
        0.82 -> 0.79:  1.63%
        0.79 -> 0.76:  0.00%
        0.76 -> 0.73:  0.00%
        0.73 -> 0.70:  0.00%
        Didn't find a good solution for 0.050559418398955745%
        How many lines are drawn how often:
        6: 13.56%
        7: 16.09%
        8: 21.75%
        9: 20.28%
        10: 12.51%
        11:  5.42%
        12: 10.39%
        Which divisors are chosen how often:
        1: 45.72%
        2: 26.16%
        3:  5.60%
        4: 14.19%
        5:  5.91%
        8:  2.42%
         */
    }

    @Test
    fun find_solution_for_everything_time() {
        find_solution_for_everything(time_data = true, long_test = false)
        /**
         * Output from 2021.02.19:
        minimum range used: 0.79

        1.00 -> 0.97:  3.45%
        0.97 -> 0.94: 11.98%
        0.94 -> 0.91: 20.93%
        0.91 -> 0.88: 26.81%
        0.88 -> 0.85: 26.58%
        0.85 -> 0.82:  7.53%
        0.82 -> 0.79:  2.71%
        0.79 -> 0.76:  0.01%
        0.76 -> 0.73:  0.00%
        0.73 -> 0.70:  0.00%
        Didn't find a good solution for 0.570686596271549%
        How many lines are drawn how often:
        6:  8.60%
        7: 13.30%
        8: 22.38%
        9: 23.07%
        10: 15.94%
        11:  8.27%
        12:  8.43%
        Which divisors are chosen how often:
        1: 31.23%
        2:  8.99%
        3:  4.70%
        4:  2.76%
        6:  2.43%
        12:  1.94%
        24: 31.63%
        30: 16.32%
         */
    }

    /*
private fun printExampleNumerical(start:Double, end: Double) {
    val interval = try {
        getYParametersInternal(start, end, time_data = false, fixedBounds = false)
    } catch (e: Exception) {
        PossibleInterval((end - start) / 10.0, false, 11, 1.0,
                start, end, 1.0)
    }
    printExampleNumerical( start, end, interval)
}
*/
    private fun printExampleNumerical(start: Double, end: Double) {
        val parameters = uut.getYParameters(start, end, time_data = false, fixedBounds = false)
        val boundsRange = parameters.bounds_max - parameters.bounds_min
        val interval = boundsRange / (parameters.n_intervals - 1)
        println("----------------------------")
        for (i in 0 until parameters.n_intervals.toInt()) {
            val label = parameters.bounds_max - i * interval
            println("$label")
        }
        print("Data range: [$start -> $end] ")
        print("interval: ${interval} x ${parameters.n_intervals.toInt() - 1} | ")
        println("range used = %.1f".format(100 * (end - start) / boundsRange))
        //return interval
    }

    private fun printExampleTime(
        start: Double,
        end: Double
    ): DataDisplayIntervalHelper.YAxisParameters {
        val parameters = uut.getYParameters(
            y_min = start,
            y_max = end,
            time_data = true,
            fixedBounds = false
        )
        val boundsRange = parameters.bounds_max - parameters.bounds_min
        val interval = boundsRange / (parameters.n_intervals - 1)
        println("----------------------------")
        for (i in 0 until parameters.n_intervals.toInt()) {
            val label_seconds = parameters.bounds_max - i * interval
            val label = duration2string(label_seconds.seconds)
            println(label)
        }
        print("Data range: [${duration2string(start.seconds)} -> ${duration2string(end.seconds)}] ")
        print("interval: ${duration2string(interval.seconds)} x ${parameters.n_intervals.toInt() - 1} | ")
        println("range used = ${round(100 * (end - start) / boundsRange)}%")
        return parameters
    }

    private fun duration2string(duration: Duration): String {
        val hours = duration.inWholeHours.toInt().toString().padStart(2, '0')
        val mins = duration.inWholeMinutes.rem(60).toInt().toString().padStart(2, '0')
        val seconds = duration.inWholeSeconds.rem(60).toInt().toString().padStart(2, '0')
        return "$hours:$mins:$seconds"
    }
}