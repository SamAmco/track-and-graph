package com.samco.trackandgraph.selectgraphstatgroup

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.GraphStatGroup
import com.samco.trackandgraph.databinding.FragmentSelectGroupBinding
import com.samco.trackandgraph.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SelectGraphStatGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener,
    RenameGroupDialogFragment.RenameGroupDialogListener
{
    private var navController: NavController? = null
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var viewModel: SelectGraphStatGroupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(SelectGraphStatGroupViewModel::class.java)
        viewModel.initViewModel(requireActivity())

        val adapter = GroupListAdapter(
            GroupClickListener(
                this::onGraphStatGroupSelected,
                this::onRenameClicked,
                this::onDeleteClicked
            )
        )
        binding.groupList.adapter = adapter
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeGraphStatGroupDataAndUpdate(adapter)
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun observeGraphStatGroupDataAndUpdate(adapter: GroupListAdapter) {
        viewModel.graphStatGroups.observe(viewLifecycleOwner, Observer {
            it?.let {
                //TODO implement display index here
                adapter.submitList(it.map { gsg -> GroupItem(gsg.id, gsg.name, 0) })
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
    }

    private fun groupItemToGraphStatGroup(groupItem: GroupItem) = GraphStatGroup(groupItem.id, groupItem.name)

    private fun onGraphStatGroupSelected(groupItem: GroupItem) {
        navController?.navigate(
            SelectGraphStatGroupFragmentDirections.actionSelectGraphStatGroup(groupItem.id)
        )
    }

    override fun getGroupItem() = viewModel.currentActionGroupItem!!

    private fun onRenameClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = RenameGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_graph_stat_group_dialog") }
    }

    override fun onRenameGroupItem(groupItem: GroupItem) {
        viewModel.updateGraphStatGroup(groupItemToGraphStatGroup(groupItem))
    }

    override fun getRenameDialogHintText() = getString(R.string.graph_stat_group_name)

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_graph_stat_group) -> viewModel.currentActionGroupItem?.let {
                viewModel.deleteGraphStatGroup(groupItemToGraphStatGroup(it))
            }
        }
    }

    private fun onDeleteClicked(groupItem: GroupItem) {
        viewModel.currentActionGroupItem = groupItem
        val dialog = YesCancelDialogFragment()
        var args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_graph_stat_group))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_graph_stat_group_fragment") }
    }

    private fun onAddClicked() {
        val dialog = AddGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "add_graph_stat_group_dialog") }
    }

    override fun onAddGroup(name: String) { viewModel.addGraphStatGroup(GraphStatGroup(0L, name)) }

    override fun getAddGroupHintText() = getString(R.string.graph_stat_group_name)

    override fun getAddGroupTitleText() = getString(R.string.add_graph_stat_group)

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
}

class SelectGraphStatGroupViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var dataSource: TrackAndGraphDatabaseDao? = null
    lateinit var graphStatGroups: LiveData<List<GraphStatGroup>>

    var currentActionGroupItem: GroupItem? = null

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        graphStatGroups = dataSource!!.getGraphStatGroups()
    }

    fun deleteGraphStatGroup(graphStatGroup: GraphStatGroup) = ioScope.launch {
        dataSource!!.deleteGraphStatGroup(graphStatGroup)
    }

    fun updateGraphStatGroup(graphStatGroup: GraphStatGroup) = ioScope.launch {
        dataSource!!.updateGraphStatGroup(graphStatGroup)
    }

    fun addGraphStatGroup(graphStatGroup: GraphStatGroup) = ioScope.launch {
        dataSource!!.insertGraphStatGroup(graphStatGroup)
    }
}
