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
import com.samco.trackandgraph.database.dto.DisplayNote
import com.samco.trackandgraph.database.dto.NoteType
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.database.entity.GlobalNote
import com.samco.trackandgraph.databinding.ShowNoteDialogHeaderBinding
import com.samco.trackandgraph.util.formatDayMonthYearHourMinute
import org.threeten.bp.OffsetDateTime

fun showFeatureDescriptionDialog(context: Context, name: String, description: String) {
    val descriptionOrNone = if (description.isEmpty())
        context.getString(R.string.no_description)
    else description

    val descriptionView = LayoutInflater.from(context)
        .inflate(R.layout.feature_description_layout, null, false)

    descriptionView.findViewById<TextView>(R.id.tv_title).let {
        it.text = name
    }
    descriptionView.findViewById<TextView>(R.id.tv_description).let {
        it.text = descriptionOrNone
    }

    AlertDialog.Builder(context)
        .setView(descriptionView)
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
    return bodyView
}

fun showNoteDialog(inflater: LayoutInflater, context: Context, note: GlobalNote) {
    val headerView = getNoteDialogHeader(inflater, context, note.timestamp, null, null)
    AlertDialog.Builder(context)
        .setCustomTitle(headerView)
        .setView(getBodyTextView(context, note.note))
        .create()
        .show()
}

fun showNoteDialog(inflater: LayoutInflater, context: Context, note: DisplayNote) {
    val featureDispalayName = when (note.noteType) {
        NoteType.DATA_POINT -> "${note.trackGroupName} -> ${note.featureName}"
        NoteType.GLOBAL_NOTE -> ""
    }
    val headerView =
        getNoteDialogHeader(inflater, context, note.timestamp, null, featureDispalayName)
    AlertDialog.Builder(context)
        .setCustomTitle(headerView)
        .setView(getBodyTextView(context, note.note))
        .create()
        .show()
}

fun getNoteDialogHeader(
    inflater: LayoutInflater, context: Context, timestamp: OffsetDateTime,
    displayValue: String?, featureDispalayName: String?
): View {
    val headerView = ShowNoteDialogHeaderBinding.inflate(inflater)
    headerView.dateTimeText.text = formatDayMonthYearHourMinute(context, timestamp)
    headerView.valueText.visibility = if (displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
    displayValue?.let { headerView.valueText.text = it }
    headerView.featureDisplayNameText.visibility =
        if (featureDispalayName.isNullOrEmpty()) View.GONE else View.VISIBLE
    featureDispalayName?.let { headerView.featureDisplayNameText.text = it }
    return headerView.root
}

fun showDataPointDescriptionDialog(
    context: Context, inflater: LayoutInflater, dataPoint: DataPoint,
    featureType: FeatureType, featureDispalayName: String? = null
) {
    showDataPointDescriptionDialog(
        context,
        inflater,
        dataPoint.timestamp,
        DataPoint.getDisplayValue(dataPoint, featureType),
        dataPoint.note,
        featureDispalayName
    )
}

fun showDataPointDescriptionDialog(
    context: Context, inflater: LayoutInflater, timestamp: OffsetDateTime, displayValue: String,
    note: String, featureDispalayName: String? = null
) {
    val res = context.resources

    val headerView =
        getNoteDialogHeader(inflater, context, timestamp, displayValue, featureDispalayName)

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
        .setCustomTitle(headerView)
        .setView(bodyView)
        .create()
        .show()
}
