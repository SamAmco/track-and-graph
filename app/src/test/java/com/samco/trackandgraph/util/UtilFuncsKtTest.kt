/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.util

import org.junit.Test

import org.junit.Assert.*

class UtilFuncsTest {

    @Test
    fun getDoubleFromText() {
        val testInOut = mapOf(
            "1.034567" to 1.034567,
            "1,0345.67" to 10345.67,
            "1.0345,67" to 10345.67,
            "1,034,567" to 1034567.0,
            "1.034567" to 1.034567,
            "1,03.4567" to 103.4567,
            "1,023,40,.1" to 102340.1,
            "123454" to 123454.0,
            "-12345" to -12345.0,
            "-1.0345" to -1.0345,
            "-1,532,458" to -1532458.0,
            "-2.3.4.0" to -2340.0,
            "-4.516.9,0" to -45169.0,
            "something that's not a number" to 0.0,
            "0.3435e" to 0.0,
            "-0.43e59" to -4.3e58,
            "" to 0.0,
            "e-4.2" to 0.0,
            "4,3e5" to 4.3e5,
            "3,6" to 3.6,
            "4." to 4.0,
            "4," to 4.0,
            ",4" to 0.4,
            ".4" to 0.4
        )
        for (t in testInOut) {
            //println(t.key)
            assertEquals(t.value, getDoubleFromText(t.key), 0.0)
        }
    }
}