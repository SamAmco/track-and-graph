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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalTime

@Entity(tableName = "reminders_table")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "name")
    val alarmName: String,

    @ColumnInfo(name = "time")
    val time: LocalTime,

    @ColumnInfo(name = "checked_days")
    val checkedDays: CheckedDays
)

data class CheckedDays (
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean
) {
    fun toList() = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday)

    override fun equals(other: Any?): Boolean {
        return other is CheckedDays
                && other.monday == monday
                && other.tuesday == tuesday
                && other.wednesday == wednesday
                && other.thursday == thursday
                && other.friday == friday
                && other.saturday == saturday
                && other.sunday == sunday
    }

    companion object {
        fun fromList(bools: List<Boolean>): CheckedDays {
            assert(bools.size == 7)
            return CheckedDays(bools[0], bools[1], bools[2], bools[3], bools[4], bools[5], bools[6])
        }

        fun none() = CheckedDays(false, false, false,
            false, false, false, false)
    }
}
