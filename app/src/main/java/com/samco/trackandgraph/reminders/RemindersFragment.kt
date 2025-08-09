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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.RemindersFragmentBinding
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.permissions.PermissionRequesterUseCase
import com.samco.trackandgraph.permissions.PermissionRequesterUseCaseImpl
import com.samco.trackandgraph.util.bindingForViewLifecycle
import com.samco.trackandgraph.util.hideKeyboard
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemindersFragment : Fragment(),
    PermissionRequesterUseCase by PermissionRequesterUseCaseImpl() {
    private var binding: RemindersFragmentBinding by bindingForViewLifecycle()
    private val viewModel: RemindersViewModel by viewModels<RemindersViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private lateinit var adapter: ReminderListAdapter

    init {
        initNotificationsPermissionRequester(this)
    }

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

        observeSaveChangesButton()
        observeLoading()
        observeAndUpdateReminders()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        resumeScoped { setUpMenu() }
    }

    private suspend fun setUpMenu() {
        appBarViewModel.setNavBarConfig(
            AppBarViewModel.NavBarConfig(
                title = getString(R.string.reminders),
                actions = listOf(AppBarViewModel.Action.AddReminder),
            )
        )

        for (action in appBarViewModel.actionsTaken) {
            when (action) {
                AppBarViewModel.Action.AddReminder -> onAddClicked()
                else -> {}
            }
        }
    }

    private fun observeLoading() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect {
                    binding.loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeSaveChangesButton() {
        binding.saveChangesButton.setOnClickListener { viewModel.saveChanges() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.remindersChanged.collect {
                    binding.saveChangesButton.visibility = if (it) View.VISIBLE else View.GONE
                }
            }
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentReminders.collect {
                    if (it.isEmpty()) binding.noRemindersHintText.visibility = View.VISIBLE
                    else binding.noRemindersHintText.visibility = View.GONE

                    adapter.submitList(it)
                }
            }
        }
    }

    private fun onAddClicked() {
        viewModel.addReminder(getString(R.string.default_reminder_name))
        binding.remindersList.smoothScrollToPosition(viewModel.currentReminders.value.size)
        requestAlarmAndNotificationPermission(requireContext())
    }

    override fun onPause() {
        onHideKeyboard()
        super.onPause()
    }
}
