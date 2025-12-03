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

package com.samco.trackandgraph.reminders.androidplatform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samco.trackandgraph.reminders.ReminderInteractor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecreateAlarmsBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var reminderInteractor: ReminderInteractor

    override fun onReceive(context: Context?, intent: Intent?) {
        val valid = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent?.action !in valid) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            reminderInteractor.syncReminderNotifications()
            pending.finish()
        }
    }
}