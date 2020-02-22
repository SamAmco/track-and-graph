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

package com.samco.trackandgraph.util

import android.content.Context
import android.os.Build
import java.text.NumberFormat
import java.util.*

fun getLocale(context: Context): Locale {
    val cfg = context.resources.configuration
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) cfg.locales.get(0) else cfg.locale
}

fun getDoubleFromText(context: Context, text: String): Double {
    return try {
        val format: NumberFormat = NumberFormat.getInstance(getLocale(context))
        val number: Number = format.parse(text)!!
        number.toDouble()
    } catch (_: Exception) { 0.0 }
}
