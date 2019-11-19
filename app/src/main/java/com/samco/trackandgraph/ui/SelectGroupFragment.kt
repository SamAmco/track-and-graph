package com.samco.trackandgraph.ui

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.databinding.FragmentSelectGroupBinding
import kotlinx.coroutines.*

abstract class SelectGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener,
    RenameGroupDialogFragment.RenameGroupDialogListener
{
    protected var navController: NavController? = null
    protected lateinit var binding: FragmentSelectGroupBinding
    protected lateinit var viewModel: SelectGroupViewModel
    protected lateinit var adapter: GroupListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_select_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = getViewModelImpl()
        viewModel.initViewModel(requireActivity())

        adapter = GroupListAdapter(
            GroupClickListener(
                this::onGroupSelected,
                this::onRenameClicked,
                this::onDeleteClicked
            ),
            getCornerTabColor()
        )
        binding.groupList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.groupList)
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeGroupDataAndUpdate(viewModel, adapter)
        setHasOptionsMenu(true)
        return binding.root
    }

    abstract fun getCornerTabColor(): Int

    abstract fun getViewModelImpl(): SelectGroupViewModel

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

    protected open fun observeGroupDataAndUpdate(selectGroupViewModel: SelectGroupViewModel,
                                               adapter: GroupListAdapter) {
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
    }

    private fun onRenameClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = RenameGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_group_dialog") }
    }

    override fun getGroupItem() = viewModel.currentActionGroupItem!!

    override fun onRenameGroupItem(groupItem: GroupItem) = viewModel.updateGroup(groupItem)
    override fun getRenameDialogHintText() = getRenameDialogHint()
    abstract fun getRenameDialogHint(): String

    private fun onDeleteClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getDeleteDialogTitle())
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_group_fragment") }
    }

    abstract fun getDeleteDialogTitle(): String

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getDeleteDialogTitle() -> viewModel.currentActionGroupItem?.let {
                viewModel.deleteGroup(it)
            }
        }
    }

    abstract fun onGroupSelected(groupItem: GroupItem)

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
        childFragmentManager.let { dialog.show(it, "add_group_dialog") }
    }

    override fun getAddGroupHintText() = getAddGroupHint()
    abstract fun getAddGroupHint(): String

    override fun getAddGroupTitleText() = getAddGroupTitle()
    abstract fun getAddGroupTitle(): String

    override fun onAddGroup(name: String) {
        viewModel.addGroup(
            GroupItem(0, name, viewModel.getNumGroups())
        )
    }
}

abstract class SelectGroupViewModel : ViewModel() {
    private var updateJob = Job()
    protected val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    protected var dataSource: TrackAndGraphDatabaseDao? = null
    var currentActionGroupItem: GroupItem? = null

    open fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    abstract fun getNumGroups(): Int
    abstract fun deleteGroup(groupItem: GroupItem)
    abstract fun addGroup(groupItem: GroupItem)
    abstract fun updateGroup(groupItem: GroupItem)
    abstract fun adjustDisplayIndexes(groupItems: List<GroupItem>)
}
