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
package com.samco.trackandgraph.selectgroup

import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.NameInputDialogFragment


class AddGroupDialogFragment : NameInputDialogFragment() {

    private lateinit var listener: AddGroupDialogListener

    interface AddGroupDialogListener {
        fun onAddGroup(name: String)
        fun getAddGroupHintText(): String
        fun getAddGroupTitleText(): String
        fun getGroupNameMaxLength(): Int
    }

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as AddGroupDialogListener
    }

    override fun getMaxChars(): Int = listener.getGroupNameMaxLength()

    override fun getPositiveButtonName(): String = getString(R.string.add)

    override fun onPositiveClicked(name: String) = listener.onAddGroup(name)

    override fun getNameInputHint(): String = listener.getAddGroupHintText()

    override fun getTitleText(): String = listener.getAddGroupTitleText()

    override fun getNameInputText(): String = ""
}