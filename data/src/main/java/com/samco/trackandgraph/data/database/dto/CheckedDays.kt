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

package com.samco.trackandgraph.data.database.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckedDays (
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean
) {
    private val asList by lazy { toList() }

    fun toList() = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday)

    operator fun get(index: Int) = asList[index]

    companion object {
        fun fromList(bools: List<Boolean>): CheckedDays {
            if (bools.size != 7) return none()
            return CheckedDays(
                bools[0],
                bools[1],
                bools[2],
                bools[3],
                bools[4],
                bools[5],
                bools[6]
            )
        }

        fun none() = CheckedDays(
            monday = false,
            tuesday = false,
            wednesday = false,
            thursday = false,
            friday = false,
            saturday = false,
            sunday = false
        )

        fun all() = CheckedDays(
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            sunday = true
        )

        fun CheckedDays.withSet(index: Int, value: Boolean): CheckedDays {
            val list = toList().toMutableList()
            list[index] = value
            return fromList(list)
        }
    }
}
