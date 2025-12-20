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

import kotlinx.serialization.Serializable

@Serializable
@Deprecated("Alarm information should no longer be persisted, use [AlarmInfo]")
internal data class StoredAlarmInfo(
    val reminderId: Long,
    val reminderName: String,
    val pendingIntentId: Int
)

// TODO add support for group ID
internal data class ReminderNotificationParams(
    /**
     * The alarm ID is an ID that the platform can use to identify the alarm/notification it
     * currently has scheduled for this reminder or assign to a new one. It should be generated
     * from the reminder ID such that there is a 1:1 mapping between the reminder ID and the alarm
     * ID.
     */
    val alarmId: Int,

    /**
     * The ID of the reminder this notification is for in the current user database.
     */
    val reminderId: Long,

    /**
     * The name string displayed to the user for this reminder notification.
     */
    val reminderName: String,
)

internal interface PlatformScheduler {
    /**
     * Schedules a notification to trigger at the specified time in UTC epoch milliseconds.
     */
    fun set(triggerAtMillis: Long, reminderNotificationParams: ReminderNotificationParams)

    /**
     * Returns the next scheduled notification time in UTC epoch milliseconds for the given reminder.
     */
    suspend fun getNextScheduledMillis(reminderNotificationParams: ReminderNotificationParams): Long?

    @Deprecated("This remains only for users of 9.x who still have persisted alarms to cancel them on first sync")
    fun cancel(storedAlarmInfo: StoredAlarmInfo)

    /**
     * Cancels the notification for the specified reminder.
     */
    fun cancel(reminderNotificationParams: ReminderNotificationParams)
}