package com.samco.grapheasy.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R

class YesCancelDialogFragment : DialogFragment() {
    lateinit var title: String
    private lateinit var listener: YesCancelDialogListener

    interface YesCancelDialogListener {
        fun onDialogYes(dialog: YesCancelDialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as YesCancelDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement YesCancelDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        title = arguments?.getString("title") ?: ""
        return activity?.let {
            var builder = AlertDialog.Builder(it)
            builder.setMessage(title)
                .setPositiveButton(R.string.yes) { _, _ -> listener.onDialogYes(this) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}