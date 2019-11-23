package com.samco.trackandgraph.selectgroup

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentSelectGroupBinding
import com.samco.trackandgraph.ui.RenameGroupDialogFragment
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import timber.log.Timber

enum class GroupItemType { TRACK, GRAPH }

data class GroupItem (val id: Long, val name: String, val displayIndex: Int, val type: GroupItemType)

class SelectGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener,
    RenameGroupDialogFragment.RenameGroupDialogListener
{
    private var navController: NavController? = null
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var viewModel: SelectGroupViewModel
    private lateinit var adapter: GroupListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(SelectGroupViewModel::class.java)
        viewModel.initViewModel(requireActivity())

        adapter = GroupListAdapter(
            GroupClickListener(
                this::onGroupSelected,
                this::onRenameClicked,
                this::onDeleteClicked
            ),
            getColor(context!!, R.color.primaryColor),
            getColor(context!!, R.color.secondaryColor)
        )
        binding.groupList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.groupList)
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeGroupDataAndUpdate(adapter)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity!!.toolbar.overflowIcon = getDrawable(context!!, R.drawable.add_icon)
    }

    override fun onPause() {
        super.onPause()
        activity!!.toolbar.overflowIcon = getDrawable(context!!, R.drawable.list_menu_icon)
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
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

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
    }

    private fun observeGroupDataAndUpdate(adapter: GroupListAdapter) {
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
        viewModel.allGroups.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
            if (it.isNullOrEmpty()) {
                binding.noGroupsHintText.text = getString(R.string.no_groups_hint)
                binding.noGroupsHintText.visibility = View.VISIBLE
            } else binding.noGroupsHintText.visibility = View.INVISIBLE
        })
    }

    private fun onRenameClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = RenameGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_group_dialog") }
    }

    override fun getGroupItem() = viewModel.currentActionGroupItem!!

    override fun onRenameGroupItem(groupItem: GroupItem) { viewModel.updateGroup(groupItem) }
    override fun getRenameDialogHintText() = getRenameDialogHint()
    private fun getRenameDialogHint() = getString(R.string.group_name)

    private fun onDeleteClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
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

    private fun onGroupSelected(groupItem: GroupItem) {
        when (groupItem.type) {
            GroupItemType.TRACK -> navController?.navigate(
                SelectGroupFragmentDirections.actionSelectTackGroup(groupItem.id, groupItem.name)
            )
            GroupItemType.GRAPH -> navController?.navigate(
                SelectGroupFragmentDirections.actionSelectGraphStatGroup(groupItem.id, groupItem.name)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.select_group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_track_group -> {
                viewModel.currentActionGroupType = GroupItemType.TRACK
                onAddClicked()
            }
            R.id.add_graph_group -> {
                viewModel.currentActionGroupType = GroupItemType.GRAPH
                onAddClicked()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onAddClicked() {
        val dialog = AddGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "add_group_dialog") }
    }

    override fun getAddGroupHintText() = getString(R.string.group_name)

    override fun getAddGroupTitleText() = when (viewModel.currentActionGroupType) {
        GroupItemType.TRACK -> getString(R.string.add_track_group)
        GroupItemType.GRAPH -> getString(R.string.add_graph_stat_group)
    }

    override fun getGroupNameMaxLength(): Int = MAX_GROUP_NAME_LENGTH

    override fun onAddGroup(name: String) {
        viewModel.addGroup(
            GroupItem(
                0,
                name,
                viewModel.allGroups.value?.size ?: 0,
                viewModel.currentActionGroupType
            )
        )
    }
}

class SelectGroupViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var dataSource: TrackAndGraphDatabaseDao? = null
    var currentActionGroupItem: GroupItem? = null
    var currentActionGroupType: GroupItemType = GroupItemType.TRACK

    private lateinit var trackGroups: LiveData<List<TrackGroup>>
    private lateinit var graphGroups: LiveData<List<GraphStatGroup>>
    val allGroups: LiveData<List<GroupItem>> get() { return _allGroups }
    private var _allGroups = MediatorLiveData<List<GroupItem>>()

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        trackGroups = dataSource!!.getTrackGroups()
        graphGroups = dataSource!!.getGraphStatGroups()
        _allGroups.addSource(trackGroups, this::onNewTrackGroupList)
        _allGroups.addSource(graphGroups, this::onNewGraphGroups)
    }

    private fun onNewGraphGroups(graphGroups: List<GraphStatGroup>) {
        val newList = _allGroups.value?.toMutableList() ?: mutableListOf()
        newList.removeAll { g -> g.type == GroupItemType.GRAPH }
        newList.addAll(graphGroups.map { gg -> GroupItem(gg.id, gg.name, gg.displayIndex, GroupItemType.GRAPH) })
        _allGroups.value = sortGroups(newList)
    }

    private fun onNewTrackGroupList(trackGroups: List<TrackGroup>) {
        val newList = _allGroups.value?.toMutableList() ?: mutableListOf()
        newList.removeAll { g -> g.type == GroupItemType.TRACK }
        newList.addAll(trackGroups.map { tg -> GroupItem(tg.id, tg.name, tg.displayIndex, GroupItemType.TRACK) })
        _allGroups.value = sortGroups(newList)
    }

    private fun sortGroups(groupItems: List<GroupItem>) =
        groupItems.sortedWith(Comparator { a, b -> a.displayIndex - b.displayIndex })

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    fun deleteGroup(groupItem: GroupItem) = when (groupItem.type) {
        GroupItemType.TRACK -> ioScope.launch {
            dataSource!!.deleteTrackGroup(toTG(groupItem))
        }
        GroupItemType.GRAPH -> ioScope.launch {
            dataSource!!.deleteGraphStatGroup(toGSG(groupItem))
        }
    }

    fun addGroup(groupItem: GroupItem) = when (groupItem.type) {
        GroupItemType.TRACK -> ioScope.launch {
            dataSource!!.insertTrackGroup(toTG(groupItem))
        }
        GroupItemType.GRAPH -> ioScope.launch {
            dataSource!!.insertGraphStatGroup(toGSG(groupItem))
        }
    }

    fun updateGroup(groupItem: GroupItem) = when (groupItem.type) {
        GroupItemType.TRACK -> ioScope.launch {
            dataSource!!.updateTrackGroup(toTG(groupItem))
        }
        GroupItemType.GRAPH -> ioScope.launch {
            dataSource!!.updateGraphStatGroup(toGSG(groupItem))
        }
    }

    fun adjustDisplayIndexes(groupItems: List<GroupItem>) {
        Timber.d(groupItems.joinToString { gi -> gi.name + gi.displayIndex })
        val newTrackGroups = mutableListOf<TrackGroup>()
        val newGraphGroups = mutableListOf<GraphStatGroup>()
        groupItems.forEachIndexed { i, groupItem ->
            when(groupItem.type) {
                GroupItemType.TRACK -> newTrackGroups.add(toTG(groupItem, i))
                GroupItemType.GRAPH -> newGraphGroups.add(toGSG(groupItem, i))
            }
        }

        Timber.d(newTrackGroups.joinToString { tg -> tg.name + tg.displayIndex })
        Timber.d(newGraphGroups.joinToString { gg -> gg.name + gg.displayIndex })

        ioScope.launch {
            dataSource!!.updateTrackGroups(newTrackGroups)
            dataSource!!.updateGraphStatGroups(newGraphGroups)
        }
    }

    private fun toTG(gi: GroupItem, di: Int) = TrackGroup.create(gi.id, gi.name, di)
    private fun toTG(gi: GroupItem) = TrackGroup.create(gi.id, gi.name, gi.displayIndex)
    private fun toGSG(gi: GroupItem, di: Int) = GraphStatGroup.create(gi.id, gi.name, di)
    private fun toGSG(gi: GroupItem) = GraphStatGroup.create(gi.id, gi.name, gi.displayIndex)
}
