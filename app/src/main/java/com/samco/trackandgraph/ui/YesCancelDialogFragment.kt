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
import androidx.fragment.app.DialogFragment
import com.samco.trackandgraph.R

class YesCancelDialogFragment : DialogFragment() {
    lateinit var title: String

    interface YesCancelDialogListener {
        fun onDialogYes(dialog: YesCancelDialogFragment, id: String?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        title = arguments?.getString("title") ?: ""
        val id = arguments?.getString("id")
        return activity?.let {
            val listener = parentFragment as YesCancelDialogListener
            AlertDialog.Builder(it, R.style.AppTheme_AlertDialogTheme).apply {
                setMessage(title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.yes) { _, _ -> listener.onDialogYes(this@YesCancelDialogFragment, id) }
                    .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}