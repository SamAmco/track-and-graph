/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.base.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val THEME_SETTING_PREF_KEY = "theme_setting"
const val DATE_FORMAT_SETTING_PREF_KEY = "date_format_setting"
const val FIRST_RUN_PREF_KEY = "firstrun2"
const val HIDE_DATA_POINT_TUTORIAL_PREF_KEY = "HIDE_DATA_POINT_TUTORIAL_PREF_KEY"

fun getPrefs(context: Context, mode: Int = AppCompatActivity.MODE_PRIVATE): SharedPreferences {
    return context.getSharedPreferences("com.samco.trackandgraph", mode)
}

interface PrefHelper {
    fun getHideDataPointTutorial(): Boolean

    fun isFirstRun(): Boolean

    fun setFirstRun(firstRun: Boolean)

    fun setDateTimeFormatIndex(formatIndex: Int)

    fun getDateFormatValue(): Int

    fun getThemeValue(defaultThemeValue: Int): Int

    fun setThemeValue(themeValue: Int)

    fun setHideDataPointTutorial(hide: Boolean)
}

class PrefHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PrefHelper {

    private val prefs get() = getPrefs(context)

    override fun getHideDataPointTutorial(): Boolean =
        prefs.getBoolean(HIDE_DATA_POINT_TUTORIAL_PREF_KEY, false)

    override fun isFirstRun(): Boolean = prefs.getBoolean(FIRST_RUN_PREF_KEY, true)

    override fun setFirstRun(firstRun: Boolean) {
        prefs.edit().putBoolean(FIRST_RUN_PREF_KEY, firstRun).apply()
    }

    override fun setDateTimeFormatIndex(formatIndex: Int) {
        prefs.edit().putInt(DATE_FORMAT_SETTING_PREF_KEY, formatIndex).apply()
    }

    override fun getDateFormatValue(): Int =
        prefs.getInt(DATE_FORMAT_SETTING_PREF_KEY, DateFormatSetting.DMY.ordinal)

    override fun getThemeValue(defaultThemeValue: Int): Int = prefs.getInt(THEME_SETTING_PREF_KEY, defaultThemeValue)

    override fun setThemeValue(themeValue: Int) {
        prefs.edit().putInt(THEME_SETTING_PREF_KEY, themeValue).apply()
    }

    override fun setHideDataPointTutorial(hide: Boolean) {
        prefs.edit().putBoolean(HIDE_DATA_POINT_TUTORIAL_PREF_KEY, hide).apply()
    }
}