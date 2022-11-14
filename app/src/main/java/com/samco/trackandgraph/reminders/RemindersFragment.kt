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

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.CheckedDays
import com.samco.trackandgraph.base.database.dto.Reminder
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.databinding.RemindersFragmentBinding
import com.samco.trackandgraph.permissions.PermissionRequesterUseCase
import com.samco.trackandgraph.permissions.PermissionRequesterUseCaseImpl
import com.samco.trackandgraph.util.bindingForViewLifecycle
import com.samco.trackandgraph.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class RemindersFragment : Fragment(),
    PermissionRequesterUseCase by PermissionRequesterUseCaseImpl() {
    private var binding: RemindersFragmentBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<RemindersViewModel>()
    private lateinit var adapter: ReminderListAdapter

    init { initNotificationsPermissionRequester(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RemindersFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        adapter = ReminderListAdapter(
            ReminderClickListener(
                viewModel::deleteReminder,
                viewModel::daysChanged,
                viewModel::onTimeChanged,
                viewModel::onNameChanged,
                this::onHideKeyboard
            )
        )
        binding.remindersList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.remindersList)
        binding.remindersList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.remindersList.itemAnimator = DefaultItemAnimator()
        registerForContextMenu(binding.remindersList)

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.MENU,
            getString(R.string.reminders)
        )
    }

    override fun onStart() {
        super.onStart()
        observeAndUpdateReminders()
        observeSaveChangesButton()
        observeLoading()
    }

    private fun observeLoading() {
        viewModel.loading.observe(viewLifecycleOwner) {
            binding.loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun observeSaveChangesButton() {
        binding.saveChangesButton.setOnClickListener { viewModel.saveChanges() }
        viewModel.remindersChanged.observe(viewLifecycleOwner) {
            binding.saveChangesButton.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun onHideKeyboard() = requireActivity().window.hideKeyboard()

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
        viewModel.currentReminders.observe(viewLifecycleOwner) {

            if (it.isEmpty()) binding.noRemindersHintText.visibility = View.VISIBLE
            else binding.noRemindersHintText.visibility = View.GONE

            adapter.submitList(it)
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
        viewModel.currentReminders.value?.size?.let {
            binding.remindersList.smoothScrollToPosition(it)
        }
        requestAlarmAndNotificationPermission(requireContext())
    }

    override fun onPause() {
        onHideKeyboard()
        super.onPause()
    }
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _currentReminders = MutableLiveData<List<Reminder>>(emptyList())
    val currentReminders: LiveData<List<Reminder>> = _currentReminders

    private var savedReminders: List<Reminder> = emptyList()

    private val _remindersChanged = MutableLiveData(false)
    private val _loading = MutableLiveData(true)

    val remindersChanged: LiveData<Boolean> = _remindersChanged
    val loading: LiveData<Boolean> = _loading

    init {
        viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            savedReminders = dataInteractor.getAllRemindersSync()
            val current = savedReminders.toMutableList() //Copy the list
            withContext(ui) {
                _currentReminders.value = current
                _loading.value = false
            }
        }
    }

    fun saveChanges() {
        viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            _currentReminders.value?.let {
                val withDisplayIndices = it
                    .mapIndexed { index, reminder -> reminder.copy(displayIndex = index) }
                dataInteractor.updateReminders(withDisplayIndices)
                savedReminders = dataInteractor.getAllRemindersSync()
                withContext(ui) {
                    _currentReminders.value = savedReminders.toMutableList()
                    onRemindersUpdated()
                }
            }
            withContext(ui) { _loading.value = false }
        }
    }

    private fun onRemindersUpdated() {
        val a = savedReminders
        val b = currentReminders.value ?: return
        //If the two lists are not equal we have an update
        _remindersChanged.value = !(a.size == b.size && a.zip(b).all { it.first == it.second })
    }

    fun addReminder(defaultName: String) {
        _currentReminders.value = _currentReminders.value?.let {
            val newReminder = Reminder(
                //We just want a unique ID for now,
                // this won't be used when it's added to the db
                System.nanoTime(),
                getNextDisplayIndex(),
                defaultName,
                LocalTime.now(),
                CheckedDays.none()
            )
            it.toMutableList().apply { add(newReminder) }
        }
        onRemindersUpdated()
    }

    private fun getNextDisplayIndex(): Int {
        return _currentReminders.value?.let {
            if (it.isEmpty()) 0
            else it.maxOf { r -> r.displayIndex } + 1
        } ?: 0
    }

    fun deleteReminder(reminder: Reminder) {
        _currentReminders.value = _currentReminders.value?.let { reminders ->
            reminders.filter { it.id != reminder.id }
        }
        onRemindersUpdated()
    }

    fun adjustDisplayIndexes(indexUpdate: List<Reminder>) {
        _currentReminders.value = _currentReminders.value?.let { curr ->
            indexUpdate.mapIndexed { i, r ->
                curr.firstOrNull { it.id == r.id }?.copy(displayIndex = i)
            }.filterNotNull()
        }
        onRemindersUpdated()
    }

    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) =
        updateReminder(reminder, reminder.copy(checkedDays = checkedDays))

    fun onTimeChanged(reminder: Reminder, localTime: LocalTime) =
        updateReminder(reminder, reminder.copy(time = localTime))

    fun onNameChanged(reminder: Reminder, name: String) =
        updateReminder(reminder, reminder.copy(alarmName = name))

    private fun updateReminder(from: Reminder, to: Reminder) {
        _currentReminders.value = _currentReminders.value?.let { reminders ->
            val mutable = reminders.toMutableList()
            val index = mutable.indexOfFirst { it.id == from.id }
            if (index >= 0) {
                mutable.removeAt(index)
                mutable.add(index, to)
            }
            mutable
        }
        onRemindersUpdated()
    }
}
