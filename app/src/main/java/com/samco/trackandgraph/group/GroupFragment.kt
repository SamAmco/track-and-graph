/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.R
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.permissions.PermissionRequesterUseCase
import com.samco.trackandgraph.permissions.PermissionRequesterUseCaseImpl
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The group fragment is used on the home page and in any nested group to display the contents of
 * that group. It must display a list of groups, graphs and features contained within its group.
 * The default value for args.groupId is 0L representing the root group or home page. The
 * args.groupName may be null or empty.
 */
@AndroidEntryPoint
class GroupFragment : Fragment(),
    PermissionRequesterUseCase by PermissionRequesterUseCaseImpl() {
    private var navController: NavController? = null
    private val args: GroupFragmentArgs by navArgs()

    private val viewModel: GroupViewModel by viewModels<GroupViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private val groupDialogsViewModel by viewModels<GroupDialogsViewModel>()

    private val addGroupDialogViewModel by viewModels<AddGroupDialogViewModelImpl>()

    @Inject
    lateinit var tngSettings: TngSettings

    @Inject
    @MainDispatcher
    lateinit var ui: CoroutineDispatcher

    init {
        initNotificationsPermissionRequester(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.navController = container?.findNavController()
        
        viewModel.setGroup(args.groupId)

        listenToViewModel()

        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    GroupScreen(
                        groupViewModel = viewModel,
                        groupDialogsViewModel = groupDialogsViewModel,
                        addGroupDialogViewModel = addGroupDialogViewModel,
                        groupId = args.groupId,
                        groupName = args.groupName,
                        // Navigation callbacks only
                        onTrackerEdit = this@GroupFragment::onTrackerEditClicked,
                        onGraphStatEdit = this@GroupFragment::onEditGraphStat,
                        onGraphStatClick = this@GroupFragment::onGraphStatClicked,
                        onGroupClick = this@GroupFragment::onGroupSelected,
                        onTrackerHistory = this@GroupFragment::onTrackerHistoryClicked,
                        // Permission-related callback
                        onRequestNotificationPermission = { requestNotificationPermission(requireContext()) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeScoped { setupMenu() }
    }

    private fun isRootGroup(): Boolean {
        return args.groupId == 0L
    }

    private suspend fun setupMenu() {
        appBarViewModel.setNavBarConfig(
            AppBarViewModel.NavBarConfig(
                title = if (isRootGroup()) getString(R.string.app_name) else args.groupName,
                actions = listOf(
                    AppBarViewModel.Action.ImportCSV,
                    AppBarViewModel.Action.ExportCSV,
                ),
                collapsedActions = AppBarViewModel.CollapsedActions(
                    overflowIconId = R.drawable.add_icon,
                    actions = listOf(
                        AppBarViewModel.Action.AddTracker,
                        AppBarViewModel.Action.AddGraphStat,
                        AppBarViewModel.Action.AddGroup,
                    ),
                )
            )
        )

        for (action in appBarViewModel.actionsTaken) {
            when (action) {
                AppBarViewModel.Action.ImportCSV -> onImportClicked()
                AppBarViewModel.Action.ExportCSV -> onExportClicked()
                AppBarViewModel.Action.AddTracker -> onAddTrackerClicked()
                AppBarViewModel.Action.AddGraphStat -> onAddGraphStatClicked()
                AppBarViewModel.Action.AddGroup -> onAddGroupClicked()
                else -> {}
            }
        }
    }

    private fun onGroupSelected(group: Group) {
        navigate(GroupFragmentDirections.actionSelectGroup(group.id, group.name))
    }

    private fun onGraphStatClicked(graphOrStat: IGraphStatViewData) {
        navigate(GroupFragmentDirections.actionViewGraphStat(graphOrStat.graphOrStat.id))
    }

    private fun onEditGraphStat(graphOrStat: IGraphStatViewData) {
        navigate(
            GroupFragmentDirections.actionGraphStatInput(
                graphStatId = graphOrStat.graphOrStat.id,
                groupId = args.groupId
            )
        )
    }

    private fun onTrackerHistoryClicked(tracker: DisplayTracker) {
        navigate(GroupFragmentDirections.actionFeatureHistory(tracker.featureId, tracker.name))
    }

    private fun navigate(direction: NavDirections) {
        //This check fixes a bug where calling navigate twice quickly would cause the app to crash
        // https://stackoverflow.com/a/53737537
        if (navController?.currentDestination?.id?.equals(R.id.groupFragment) != true) return
        navController?.navigate(direction)
    }

    private fun onTrackerEditClicked(tracker: DisplayTracker) {
        navigate(GroupFragmentDirections.actionAddTracker(args.groupId, tracker.id))
    }

    private fun listenToViewModel() {
        lifecycleScope.launch {
            viewModel.hasAnyReminders.filter { it }.collect {
                withContext(ui) {
                    requestNotificationPermission(requireContext())
                }
            }
        }
    }

    private fun onExportClicked() {
        groupDialogsViewModel.showExportDialog()
    }

    private fun onImportClicked() {
        groupDialogsViewModel.showImportDialog()
    }

    private fun onAddTrackerClicked() {
        navigate(GroupFragmentDirections.actionAddTracker(args.groupId))
    }

    private fun onAddGraphStatClicked() {
        lifecycleScope.launch {
            if (!viewModel.userHasAnyTrackers()) {
                groupDialogsViewModel.showNoTrackersDialog()
            } else {
                navigate(GroupFragmentDirections.actionGraphStatInput(groupId = args.groupId))
            }
        }
    }

    private fun onAddGroupClicked() {
        addGroupDialogViewModel.show(
            parentGroupId = args.groupId,
            groupId = null
        )
    }
}
