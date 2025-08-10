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
package com.samco.trackandgraph.selectitemdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.ListItemMoveToGroupBinding
import com.samco.trackandgraph.databinding.MoveToGroupDialogBinding
import com.samco.trackandgraph.util.GroupPathProvider
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.math.min

const val MOVE_DIALOG_TYPE_KEY = "move_dialog_type"
const val MOVE_DIALOG_GROUP_KEY = "move_dialog_group"
const val MOVE_DIALOG_TYPE_TRACK = "track"
const val MOVE_DIALOG_TYPE_GRAPH = "graph"
const val MOVE_DIALOG_TYPE_GROUP = "group"

enum class MoveDialogType { TRACKER, GRAPH, GROUP }

@AndroidEntryPoint
class MoveToDialogFragment : DialogFragment() {
    private val viewModel by viewModels<MoveToDialogViewModel>()
    private var binding: MoveToGroupDialogBinding by bindingForViewLifecycle()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return activity?.let {
            initViewModel()
            binding = MoveToGroupDialogBinding.inflate(inflater, container, false)
            binding.cancelButton.setOnClickListener { dismiss() }
            dialog?.setCanceledOnTouchOutside(true)
            binding.root
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("setting layout on resume")
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initViewModel() {
        val mode = when (requireArguments().getString(MOVE_DIALOG_TYPE_KEY)) {
            MOVE_DIALOG_TYPE_TRACK -> MoveDialogType.TRACKER
            MOVE_DIALOG_TYPE_GRAPH -> MoveDialogType.GRAPH
            MOVE_DIALOG_TYPE_GROUP -> MoveDialogType.GROUP
            else -> throw Exception("Unrecognised move dialog mode")
        }
        val id = requireArguments().getLong(MOVE_DIALOG_GROUP_KEY)
        viewModel.init(mode, id)
        listenToViewModel()
    }

    private fun listenToViewModel() {
        viewModel.availableGroups.observe(viewLifecycleOwner) {
            inflateGroupItems(it)
        }

        viewModel.state.observe(viewLifecycleOwner) {
            if (it == MoveToDialogState.MOVED) dismiss()
        }
    }

    private fun inflateGroupItems(groupPathProvider: GroupPathProvider) {
        for (item in groupPathProvider.filteredSortedGroups) {
            val groupItemView =
                ListItemMoveToGroupBinding.inflate(layoutInflater, binding.groupsLayout, false)
            groupItemView.groupNameText.text = item.path
            groupItemView.itemBackground.setOnClickListener { viewModel.moveTo(item.group.id) }
            binding.groupsLayout.addView(groupItemView.root)
        }
        setDialogHeight(groupPathProvider.groups.size)
    }

    private fun setDialogHeight(numItems: Int) {
        val itemSize = resources.getDimensionPixelSize(R.dimen.list_item_group_item_height)
        val baseHeight = resources.getDimensionPixelSize(R.dimen.move_to_dialog_base_height)
        val maxSize = resources.getDimensionPixelSize(R.dimen.move_to_dialog_max_height)
        val height = min(baseHeight + (itemSize * numItems), maxSize)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            height
        )
        binding.moveToDialogRoot.layoutParams = params
    }
}
