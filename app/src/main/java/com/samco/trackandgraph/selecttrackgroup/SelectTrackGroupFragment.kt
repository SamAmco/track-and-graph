package com.samco.trackandgraph.selecttrackgroup

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackGroup
import com.samco.trackandgraph.ui.*
import kotlinx.coroutines.*

class SelectTrackGroupFragment : SelectGroupFragment() {
    override fun getViewModelImpl(): SelectGroupViewModel {
        return ViewModelProviders.of(this).get(SelectTrackGroupViewModel::class.java)
    }

    override fun getRenameDialogHint() = getString(R.string.track_group_name)

    override fun getDeleteDialogTitle() = getString(R.string.ru_sure_del_track_group)

    override fun getAddGroupHint() = getString(R.string.track_group_name)

    override fun getAddGroupTitle() = getString(R.string.add_track_group)

    override fun observeGroupDataAndUpdate(selectGroupViewModel: SelectGroupViewModel,
                                               adapter: GroupListAdapter) {
        super.observeGroupDataAndUpdate(selectGroupViewModel, adapter)
        val viewModel = (selectGroupViewModel as SelectTrackGroupViewModel)
        viewModel.trackGroups.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it.map { tg -> GroupItem(tg.id, tg.name, tg.displayIndex) }.toMutableList())
            }
        })
    }

    override fun onGroupSelected(groupItem: GroupItem) {
        navController?.navigate(
            SelectTrackGroupFragmentDirections
                .actionSelectTackGroup(groupItem.id, groupItem.name)
        )
    }
}

class SelectTrackGroupViewModel : SelectGroupViewModel() {
    lateinit var trackGroups: LiveData<List<TrackGroup>>

    override fun initViewModel(activity: Activity) {
        super.initViewModel(activity)
        trackGroups = dataSource!!.getTrackGroups()
    }

    override fun getNumGroups(): Int {
        return trackGroups.value?.size ?: 0
    }

    override fun deleteGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.deleteTrackGroup(toTG(groupItem))
        }
    }

    override fun addGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.insertTrackGroup(toTG(groupItem))
        }
    }

    override fun updateGroup(groupItem: GroupItem) {
        ioScope.launch {
            dataSource!!.updateTrackGroup(toTG(groupItem))
        }
    }

    override fun adjustDisplayIndexes(groupItems: List<GroupItem>) {
        ioScope.launch {
            trackGroups.value?.let { oldList ->
                val newList = groupItems.mapIndexed { i, gi ->
                    oldList.first { tg -> tg.id == gi.id }.copy(displayIndex = i)
                }
                dataSource!!.updateTrackGroups(newList)
            }
        }
    }

    private fun toTG(gi: GroupItem) = TrackGroup(gi.id, gi.name, gi.displayIndex)
}
