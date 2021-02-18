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

package com.samco.trackandgraph.statistics

import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.math.floor
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class Statistics_getYParameters_KtTest {

    // these first four ones aren't really tests, but declaring them as tests makes it easier to run them

    @Test
    fun manuallyTestValuesNumerical() {
        runBlocking {
            val y_min = 0.8
            val y_max = 77.4

            printExampleNumerical(y_min.toDouble(), y_max.toDouble())
        }
    }

    @Test
    @ExperimentalTime
    fun manuallyTestValuesTime() {
        runBlocking {
            val y_min = 1 *60*60 + 53 *60 // in seconds
            val y_max = 8 *60*60 + 25 *60

            printExampleTime(y_min.toDouble(), y_max.toDouble())
        }
    }

    @Test
    @ExperimentalTime
    fun makeExamplesTime() {
        for (q_ in 0 until 32) {
            val start =  Random.nextInt(0,3*60*60).toDouble()
            val length = Random.nextInt(1,10*60*60).toDouble()
            val end = start + length

            printExampleTime(start, end)
        }
    }

    @Test
    fun makeExamplesNumerical() {
        for (q_ in 0 until 32) {
            val start =  Random.nextInt(0,1000).toDouble() / 10
            val length = Random.nextInt(1,500).toDouble() / 10
            val end = start + length

            printExampleNumerical(start, end)
        }
    }

    @Test
    fun find_solution_for_everything_numerical() {
        runBlocking {
            var errors = 0
            val range_used_list = mutableListOf<Double>()
            for (start_ in 0..10000 step 7) {
                val start : Double = start_ / 10.0
                for (length_ in 1..5000) {
                    val length : Double = length_ / 10.0
                    try {
                        val parameters = getYParameters(start, start+length,
                                                time_data = false, throw_exc_if_non_found = true,
                                                fixedBounds = false)
                        val range_used = length / (parameters.bounds_max-parameters.bounds_min)
                        range_used_list.add(range_used)
                    } catch (e: Exception) {
                        throw Exception("min: $start, max: ${start + length}")
                        errors += 1
                    }

                }
            }
            print("minimum range used: ${range_used_list.minOrNull()}\n\n")
            val nRuns = range_used_list.count()
            for (i in 0..9) {
                val top = 1-i*0.03
                val bot = 1-(i+1)*0.03
                val nInRange = range_used_list.filter { top >= it && it > bot }.count()
                print("$top -> $bot: ${100*nInRange.toDouble()/nRuns.toDouble()}%\n")

                /**
                 * OUTPUT FROM 2021.02.18 :
                minimum range used: 0.7148571428571429

                1.0  -> 0.97:  4.690510846745976%
                0.97 -> 0.94: 13.121357592722184%
                0.94 -> 0.91: 20.35140657802659%
                0.91 -> 0.88: 26.00474457662701%
                0.88 -> 0.85: 20.37006298110567%
                0.85 -> 0.82:  9.145878236529041%
                0.82 -> 0.79:  4.100265920223933%
                0.79 -> 0.76:  1.569447165850245%
                0.76 -> 0.73:  0.527851644506648%
                0.73 -> 0.7:   0.06376487053883835%

                Process finished with exit code 0

                 */

            }
            assertEquals( 0, errors)
        }
    }

    @Test
    fun find_solution_for_everything_time() {
        // only difference in code is that time_data is true here
        runBlocking {
            var errors = 0
            val range_used_list = mutableListOf<Double>()
            for (start_ in 0..10000 step 7) {
                val start : Double = start_ / 10.0
                for (length_ in 1..5000) {
                    val length : Double = length_ / 10.0
                    try {
                        val parameters = getYParameters(start, start+length,
                                time_data = true, throw_exc_if_non_found = true,
                                fixedBounds = false)
                        val range_used = length / (parameters.bounds_max-parameters.bounds_min)
                        range_used_list.add(range_used)
                    } catch (e: Exception) {
                        throw Exception("min: ${start /10}, max: ${(start + length)/10}")
                        errors += 1
                    }

                }
            }
            print("minimum range used: ${range_used_list.minOrNull()}\n\n")
            val nRuns = range_used_list.count()
            for (i in 0..9) {
                val top = 1-i*0.03
                val bot = 1-(i+1)*0.03
                val nInRange = range_used_list.filter { top >= it && it > bot }.count()
                print("$top -> $bot: ${100*nInRange.toDouble()/nRuns.toDouble()}%\n")

                /**
                 * OUTPUT FROM 2021.02.18 :
                minimum range used: 0.7171428571428572

                1.0  -> 0.97:  4.965374387683695%
                0.97 -> 0.94: 14.046955913226032%
                0.94 -> 0.91: 23.68197340797761%
                0.91 -> 0.88: 27.03037088873338%
                0.88 -> 0.85: 19.606815955213435%
                0.85 -> 0.82:  7.787837648705389%
                0.82 -> 0.79:  2.292078376487054%
                0.79 -> 0.76:  0.4178026592022393%
                0.76 -> 0.73:  0.1366410076976907%
                0.73 -> 0.7:   0.015087473757872638%
                 */

            }
            assertEquals( 0, errors)
        }
    }

    private fun printExampleNumerical(start: Double, end: Double): YAxisParameters {
        val parameters = getYParameters(start, end, time_data = false, fixedBounds = false, throw_exc_if_non_found = true)
        val boundsRange = parameters.bounds_max - parameters.bounds_min
        val interval = boundsRange / (parameters.n_intervals-1)
        println("----------------------------")
        for (i in 0 until parameters.n_intervals.toInt()) {
            val label = parameters.bounds_max - i*interval
            println("$label")
        }
        print("Data range: [$start -> $end] ")
        print("interval: $interval x ${parameters.n_intervals.toInt()} | ")
        println("range used = ${round(100*(end-start)/boundsRange)}%")
        return parameters
    }



    @ExperimentalTime
    private fun printExampleTime(start: Double, end: Double): YAxisParameters {
        val parameters = getYParameters(start, end, time_data = true, fixedBounds = false, throw_exc_if_non_found = true)
        val boundsRange = parameters.bounds_max - parameters.bounds_min
        val interval = boundsRange / (parameters.n_intervals-1)
        println("----------------------------")
        for (i in 0 until parameters.n_intervals.toInt()) {
            val label_seconds = parameters.bounds_max - i*interval
            val label = duration2string(label_seconds.seconds)
            println(label)
        }
        print("Data range: [${duration2string(start.seconds)} -> ${duration2string(end.seconds)}] ")
        print("interval: ${duration2string(interval.seconds)} x ${parameters.n_intervals.toInt()} | ")
        println("range used = ${round(100*(end-start)/boundsRange)}%")
        return parameters
    }

    @ExperimentalTime
    fun duration2string(duration: Duration): String {
        val hours = floor(duration.inHours).toInt().toString().padStart(2, '0')
        val mins = floor(duration.inMinutes).rem(60).toInt().toString().padStart(2, '0')
        val seconds = floor(duration.inSeconds).rem(60).toInt().toString().padStart(2, '0')

        return "$hours:$mins:$seconds"

    }
}