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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.databinding.ListItemMoveToGroupBinding
import com.samco.trackandgraph.databinding.MoveToGroupDialogBinding
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import kotlin.math.min

const val MOVE_DIALOG_TYPE_KEY = "move_dialog_type"
const val MOVE_DIALOG_GROUP_KEY = "move_dialog_group"
const val MOVE_DIALOG_TYPE_TRACK = "track"
const val MOVE_DIALOG_TYPE_GRAPH = "graph"

enum class MoveDialogType { TRACKER, GRAPH }

class MoveToDialogFragment : DialogFragment() {
    private val viewModel by viewModels<MoveToDialogViewModel>()
    private lateinit var binding: MoveToGroupDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
            else -> throw Exception("Unrecognised move dialog mode")
        }
        val id = requireArguments().getLong(MOVE_DIALOG_GROUP_KEY)
        viewModel.init(requireActivity(), mode, id)
        listenToViewModel()
    }

    private fun listenToViewModel() {
        viewModel.availableGroups.observe(this, Observer {
            if (it != null) inflateGroupItems(it)
        })

        viewModel.state.observe(this, Observer {
            if (it == MoveToDialogState.MOVED) dismiss()
        })
    }

    private fun inflateGroupItems(items: List<Group>) {
        val inflater = LayoutInflater.from(context)
        for (item in items) {
            val groupItemView = ListItemMoveToGroupBinding.inflate(inflater, binding.groupsLayout, false)
            groupItemView.groupNameText.text = item.name
            groupItemView.itemBackground.setOnClickListener { viewModel.moveTo(item.id) }
            binding.groupsLayout.addView(groupItemView.root)
        }
        setDialogHeight(items.size)
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

enum class MoveToDialogState { INITIALIZING, WAITING, MOVING, MOVED }

class MoveToDialogViewModel : ViewModel() {
    private lateinit var dao: TrackAndGraphDatabaseDao
    private lateinit var mode: MoveDialogType
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    val state: LiveData<MoveToDialogState> get() { return _state }
    private val _state = MutableLiveData<MoveToDialogState>(MoveToDialogState.INITIALIZING)

    val availableGroups: LiveData<List<Group>?> get() { return _availableGroups }
    private val _availableGroups = MutableLiveData<List<Group>?>(null)

    private lateinit var feature: Feature
    private lateinit var graphStat: GraphOrStat

    fun init(activity: Activity, mode: MoveDialogType, id: Long) {
        if (_state.value != MoveToDialogState.INITIALIZING) return
        this.mode = mode
        val application = activity.application
        dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        ioScope.launch {
            val groups = dao.getAllGroupsSync()
            if (mode == MoveDialogType.TRACKER) feature = dao.getFeatureById(id)
            else graphStat = dao.getGraphStatById(id)
            withContext(Dispatchers.Main) {
                _availableGroups.value = groups
                _state.value = MoveToDialogState.WAITING
            }
        }
    }

    fun moveTo(newGroupId: Long) = ioScope.launch {
        if (_state.value != MoveToDialogState.WAITING) return@launch
        withContext(Dispatchers.Main) { _state.value = MoveToDialogState.MOVING }
        if (mode == MoveDialogType.TRACKER) {
            val newFeature = feature.copy(groupId = newGroupId)
            dao.updateFeature(newFeature)
        } else {
            val newGraphStat = graphStat.copy(groupId = newGroupId)
            dao.updateGraphOrStat(newGraphStat)
        }
        withContext(Dispatchers.Main) { _state.value = MoveToDialogState.MOVED }
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
