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

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat.getDrawable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.databinding.FragmentSelectGroupBinding
import com.samco.trackandgraph.ui.RenameGroupDialogFragment
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class SelectGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener,
    RenameGroupDialogFragment.RenameGroupDialogListener {
    private var navController: NavController? = null
    private lateinit var binding: FragmentSelectGroupBinding
    private val viewModel by viewModels<SelectGroupViewModel>()
    private lateinit var adapter: GroupListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.navController = container?.findNavController()
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.initViewModel(requireActivity())
        listenToViewModel()

        binding.noGroupsHintText.visibility = View.INVISIBLE

        adapter = GroupListAdapter(
            GroupClickListener(
                this::onGroupSelected,
                this::onRenameClicked,
                this::onDeleteClicked
            ),
            binding.groupList.context.getColorFromAttr(R.attr.colorPrimary),
            binding.groupList.context.getColorFromAttr(R.attr.colorSecondary)
        )
        binding.groupList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.groupList)
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeGroupDataAndUpdate(adapter)
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun listenToViewModel() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            if (it == SelectGroupViewModelState.CREATED_FIRST_GROUP) {
                try {
                    navController?.navigate(
                        SelectGroupFragmentDirections.actionSelectTackGroup(
                            viewModel.firstTrackGroupId, getString(R.string.my_first_track_group)
                        )
                    )
                } catch (e: Exception) {
                }
            } else if (it == SelectGroupViewModelState.TRACK_GROUP_SELECTED) {
                SelectGroupFragmentDirections.actionSelectTackGroup(
                    viewModel.currentActionGroupItem!!.id,
                    viewModel.currentActionGroupItem!!.name
                )
            } else if (it == SelectGroupViewModelState.GRAPH_GROUP_SELECTED) {
                SelectGroupFragmentDirections.actionSelectGraphStatGroup(
                    viewModel.currentActionGroupItem!!.id,
                    viewModel.currentActionGroupItem!!.name
                )
            }
        })
    }

    override fun onStart() {
        super.onStart()
        requireActivity().toolbar.overflowIcon = getDrawable(requireContext(), R.drawable.add_icon)
    }

    override fun onStop() {
        super.onStop()
        if (navController?.currentDestination?.id != R.id.selectGroupFragment) {
            requireActivity().toolbar.overflowIcon =
                getDrawable(requireContext(), R.drawable.list_menu_icon)
        }
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeFlag(
                ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (viewHolder != null && viewHolder is GroupViewHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.elevateCard()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as GroupViewHolder).dropCard()
            viewModel.adjustDisplayIndexes(adapter.getItems())
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun observeGroupDataAndUpdate(adapter: GroupListAdapter) {
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
        viewModel.allGroups.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
            if (it.isNullOrEmpty()) {
                setHasOptionsMenu(false)
                binding.noGroupsHintText.visibility = View.VISIBLE
            } else binding.noGroupsHintText.visibility = View.INVISIBLE
        })
    }

    private fun onRenameClicked(group: Group) {
        viewModel.currentActionGroupItem = group
        val dialog = RenameGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_group_dialog") }
    }

    override fun getGroupItem() = viewModel.currentActionGroupItem!!

    override fun onRenameGroup(group: Group) {
        viewModel.updateGroup(group)
    }

    override fun getRenameDialogHintText() = getRenameDialogHint()
    private fun getRenameDialogHint() = getString(R.string.group_name)

    private fun onDeleteClicked(group: Group) {
        viewModel.currentActionGroupItem = group
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getDeleteDialogTitle())
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_group_fragment") }
    }

    private fun getDeleteDialogTitle() = getString(R.string.ru_sure_del_group)

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getDeleteDialogTitle() -> viewModel.currentActionGroupItem?.let {
                viewModel.deleteGroup(it)
            }
        }
    }

    private fun onGroupSelected(group: Group) {
        viewModel.currentActionGroupItem = group
        viewModel.onGroupSelected(group)
        //when (groupItem.type) {
        //    GroupItemType.TRACK -> navController?.navigate(
        //        SelectGroupFragmentDirections.actionSelectTackGroup(groupItem.id, groupItem.name)
        //    )
        //    GroupItemType.GRAPH -> navController?.navigate(
        //        SelectGroupFragmentDirections.actionSelectGraphStatGroup(groupItem.id, groupItem.name)
        //    )
        //}
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.select_group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //when (item.itemId) {
        //    R.id.add_track_group -> {
        //        viewModel.currentActionGroupType = GroupItemType.TRACK
        //        onAddClicked()
        //    }
        //    R.id.add_graph_group -> {
        //        viewModel.currentActionGroupType = GroupItemType.GRAPH
        //        onAddClicked()
        //    }
        //}
        return super.onOptionsItemSelected(item)
    }

    private fun onAddClicked() {
        val dialog = AddGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "add_group_dialog") }
    }

    override fun getAddGroupHintText() = getString(R.string.group_name)

    override fun getAddGroupTitleText() = getString(R.string.add_group)

    override fun onAddGroup(name: String) {
        viewModel.addGroup(
            Group(
                0,
                name,
                viewModel.allGroups.value?.size ?: 0,
                null,//TODO implement these
                0
            )
        )
    }
}

enum class SelectGroupViewModelState { INITIALIZING, WAITING, CREATED_FIRST_GROUP, TRACK_GROUP_SELECTED, GRAPH_GROUP_SELECTED }
class SelectGroupViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)
    private var dataSource: TrackAndGraphDatabaseDao? = null

    var currentActionGroupItem: Group? = null

    var firstTrackGroupId: Long = -1
        private set

    lateinit var allGroups: LiveData<List<Group>>
        private set

    val state: LiveData<SelectGroupViewModelState>
        get() {
            return _state
        }
    private val _state = MutableLiveData(SelectGroupViewModelState.INITIALIZING)

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource =
            TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allGroups = dataSource!!.getAllGroups()
        _state.value = SelectGroupViewModelState.WAITING
    }

    fun onGroupSelected(group: Group) {
        ioScope.launch {
            val graphs = dataSource?.getGraphsAndStatsByGroupId(group.id)?.value?.size ?: 0
            if (graphs > 0) _state.value = SelectGroupViewModelState.GRAPH_GROUP_SELECTED
            else _state.value = SelectGroupViewModelState.TRACK_GROUP_SELECTED
        }
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    fun deleteGroup(groupItem: Group) = dataSource!!.deleteGroup(groupItem)

    fun addGroup(group: Group) = ioScope.launch { dataSource!!.insertGroup(group) }

    fun updateGroup(group: Group) = ioScope.launch { dataSource!!.updateGroup(group) }

    fun adjustDisplayIndexes(groups: List<Group>) {
        val newGroups = groups.mapIndexed { i, group -> group.copy(displayIndex = i) }
        ioScope.launch { dataSource!!.updateGroups(newGroups) }
    }
}
