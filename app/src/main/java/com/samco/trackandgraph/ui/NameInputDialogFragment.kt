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
package com.samco.trackandgraph.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R

abstract class NameInputDialogFragment : DialogFragment(), TextWatcher {
    private lateinit var editText: EditText
    private lateinit var alertDialog: AlertDialog

    abstract fun registerListener(parentFragment: Fragment?)
    abstract fun getPositiveButtonName() : String
    abstract fun onPositiveClicked(name: String)
    abstract fun getNameInputHint() : String
    abstract fun getTitleText() : String
    abstract fun getNameInputText() : String
    abstract fun getMaxChars() : Int

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            registerListener(parentFragment)
            val view = it.layoutInflater.inflate(R.layout.name_input_dialog, null)
            editText = view.findViewById(R.id.edit_name_input)
            editText.hint = getNameInputHint()
            editText.setText(getNameInputText())
            editText.setSelection(editText.text.length)
            editText.addTextChangedListener(this)
            editText.filters = arrayOf(InputFilter.LengthFilter(getMaxChars()))
            view.findViewById<TextView>(R.id.prompt_text).text = getTitleText()
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(getPositiveButtonName()) { _, _ -> onPositiveClicked(editText.text.toString()) }
                .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            alertDialog = builder.create()
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(requireContext(), R.color.secondaryColor))
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(requireContext(), R.color.toolBarTextColor))
            }
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
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = editText.text.isNotEmpty()
    }
}
