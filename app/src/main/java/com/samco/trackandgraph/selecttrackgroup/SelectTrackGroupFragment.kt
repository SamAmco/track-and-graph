package com.samco.trackandgraph.selecttrackgroup

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.TrackGroup
import com.samco.trackandgraph.databinding.FragmentSelectGroupBinding
import com.samco.trackandgraph.ui.*
import kotlinx.coroutines.*

class SelectTrackGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener,
    RenameGroupDialogFragment.RenameGroupDialogListener
{
    private var navController: NavController? = null
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var viewModel: SelectTrackGroupViewModel
    private lateinit var adapter: GroupListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_select_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(SelectTrackGroupViewModel::class.java)
        viewModel.initViewModel(requireActivity())

        adapter = GroupListAdapter(
            GroupClickListener(
                this::onTrackGroupSelected,
                this::onRenameClicked,
                this::onDeleteClicked
            )
        )
        binding.groupList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.groupList)
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeTrackGroupDataAndUpdate(viewModel, adapter)
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ACTION_STATE_DRAG, UP or DOWN)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (viewHolder != null && viewHolder is GroupViewHolder && actionState == ACTION_STATE_DRAG) {
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

    private fun observeTrackGroupDataAndUpdate(selectTrackGroupViewModel: SelectTrackGroupViewModel,
                                               adapter: GroupListAdapter) {
        selectTrackGroupViewModel.trackGroups.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it.map { tg -> GroupItem(tg.id, tg.name, tg.displayIndex) })
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
    }

    private fun onRenameClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = RenameGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_track_group_dialog") }
    }

    override fun getGroupItem() = viewModel.currentActionGroupItem!!

    override fun onRenameGroupItem(groupItem: GroupItem) {
        viewModel.updateTrackGroup(groupItemToTrackGroup(groupItem))
    }

    override fun getRenameDialogHintText() = getString(R.string.track_group_name)

    private fun groupItemToTrackGroup(groupItem: GroupItem): TrackGroup {
        return TrackGroup(groupItem.id, groupItem.name, groupItem.displayIndex)
    }

    private fun onDeleteClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = YesCancelDialogFragment()
        var args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_track_group))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_track_group_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_track_group) -> viewModel.currentActionGroupItem?.let {
                viewModel.deleteTrackGroup(groupItemToTrackGroup(it))
            }
        }
    }

    private fun onTrackGroupSelected(groupItem: GroupItem) {
        navController?.navigate(
            SelectTrackGroupFragmentDirections
                .actionSelectTackGroup(groupItem.id, groupItem.name)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.select_group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> onAddClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onAddClicked() {
        val dialog = AddGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "add_track_group_dialog") }
    }

    override fun getAddGroupHintText() = getString(R.string.track_group_name)

    override fun getAddGroupTitleText() = getString(R.string.add_track_group)

    override fun onAddGroup(name: String) {
        viewModel.addTrackGroup(
            TrackGroup(0, name, viewModel.trackGroups.value?.size ?: 0)
        )
    }
}

class SelectTrackGroupViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var dataSource: TrackAndGraphDatabaseDao? = null
    var currentActionGroupItem: GroupItem? = null
    lateinit var trackGroups: LiveData<List<TrackGroup>>

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        trackGroups = dataSource!!.getTrackGroups()
    }

    //TODO check all view models to make sure they do this
    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    fun deleteTrackGroup(trackGroup: TrackGroup) = ioScope.launch {
        dataSource!!.deleteTrackGroup(trackGroup)
    }

    fun addTrackGroup(trackGroup: TrackGroup) = ioScope.launch {
        dataSource!!.insertTrackGroup(trackGroup)
    }

    fun updateTrackGroup(trackGroup: TrackGroup) = ioScope.launch {
        dataSource!!.updateTrackGroup(trackGroup)
    }

    fun adjustDisplayIndexes(groupItems: List<GroupItem>) = ioScope.launch {
        trackGroups.value?.let { oldList ->
            val newList = groupItems.mapIndexed { i, gi ->
                oldList.first { tg -> tg.id == gi.id }.copy(displayIndex = i)
            }
            dataSource!!.updateTrackGroups(newList)
        }
    }
}
