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

package com.samco.trackandgraph.reminders

import com.samco.trackandgraph.system.AlarmInfo
import com.samco.trackandgraph.system.AlarmManagerWrapper
import com.samco.trackandgraph.system.StoredAlarmInfo

internal class FakeAlarmManagerWrapper : AlarmManagerWrapper {
    val setAlarms = mutableListOf<Pair<Long, AlarmInfo>>()
    val cancelledAlarms = mutableListOf<AlarmInfo>()
    val cancelledStoredAlarms = mutableListOf<StoredAlarmInfo>()

    override fun set(triggerAtMillis: Long, alarmInfo: AlarmInfo) {
        setAlarms.add(triggerAtMillis to alarmInfo)
    }

    override fun cancel(storedAlarmInfo: StoredAlarmInfo) {
        cancelledStoredAlarms.add(storedAlarmInfo)
    }

    override fun cancel(alarmInfo: AlarmInfo) {
        cancelledAlarms.add(alarmInfo)
    }

    fun reset() {
        setAlarms.clear()
        cancelledAlarms.clear()
        cancelledStoredAlarms.clear()
    }
}