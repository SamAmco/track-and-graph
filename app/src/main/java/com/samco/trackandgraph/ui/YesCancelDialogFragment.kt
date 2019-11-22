package com.samco.trackandgraph.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import com.samco.trackandgraph.R

class YesCancelDialogFragment : DialogFragment() {
    lateinit var title: String
    private lateinit var listener: YesCancelDialogListener

    interface YesCancelDialogListener {
        fun onDialogYes(dialog: YesCancelDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        title = arguments?.getString("title") ?: ""
        return activity?.let {
            listener = parentFragment as YesCancelDialogListener
            var builder = AlertDialog.Builder(it)
            builder.setMessage(title)
                .setPositiveButton(R.string.yes) { _, _ -> listener.onDialogYes(this) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            val alertDialog = builder.create()
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(context!!, R.color.secondaryColor))
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(context!!, R.color.toolBarTextColor ))
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}