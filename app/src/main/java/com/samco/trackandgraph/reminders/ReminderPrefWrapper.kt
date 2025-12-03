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
 * This interface is now legacy and will be removed in a future release. It exists here only so
 * that apps with prefs still stored can run a migration to delete any alarms stored in prefs.
 */
@Deprecated("This interface is now legacy and will be removed in a future release")
internal interface ReminderPrefWrapper {
    fun getStoredIntents(): String?
    fun clear()
}

@Deprecated("This interface is now legacy and will be removed in a future release")
internal class ReminderPrefWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderPrefWrapper {
    companion object {
        private const val PREFS_NAME = "REMINDERS_PREFS"
        private const val STORED_INTENTS_KEY = "STORED_ALARMS_KEY"
    }

    private val sharedPrefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getStoredIntents() = sharedPrefs.getString(STORED_INTENTS_KEY, null)

    override fun clear() {
        context.deleteSharedPreferences(PREFS_NAME)
    }
}