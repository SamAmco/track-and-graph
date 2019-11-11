package com.samco.trackandgraph.selectgraphstatgroup

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.GraphStatGroup
import com.samco.trackandgraph.database.MAX_GRAPH_STAT_GROUP_NAME_LENGTH
import com.samco.trackandgraph.ui.*
import kotlinx.coroutines.launch

class SelectGraphStatGroupFragment : SelectGroupFragment() {
    override fun getCornerTabColor(): Int = getColor(context!!, R.color.secondaryColor)

    override fun getViewModelImpl(): SelectGroupViewModel {
        return ViewModelProviders.of(this).get(SelectGraphStatGroupViewModel::class.java)
    }

    override fun getRenameDialogHint() = getString(R.string.graph_stat_group_name)

    override fun getDeleteDialogTitle() = getString(R.string.ru_sure_del_graph_stat_group)

    override fun onGroupSelected(groupItem: GroupItem) {
        navController?.navigate(
            SelectGraphStatGroupFragmentDirections.actionSelectGraphStatGroup(groupItem.id)
        )
    }

    override fun getGroupNameMaxLength(): Int = MAX_GRAPH_STAT_GROUP_NAME_LENGTH

    override fun getAddGroupHint() = getString(R.string.graph_stat_group_name)

    override fun getAddGroupTitle() = getString(R.string.add_graph_stat_group)

    override fun observeGroupDataAndUpdate(selectGroupViewModel: SelectGroupViewModel, adapter: GroupListAdapter) {
        super.observeGroupDataAndUpdate(selectGroupViewModel, adapter)
        val viewModel = selectGroupViewModel as SelectGraphStatGroupViewModel
        viewModel.graphStatGroups.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it.map { gsg -> GroupItem(gsg.id, gsg.name, gsg.displayIndex) }.toMutableList())
            }
            if (it.isNullOrEmpty()) {
                binding.noGroupsHintText.text = getString(R.string.no_graph_stat_groups_hint)
                binding.noGroupsHintText.visibility = View.VISIBLE
            } else binding.noGroupsHintText.visibility = View.INVISIBLE
        })
    }
}

class SelectGraphStatGroupViewModel : SelectGroupViewModel() {
    lateinit var graphStatGroups: LiveData<List<GraphStatGroup>>

    override fun initViewModel(activity: Activity) {
        super.initViewModel(activity)
        graphStatGroups = dataSource!!.getGraphStatGroups()
    }

    override fun getNumGroups(): Int {
        return graphStatGroups.value?.size ?: 0
    }

    override fun deleteGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.deleteGraphStatGroup(toGSG(groupItem))
        }
    }

    override fun addGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.insertGraphStatGroup(toGSG(groupItem))
        }
    }

    override fun updateGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.updateGraphStatGroup(toGSG(groupItem))
        }
    }

    override fun adjustDisplayIndexes(groupItems: List<GroupItem>) {
        ioScope.launch {
            graphStatGroups.value?.let { oldList ->
                val newList = groupItems.mapIndexed { i, gi ->
                    oldList.first { tg -> tg.id == gi.id }.copy(displayIndex = i)
                }
                dataSource!!.updateGraphStatGroups(newList)
            }
        }
    }

    private fun toGSG(gi: GroupItem) = GraphStatGroup.create(gi.id, gi.name, gi.displayIndex)
}
