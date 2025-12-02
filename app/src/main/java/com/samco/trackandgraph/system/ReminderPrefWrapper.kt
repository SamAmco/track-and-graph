package com.samco.trackandgraph.system

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * This interface is now legacy and will be removed in a future release. It exists here only so
 * that apps with prefs still stored can run a migration to delete any alarms stored in prefs.
 */
internal interface ReminderPrefWrapper {
    fun getStoredIntents(): String?
    fun clear()
}

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