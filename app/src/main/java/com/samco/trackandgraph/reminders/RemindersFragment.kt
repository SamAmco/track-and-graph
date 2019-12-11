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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.RemindersFragmentBinding
import kotlinx.coroutines.*
import org.threeten.bp.LocalTime

class RemindersFragment : Fragment() {
    private lateinit var binding: RemindersFragmentBinding
    private lateinit var viewModel: RemindersViewModel
    private lateinit var adapter: ReminderListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = RemindersFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(RemindersViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        listenToViewModel()

        adapter = ReminderListAdapter(ReminderClickListener())
        binding.remindersList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.remindersList)
        binding.remindersList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.remindersList)

        setHasOptionsMenu(true)
        return binding.root
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
            if (viewHolder != null && viewHolder is ReminderViewHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.elevateCard()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as ReminderViewHolder).dropCard()
            viewModel.adjustDisplayIndexes(adapter.getItems())
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
    }

    private fun listenToViewModel() {
        viewModel.state.observe(this, Observer {
            if (it == RemindersViewModelState.WAITING) observeAndUpdateReminders()
        })
    }

    private fun observeAndUpdateReminders() {
        viewModel.allReminders.observe(this, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
            if (it.isNullOrEmpty()) binding.noRemindersHintText.visibility = View.VISIBLE
            else binding.noRemindersHintText.visibility = View.GONE
        })
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reminders_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_reminder) onAddClicked()
        return super.onOptionsItemSelected(item)
    }

    private fun onAddClicked() {
        viewModel.addReminder()
    }
}

enum class RemindersViewModelState { INITIALIZING, WAITING }
class RemindersViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)
    private var dataSource: TrackAndGraphDatabaseDao? = null

    lateinit var allReminders: LiveData<List<Reminder>>
        private set

    val state: LiveData<RemindersViewModelState> get() { return _state }
    private val _state = MutableLiveData<RemindersViewModelState>(RemindersViewModelState.INITIALIZING)

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allReminders = dataSource!!.getAllReminders()
        _state.value = RemindersViewModelState.WAITING
    }

    fun addReminder() = ioScope.launch {
        dataSource?.insertReminder(
            Reminder(0, 0, "", LocalTime.now(), CheckedDays.none())
        )
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

    fun adjustDisplayIndexes(reminders: List<Reminder>) {}
}
