package com.samco.grapheasy.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R

class YesCancelDialogFragment(
    @StringRes val title: Int,
    val onYes: () -> Unit,
    val onCancel: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            var builder = AlertDialog.Builder(it)
            builder.setMessage(title)
                .setPositiveButton(R.string.yes) { _, _ -> onYes() }
                .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}