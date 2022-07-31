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

const val THEME_SETTING_PREF_KEY = "theme_setting"
const val DATE_FORMAT_SETTING_PREF_KEY = "date_format_setting"
const val FIRST_RUN_PREF_KEY = "firstrun2"

fun getPrefs(context: Context, mode: Int = AppCompatActivity.MODE_PRIVATE): SharedPreferences {
    return context.getSharedPreferences("com.samco.trackandgraph", mode)
}
