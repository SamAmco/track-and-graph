package com.samco.grapheasy.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.samco.grapheasy.R

abstract class NameInputDialogFragment : DialogFragment(), TextWatcher {
    private lateinit var editText: EditText
    private lateinit var alertDialog: AlertDialog

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            registerListener(parentFragment)
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement YesCancelDialogListener"))
        }
    }

    abstract fun registerListener(parentFragment: Fragment?)
    abstract fun getPositiveButtonName() : String
    abstract fun onPositiveClicked(name: String)
    abstract fun getNameInputHint() : String
    abstract fun getTitleText() : String
    abstract fun getNameInputText() : String

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.name_input_dialog, null)
            editText = view.findViewById(R.id.edit_name_input)
            editText.hint = getNameInputHint()
            editText.setText(getNameInputText())
            editText.setSelection(editText.text.length)
            editText.addTextChangedListener(this)
            view.findViewById<TextView>(R.id.prompt_text).text = getTitleText()
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(getPositiveButtonName()) { _, _ -> onPositiveClicked(editText.text.toString()) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun afterTextChanged(p0: Editable?) {
        p0?.let {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = p0.isNotEmpty()
        }
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, length: Int) { }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }
}