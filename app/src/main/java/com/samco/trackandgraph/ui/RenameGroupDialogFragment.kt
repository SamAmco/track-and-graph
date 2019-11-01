package com.samco.trackandgraph.ui

import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R

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
        val newGroupItem = GroupItem(groupItem.id, name)
        listener.onRenameGroupItem(newGroupItem)
    }

    override fun getNameInputHint() = listener.getRenameDialogHintText()

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = groupItem.name
}