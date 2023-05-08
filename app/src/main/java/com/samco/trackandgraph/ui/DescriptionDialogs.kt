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

package com.samco.trackandgraph.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.base.helpers.getDisplayValue
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.DescriptionBodyTextBinding
import com.samco.trackandgraph.databinding.ShowNoteDialogHeaderBinding
import org.threeten.bp.OffsetDateTime

fun showFeatureDescriptionDialog(context: Context, name: String, description: String) {
    val descriptionOrNone = description.ifEmpty { context.getString(R.string.no_description) }

    val descriptionView = LayoutInflater.from(context)
        .inflate(R.layout.feature_description_layout, null, false)

    descriptionView.findViewById<TextView>(R.id.tv_title).text = name
    descriptionView.findViewById<TextView>(R.id.tv_description).text = descriptionOrNone

    AlertDialog.Builder(context)
        .setView(descriptionView)
        .create()
        .show()
}

private fun getBodyTextView(context: Context, text: String): View {
    val bodyView = DescriptionBodyTextBinding.inflate(LayoutInflater.from(context))
    bodyView.text.text = text
    return bodyView.root
}

fun showNoteDialog(inflater: LayoutInflater, context: Context, note: GlobalNote) {
    val headerView = getNoteDialogHeader(inflater, context, note.timestamp, null, null)
    AlertDialog.Builder(context)
        .setCustomTitle(headerView)
        .setView(getBodyTextView(context, note.note))
        .create()
        .show()
}

fun showNoteDialog(
    inflater: LayoutInflater,
    context: Context,
    note: DisplayNote,
    featurePath: String?
) {
    val headerView =
        getNoteDialogHeader(inflater, context, note.timestamp, null, featurePath)
    AlertDialog.Builder(context)
        .setCustomTitle(headerView)
        .setView(getBodyTextView(context, note.note))
        .create()
        .show()
}

private fun getNoteDialogHeader(
    inflater: LayoutInflater,
    context: Context,
    timestamp: OffsetDateTime,
    displayValue: String?,
    featureDisplayName: String?
): View {
    val headerView = ShowNoteDialogHeaderBinding.inflate(inflater)
    headerView.dateTimeText.text =
        formatDayMonthYearHourMinuteWeekDayOneLine(context, getWeekDayNames(context), timestamp)
    headerView.valueText.visibility = if (displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
    displayValue?.let { headerView.valueText.text = it }
    headerView.featureDisplayNameText.visibility =
        if (featureDisplayName.isNullOrEmpty()) View.GONE else View.VISIBLE
    featureDisplayName?.let { headerView.featureDisplayNameText.text = it }
    return headerView.root
}

fun showDataPointDescriptionDialog(
    context: Context,
    inflater: LayoutInflater,
    dataPoint: DataPoint,
    isDuration: Boolean,
    featureDispalayName: String? = null
) {
    showDataPointDescriptionDialog(
        context,
        inflater,
        dataPoint.timestamp,
        dataPoint.getDisplayValue(isDuration),
        dataPoint.note,
        featureDispalayName
    )
}

fun showDataPointDescriptionDialog(
    context: Context, inflater: LayoutInflater, timestamp: OffsetDateTime, displayValue: String,
    note: String, featureDispalayName: String? = null
) {
    val headerView =
        getNoteDialogHeader(inflater, context, timestamp, displayValue, featureDispalayName)

    val bodyView = getBodyTextView(context, note)

    AlertDialog.Builder(context)
        .setCustomTitle(headerView)
        .setView(bodyView)
        .create()
        .show()
}
