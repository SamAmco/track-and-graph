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

package com.samco.trackandgraph.widgets

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker

/**
 * Centralized widget state management for Track & Graph widgets
 * Contains shared preference keys, data structures, and state management logic
 */
object TrackWidgetState {

    // Shared preference keys used across all widget components
    private val KEY_FEATURE_ID = longPreferencesKey("feature_id")
    private val KEY_TITLE = stringPreferencesKey("title")
    private val KEY_REQUIRE_INPUT = booleanPreferencesKey("require_input")
    private val KEY_IS_DURATION = booleanPreferencesKey("is_duration")
    private val KEY_TIMER_RUNNING = booleanPreferencesKey("timer_running")
    private val KEY_IS_ENABLED = booleanPreferencesKey("is_enabled")

    const val WIDGET_PREFS_NAME = "TrackWidget"
    const val DELETE_FEATURE_ID = "DELETE_FEATURE_ID"
    const val UPDATE_FEATURE_ID = "UPDATE_FEATURE_ID"

    fun getFeatureIdPref(appWidgetId: Int) = "widget_feature_id_$appWidgetId"

    /**
     * Widget data structure representing the state of a track widget for display
     */
    sealed class WidgetData {
        data object Disabled : WidgetData()

        data class Enabled(
            val appWidgetId: Int,
            val featureId: Long,
            val title: String,
            val requireInput: Boolean,
            val isDuration: Boolean,
            val timerRunning: Boolean
        ) : WidgetData()
    }

    /**
     * Extension function to update widget preferences with tracker data
     */
    fun MutablePreferences.updateFromTracker(featureId: Long, tracker: DisplayTracker?) {
        // Update all preference keys
        this[KEY_FEATURE_ID] = featureId
        this[KEY_TITLE] = tracker?.name ?: ""
        this[KEY_REQUIRE_INPUT] = tracker?.hasDefaultValue?.not() ?: true
        this[KEY_IS_DURATION] = tracker?.dataType == DataType.DURATION
        this[KEY_TIMER_RUNNING] = tracker?.timerStartInstant != null
        this[KEY_IS_ENABLED] = tracker != null
    }

    /**
     * Create WidgetData from preferences
     */
    fun createWidgetDataFromPreferences(prefs: Preferences): WidgetData {
        val featureId = prefs[KEY_FEATURE_ID] ?: -1L
        val isEnabled = prefs[KEY_IS_ENABLED] ?: false

        return if (isEnabled && featureId != -1L) {
            WidgetData.Enabled(
                appWidgetId = -1, // Not needed for UI
                featureId = featureId,
                title = prefs[KEY_TITLE] ?: "",
                requireInput = prefs[KEY_REQUIRE_INPUT] ?: false,
                isDuration = prefs[KEY_IS_DURATION] ?: false,
                timerRunning = prefs[KEY_TIMER_RUNNING] ?: false
            )
        } else {
            WidgetData.Disabled
        }
    }
}
