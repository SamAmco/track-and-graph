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
            fileButton.setTextColor(ContextCompat.getColor(context, R.color.regularText))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
        cursor?.close()
    }
}