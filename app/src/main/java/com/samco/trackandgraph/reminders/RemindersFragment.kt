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

package com.samco.trackandgraph.reminders

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.CheckedDays
import com.samco.trackandgraph.database.entity.Reminder
import com.samco.trackandgraph.databinding.RemindersFragmentBinding
import org.threeten.bp.LocalTime
import java.util.concurrent.Executors

const val REMINDERS_CHANNEL_ID = "reminder_notifications_channel"

class RemindersFragment : Fragment() {
    private lateinit var binding: RemindersFragmentBinding
    private val viewModel by viewModels<RemindersViewModel>()
    private lateinit var adapter: ReminderListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RemindersFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        adapter = ReminderListAdapter(
            ReminderClickListener(
                this::onDeleteClicked,
                this::onDaysChanged,
                this::onTimeChanged,
                this::onNameChanged,
                this::onHideKeyboard
            ), requireContext()
        )
        binding.remindersList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.remindersList)
        binding.remindersList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.remindersList)

        setHasOptionsMenu(true)

        createNotificationChannel()
        return binding.root
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.reminders_notifications_channel_name)
            val descriptionText = getString(R.string.reminders_notifications_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(REMINDERS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.initViewModel(requireActivity())
        listenToViewModel()
    }

    override fun onStop() {
        super.onStop()
        val syncRemindersIntent = Intent(requireActivity(), RecreateAlarms::class.java).apply {
            action = "action.REMINDERS_CHANGED"
        }
        requireContext().sendBroadcast(syncRemindersIntent)
    }

    private fun onHideKeyboard() {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            requireActivity().window.decorView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun onDeleteClicked(reminder: Reminder) {
        adapter.submitList(adapter.currentList.minus(reminder).toMutableList())
        viewModel.deleteReminder(reminder)
    }

    private fun onDaysChanged(reminder: Reminder, checkedDays: CheckedDays) {
        viewModel.daysChanged(reminder, checkedDays)
    }

    private fun onTimeChanged(reminder: Reminder, localTime: LocalTime) {
        viewModel.onTimeChanged(reminder, localTime)
    }

    private fun onNameChanged(reminder: Reminder, name: String) {
        viewModel.onNameChanged(reminder, name)
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
            if (viewHolder != null && viewHolder is ReminderViewHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.elevateCard()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as ReminderViewHolder).dropCard()
            viewModel.adjustDisplayIndexes(adapter.getItems())
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun listenToViewModel() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            if (it == RemindersViewModelState.WAITING) observeAndUpdateReminders()
        })
    }

    private fun observeAndUpdateReminders() {
        viewModel.allReminders.observe(viewLifecycleOwner, Observer {
            val displayIndexesAreTheSame =
                adapter.currentList.map { item -> item.displayIndex } == it.map { item -> item.displayIndex }

            if (adapter.currentList.size != it.size || !displayIndexesAreTheSame) {
                it?.let { adapter.submitList(it.toMutableList()) }
                if (it.isNullOrEmpty()) binding.noRemindersHintText.visibility = View.VISIBLE
                else binding.noRemindersHintText.visibility = View.GONE
            }
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
        viewModel.addReminder(getString(R.string.default_reminder_name))
        binding.remindersList.postDelayed(
            { binding.remindersList.smoothScrollToPosition(0) },
            105
        )
    }

    override fun onPause() {
        onHideKeyboard()
        super.onPause()
    }
}

enum class RemindersViewModelState { INITIALIZING, WAITING }
class RemindersViewModel : ViewModel() {
    //I use an executor here rather than kotlin co-routines because I want everything executed in the
    // order that it is called
    private val executor = Executors.newSingleThreadExecutor()
    private var dataSource: TrackAndGraphDatabaseDao? = null

    lateinit var allReminders: LiveData<List<Reminder>>
        private set

    val state: LiveData<RemindersViewModelState>
        get() {
            return _state
        }
    private val _state = MutableLiveData(RemindersViewModelState.INITIALIZING)

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource =
            TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        allReminders = dataSource!!.getAllReminders()
        _state.value = RemindersViewModelState.WAITING
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    fun addReminder(defaultName: String) {
        executor.submit {
            dataSource?.insertReminder(
                Reminder(
                    0,
                    0,
                    defaultName,
                    LocalTime.now(),
                    CheckedDays.none()
                )
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        executor.submit { dataSource?.deleteReminder(reminder) }
    }

    fun adjustDisplayIndexes(reminders: List<Reminder>) {
        executor.submit {
            val newList = allReminders.value?.let { oldList ->
                reminders.mapIndexed { i, r ->
                    oldList.firstOrNull { or -> or.id == r.id }?.copy(displayIndex = i)
                }.filterNotNull()
            }
            newList?.let {
                dataSource!!.updateReminders(it)
            }
        }
    }

    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) =
        updateReminder(reminder) { it.copy(checkedDays = checkedDays) }

    fun onTimeChanged(reminder: Reminder, localTime: LocalTime) =
        updateReminder(reminder) { it.copy(time = localTime) }

    fun onNameChanged(reminder: Reminder, name: String) =
        updateReminder(reminder) { it.copy(alarmName = name) }

    private fun updateReminder(reminder: Reminder, onFound: (Reminder) -> Reminder) {
        executor.submit {
            allReminders.value?.firstOrNull { it.id == reminder.id }?.let {
                val newReminder = onFound(it)
                dataSource?.updateReminder(newReminder)
            }
        }
    }
}
