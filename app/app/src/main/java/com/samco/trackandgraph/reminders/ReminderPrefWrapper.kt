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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads and clears reminder scheduler prefs written by the pre-10.x reminder implementation.
 * New reminder scheduling state must not be added here.
 */
internal interface LegacyReminderPrefs {
    fun getEncodedLegacyAlarms(): String?
    fun clear()
}

internal class LegacyReminderPrefsImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LegacyReminderPrefs {
    companion object {
        private const val PREFS_NAME = "REMINDERS_PREFS"
        private const val STORED_INTENTS_KEY = "STORED_ALARMS_KEY"
    }

    private val sharedPrefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getEncodedLegacyAlarms() = sharedPrefs.getString(STORED_INTENTS_KEY, null)

    override fun clear() {
        context.deleteSharedPreferences(PREFS_NAME)
    }
}
