package com.samco.grapheasy.selecttrackgroup

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R


class AddTrackGroupDialogFragment : DialogFragment(), TextWatcher {
    private lateinit var listener: AddTrackGroupDialogListener
    private lateinit var editText: EditText
    private lateinit var alertDialog: AlertDialog

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
            editText.addTextChangedListener(this)
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.add) { _, _ -> listener.onAddTrackGroup(editText.text.toString()) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun afterTextChanged(p0: Editable?) { }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, length: Int) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = length > 0
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }
}