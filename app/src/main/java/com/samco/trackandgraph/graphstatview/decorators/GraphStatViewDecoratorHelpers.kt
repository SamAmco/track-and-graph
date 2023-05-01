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

package com.samco.trackandgraph.graphstatview.decorators

import android.content.Context
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.ui.GraphLegendItemView

internal fun inflateGraphLegendItem(
    binding: GraphStatViewBinding, context: Context,
    color: Int, label: String
) {
    binding.legendFlexboxLayout.addView(
        GraphLegendItemView(context).apply {
            this.color = color
            this.text = label
        }
    )
}

internal fun formatTimeToDaysHoursMinutesSeconds(
    context: Context,
    millis: Long,
    twoLines: Boolean = true
): String {
    val totalSeconds = millis / 1000
    val daysNum = (totalSeconds / 86400).toInt()
    val days = daysNum.toString()
    val hours = ((totalSeconds % 86400) / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    val hoursStr = "%02d".format(((totalSeconds % 86400) / 3600).toInt())
    val minutesStr = "%02d".format(((totalSeconds % 3600) / 60).toInt())
    val secondsStr = "%02d".format((totalSeconds % 60).toInt())
    val hasHms = (hours + minutes + seconds) > 0
    val hms = "$hoursStr:$minutesStr:$secondsStr"

    return StringBuilder().apply {
        if (daysNum == 0) append(hms)
        else {
            val daysSuffix =
                if (daysNum == 1) context.getString(R.string.day)
                else context.getString(R.string.days)

            append("$days $daysSuffix")

            if (hasHms) {
                if (twoLines) appendLine()
                else append(", ")
                append(hms)
            }
        }
    }.toString()
}
