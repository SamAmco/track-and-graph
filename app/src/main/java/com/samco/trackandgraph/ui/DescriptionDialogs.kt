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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.samco.trackandgraph.R

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
