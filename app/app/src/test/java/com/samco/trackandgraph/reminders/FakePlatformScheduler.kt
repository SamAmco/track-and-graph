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

internal class FakePlatformScheduler : PlatformScheduler {
    val setNotifications = mutableListOf<Pair<Long, ReminderNotificationParams>>()
    val cancelledNotifications = mutableListOf<ReminderNotificationParams>()
    val cancelledLegacyAlarms = mutableListOf<LegacyReminderAlarmInfo>()

    override fun set(triggerAtMillis: Long, reminderNotificationParams: ReminderNotificationParams) {
        setNotifications.add(triggerAtMillis to reminderNotificationParams)
    }

    override fun cancelLegacyAlarm(legacyAlarmInfo: LegacyReminderAlarmInfo) {
        cancelledLegacyAlarms.add(legacyAlarmInfo)
    }

    override fun cancel(reminderNotificationParams: ReminderNotificationParams) {
        cancelledNotifications.add(reminderNotificationParams)
    }

    override suspend fun getNextScheduledMillis(reminderNotificationParams: ReminderNotificationParams): Long? {
        // For testing, return null (no scheduled notification)
        return null
    }

    fun reset() {
        setNotifications.clear()
        cancelledNotifications.clear()
        cancelledLegacyAlarms.clear()
    }
}
