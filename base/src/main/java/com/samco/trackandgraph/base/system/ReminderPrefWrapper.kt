package com.samco.trackandgraph.base.system

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


internal interface ReminderPrefWrapper {

    fun getStoredIntents(): String?

    fun putStoredIntents(str: String)
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

    override fun putStoredIntents(str: String) {
        sharedPrefs.edit().apply {
            putString(
                STORED_INTENTS_KEY,
                str
            )
        }.commit()
    }
}