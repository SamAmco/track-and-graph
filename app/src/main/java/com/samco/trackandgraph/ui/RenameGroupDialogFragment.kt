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

import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.dto.GroupItem

class RenameGroupDialogFragment : NameInputDialogFragment() {
    private lateinit var listener: RenameGroupDialogListener
    private lateinit var groupItem: GroupItem

    interface RenameGroupDialogListener {
        fun getGroupItem() : GroupItem
        fun onRenameGroupItem(groupItem: GroupItem)
        fun getRenameDialogHintText(): String
    }

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as RenameGroupDialogListener
        groupItem = listener.getGroupItem()
    }

    override fun getPositiveButtonName(): String {
        return getString(R.string.rename)
    }

    override fun onPositiveClicked(name: String) {
        val newGroupItem = GroupItem(
            groupItem.id,
            name,
            groupItem.displayIndex,
            groupItem.type
        )
        listener.onRenameGroupItem(newGroupItem)
    }

    override fun getNameInputHint() = listener.getRenameDialogHintText()

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = groupItem.name
}