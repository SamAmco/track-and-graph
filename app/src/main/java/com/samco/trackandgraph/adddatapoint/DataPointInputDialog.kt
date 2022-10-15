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
package com.samco.trackandgraph.adddatapoint

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

const val TRACKER_LIST_KEY = "TRACKER_LIST_KEY"
const val DATA_POINT_TIMESTAMP_KEY = "DATA_POINT_ID"
const val DURATION_SECONDS_KEY = "DURATION_SECONDS_KEY"

@AndroidEntryPoint
open class DataPointInputDialog : DialogFragment() {
    private val viewModel: AddDataPointsViewModel by viewModels<AddDataPointsViewModelImpl>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val timestampStr = requireArguments().getString(DATA_POINT_TIMESTAMP_KEY)
        val timestamp = if (timestampStr != null) odtFromString(timestampStr) else null
        val duration = requireArguments().getLong(DURATION_SECONDS_KEY).let {
            if (it > 0) it.toDouble() else null
        }
        viewModel.initFromArgs(
            requireArguments().getLongArray(TRACKER_LIST_KEY)!!.toList(),
            timestamp,
            duration
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.setCanceledOnTouchOutside(true)
        return ComposeView(requireContext()).apply {
            setContent {
                TnGComposeTheme {
                    AddDataPointsView(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().currentFocus?.clearFocus()
        requireActivity().window?.hideKeyboard()
    }
}