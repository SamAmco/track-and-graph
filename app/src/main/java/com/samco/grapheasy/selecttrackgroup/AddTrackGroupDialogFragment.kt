package com.samco.grapheasy.selecttrackgroup

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R


class AddTrackGroupDialogFragment : DialogFragment() {
    private lateinit var listener: AddTrackGroupDialogListener
    private lateinit var editText: EditText

    interface AddTrackGroupDialogListener {
        fun onAddTrackGroup(name: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as AddTrackGroupDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement YesCancelDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.add_track_group_dialog, null)
            editText = view.findViewById(R.id.track_group_name_input)
            var builder = AlertDialog.Builder(it)
            //TODO some validation of the name
            builder.setView(view)
                .setPositiveButton(R.string.add) { _, _ -> listener.onAddTrackGroup(editText.text.toString()) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        super.onActivityCreated(savedInstanceState)
    }
}