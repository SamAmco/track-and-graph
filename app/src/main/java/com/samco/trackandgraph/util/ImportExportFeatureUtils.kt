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

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.R

object ImportExportFeatureUtils {
    fun setFileButtonTextFromUri(activity: Activity?, context: Context, uri: Uri, fileButton: Button, alertDialog: AlertDialog) {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor = activity?.contentResolver?.query(uri, projection, null, null, null)
        val index = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        if (cursor != null && index != null) {
            fileButton.text = cursor.getString(index)
            fileButton.setTextColor(fileButton.context.getColorFromAttr(android.R.attr.textColorPrimary))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
        cursor?.close()
    }
}