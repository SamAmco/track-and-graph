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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.CheckedDays
import com.samco.trackandgraph.base.database.dto.Reminder
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.databinding.RemindersFragmentBinding
import com.samco.trackandgraph.di.IODispatcher
import com.samco.trackandgraph.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.LocalTime
import javax.inject.Inject

const val REMINDERS_CHANNEL_ID = "reminder_notifications_channel"

@AndroidEntryPoint
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

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.MENU,
            getString(R.string.reminders)
        )
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
        observeAndUpdateReminders()
    }

    override fun onStop() {
        super.onStop()
        val syncRemindersIntent = Intent(requireActivity(), RecreateAlarms::class.java).apply {
            action = "action.REMINDERS_CHANGED"
        }
        requireContext().sendBroadcast(syncRemindersIntent)
    }

    private fun onHideKeyboard() = requireActivity().window.hideKeyboard()

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
            adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
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

    private fun observeAndUpdateReminders() {
        viewModel.allReminders.observe(viewLifecycleOwner) {
            val displayIndexesAreTheSame =
                adapter.currentList.map { item -> item.displayIndex } == it.map { item -> item.displayIndex }

            if (adapter.currentList.size != it.size || !displayIndexesAreTheSame) {
                it?.let { adapter.submitList(it.toMutableList()) }
                if (it.isNullOrEmpty()) binding.noRemindersHintText.visibility = View.VISIBLE
                else binding.noRemindersHintText.visibility = View.GONE
            }
        }
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

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    val allReminders: LiveData<List<Reminder>> = dataInteractor.getAllReminders()

    private sealed class ReminderEvent {
        data class Insert(val reminder: Reminder) : ReminderEvent()
        data class Update(val reminder: Reminder) : ReminderEvent()
        data class Delete(val reminder: Reminder) : ReminderEvent()
        data class Order(val reminders: List<Reminder>) : ReminderEvent()
    }

    private val eventFlow = MutableSharedFlow<ReminderEvent>(extraBufferCapacity = 100)

    init {
        viewModelScope.launch(io) {
            eventFlow.collect {
                when (it) {
                    is ReminderEvent.Insert -> dataInteractor.insertReminder(it.reminder)
                    is ReminderEvent.Delete -> dataInteractor.deleteReminder(it.reminder)
                    is ReminderEvent.Update -> dataInteractor.updateReminder(it.reminder)
                    is ReminderEvent.Order -> dataInteractor.updateReminders(it.reminders)
                }
            }
        }
    }

    fun addReminder(defaultName: String) {
        eventFlow.tryEmit(
            ReminderEvent.Insert(
                Reminder(
                    0,
                    0,
                    defaultName,
                    LocalTime.now(),
                    CheckedDays.none()
                )
            )
        )
    }

    fun deleteReminder(reminder: Reminder) {
        eventFlow.tryEmit(ReminderEvent.Delete(reminder))
    }

    fun adjustDisplayIndexes(reminders: List<Reminder>) {
        val newList = allReminders.value?.let { oldList ->
            reminders.mapIndexed { i, r ->
                oldList.firstOrNull { or -> or.id == r.id }?.copy(displayIndex = i)
            }.filterNotNull()
        }
        newList?.let { eventFlow.tryEmit(ReminderEvent.Order(it)) }
    }

    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) =
        updateReminder(reminder) { it.copy(checkedDays = checkedDays) }

    fun onTimeChanged(reminder: Reminder, localTime: LocalTime) =
        updateReminder(reminder) { it.copy(time = localTime) }

    fun onNameChanged(reminder: Reminder, name: String) =
        updateReminder(reminder) { it.copy(alarmName = name) }

    private fun updateReminder(reminder: Reminder, onFound: (Reminder) -> Reminder) {
        allReminders.value?.firstOrNull { it.id == reminder.id }?.let {
            val newReminder = onFound(it)
            eventFlow.tryEmit(ReminderEvent.Update(newReminder))
        }
    }
}
