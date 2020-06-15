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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.TextViewCompat
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.database.DisplayNote
import com.samco.trackandgraph.database.NoteType
import com.samco.trackandgraph.databinding.ShowDataPointDialogHeaderBinding
import com.samco.trackandgraph.util.formatDayMonthYearHourMinute
import org.threeten.bp.OffsetDateTime

fun showFeatureDescriptionDialog(context: Context, name: String, description: String) {
    val res = context.resources

    val descriptionOrNone = if (description.isEmpty())
        context.getString(R.string.no_description)
    else description

    val bodyView = getBodyTextView(context, descriptionOrNone)

    AlertDialog.Builder(context)
        .setTitle(name)
        .setView(bodyView)
        .create()
        .show()
}

private fun getBodyTextView(context: Context, text: String): TextView {
    val bodyView = TextView(context)
    TextViewCompat.setTextAppearance(bodyView, R.style.TextAppearance_Body)
    bodyView.text = text
    bodyView.setTextIsSelectable(true)
    val res = context.resources
    val startEndPadding = res.getDimension(R.dimen.report_dialog_start_end_padding).toInt()
    val topBottomPadding = res.getDimension(R.dimen.card_padding).toInt()
    bodyView.setPadding(startEndPadding, topBottomPadding, startEndPadding, topBottomPadding)
    bodyView.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        res.getDimension(R.dimen.text_body_size)
    )
    return bodyView
}

fun showNoteDialog(context: Context, note: DisplayNote) {
    val bodyText = when (note.noteType) {
        NoteType.DATA_POINT -> """${note.trackGroupName} -> ${note.featureName}
            |
            |${note.note}
        """.trimMargin()
        NoteType.GLOBAL_NOTE -> note.note
    }

    AlertDialog.Builder(context)
        .setTitle(formatDayMonthYearHourMinute(context, note.timestamp))
        .setView(getBodyTextView(context, bodyText))
        .create()
        .show()
}

fun showDataPointDescriptionDialog(
    context: Context, inflater: LayoutInflater, dataPoint: DataPoint,
    featureDispalayName: String? = null
) {
    showDataPointDescriptionDialog(
        context,
        inflater,
        dataPoint.timestamp,
        dataPoint.getDisplayValue(),
        dataPoint.note,
        featureDispalayName
    )
}

fun showDataPointDescriptionDialog(
    context: Context, inflater: LayoutInflater, timestamp: OffsetDateTime, displayValue: String,
    note: String, featureDispalayName: String? = null
) {
    val res = context.resources

    val headerView = ShowDataPointDialogHeaderBinding.inflate(inflater)
    headerView.dateTimeText.text = formatDayMonthYearHourMinute(context, timestamp)
    headerView.valueText.text = displayValue
    if (featureDispalayName != null && featureDispalayName.isNotEmpty()) {
        headerView.featureDisplayNameText.visibility = View.VISIBLE
        headerView.featureDisplayNameText.text = featureDispalayName
    } else {
        headerView.featureDisplayNameText.visibility = View.GONE
    }

    val bodyView = TextView(context)
    TextViewCompat.setTextAppearance(bodyView, R.style.TextAppearance_Body)
    bodyView.text = note
    bodyView.setTextIsSelectable(true)
    val startEndPadding = res.getDimension(R.dimen.report_dialog_start_end_padding).toInt()
    val topBottomPadding = res.getDimension(R.dimen.card_padding).toInt()
    bodyView.setPadding(startEndPadding, topBottomPadding, startEndPadding, topBottomPadding)
    bodyView.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        res.getDimension(R.dimen.text_body_size)
    )

    AlertDialog.Builder(context)
        .setCustomTitle(headerView.root)
        .setView(bodyView)
        .create()
        .show()
}
