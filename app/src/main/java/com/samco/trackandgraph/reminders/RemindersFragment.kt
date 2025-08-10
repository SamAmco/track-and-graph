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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.samco.trackandgraph.R
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.permissions.PermissionRequesterUseCase
import com.samco.trackandgraph.permissions.PermissionRequesterUseCaseImpl
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemindersFragment : Fragment(),
    PermissionRequesterUseCase by PermissionRequesterUseCaseImpl() {
    
    private val viewModel: RemindersViewModel by viewModels<RemindersViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()

    init {
        initNotificationsPermissionRequester(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TnGComposeTheme {
                    RemindersScreen(
                        reminders = viewModel.currentReminders.collectAsState().value,
                        isLoading = viewModel.loading.collectAsState().value,
                        hasChanges = viewModel.remindersChanged.collectAsState().value,
                        onSaveChanges = viewModel::saveChanges,
                        onDeleteReminder = viewModel::deleteReminder,
                        onMoveReminder = { from, to -> viewModel.moveItem(from, to) },
                    )
                }
            }
        }
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

    private fun onAddClicked() {
        viewModel.addReminder(getString(R.string.default_reminder_name))
        requestAlarmAndNotificationPermission(requireContext())
    }
}
