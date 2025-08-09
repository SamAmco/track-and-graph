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
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.addgroup.AddGroupDialogViewModelImpl
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.permissions.PermissionRequesterUseCase
import com.samco.trackandgraph.permissions.PermissionRequesterUseCaseImpl
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.*
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.util.performTrackVibrate
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.getValue

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

    private lateinit var adapter: GroupAdapter
    private lateinit var recyclerView: RecyclerView
    private val viewModel by viewModels<GroupViewModel>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private val groupDialogsViewModel by viewModels<GroupDialogsViewModel>()

    private val addDataPointsDialogViewModel by viewModels<AddDataPointsViewModelImpl>()
    private val addGroupDialogViewModel by viewModels<AddGroupDialogViewModelImpl>()

    private var forceNextNotifyDataSetChanged: Boolean = false
    
    // State for FAB visibility based on scroll behavior
    private val showFab = mutableStateOf(true)
    
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
        
        // Create and setup RecyclerView
        recyclerView = RecyclerView(requireContext())
        initializeGridLayout()
        adapter = GroupAdapter(
            createTrackerClickListener(),
            createGraphStatClickListener(),
            createGroupClickListener()
        )
        recyclerView.adapter = adapter
        addItemTouchHelper()
        scrollToTopOnItemAdded()
        setupFabScrollListener()

        listenToViewModel()

        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    GroupScreen(
                        recyclerView = recyclerView,
                        groupViewModel = viewModel,
                        groupDialogsViewModel = groupDialogsViewModel,
                        addDataPointsDialogViewModel = addDataPointsDialogViewModel,
                        addGroupDialogViewModel = addGroupDialogViewModel,
                        trackGroupId = args.groupId,
                        trackGroupName = args.groupName,
                        showFab = showFab.value,
                        onQueueAddAllClicked = { onQueueAddAllClicked() }
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

    private fun addItemTouchHelper() {
        ItemTouchHelper(
            DragTouchHelperCallback(
                { start: Int, end: Int -> adapter.moveItem(start, end) },
                { viewModel.adjustDisplayIndexes(adapter.getItems()) }
            )).attachToRecyclerView(recyclerView)
    }

    private fun scrollToTopOnItemAdded() {
        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    //Scroll to the top when we've added something new to our group,
                    // but not when the adapter is being re-populated, e.g. when returning
                    // to this fragment from a nested group
                    if (itemCount == 1) recyclerView.smoothScrollToPosition(0)
                }
            }
        )
    }

    private fun createGroupClickListener() = GroupClickListener(
        this::onGroupSelected,
        this::onEditGroupClicked,
        this::onDeleteGroupClicked,
        this::onMoveGroupClicked
    )

    private fun onMoveGroupClicked(group: Group) {
        val dialog = MoveToDialogFragment()
        val args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_GROUP)
        args.putLong(MOVE_DIALOG_GROUP_KEY, group.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_dialog") }
    }

    private fun onDeleteGroupClicked(group: Group) {
        groupDialogsViewModel.showDeleteGroupDialog(group)
    }

    private fun onEditGroupClicked(group: Group) {
        addGroupDialogViewModel.show(
            parentGroupId = group.parentGroupId,
            groupId = group.id
        )
    }

    private fun onGroupSelected(group: Group) {
        navigate(GroupFragmentDirections.actionSelectGroup(group.id, group.name))
    }

    private fun createGraphStatClickListener() = GraphStatClickListener(
        this::onDeleteGraphStatClicked,
        this::onEditGraphStat,
        this::onGraphStatClicked,
        this::onMoveGraphStatClicked,
        viewModel::duplicateGraphOrStat
    )

    private fun onDeleteGraphStatClicked(graphOrStat: IGraphStatViewData) {
        groupDialogsViewModel.showDeleteGraphStatDialog(graphOrStat)
    }

    private fun onMoveGraphStatClicked(graphOrStat: IGraphStatViewData) {
        val dialog = MoveToDialogFragment()
        val args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_GRAPH)
        args.putLong(MOVE_DIALOG_GROUP_KEY, graphOrStat.graphOrStat.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_dialog") }
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

    private fun createTrackerClickListener() = TrackerClickListener(
        onEdit = this::onTrackerEditClicked,
        onDelete = this::onTrackerDeleteClicked,
        onMoveTo = this::onTrackerMoveClicked,
        onDescription = this::onTrackerDescriptionClicked,
        onAdd = this::onTrackerAddClicked,
        onHistory = this::onTrackerHistoryClicked,
        onPlayTimer = this::onTrackerPlayTimerClicked,
        onStopTimer = this::onStopTimerClicked
    )

    private fun onTrackerPlayTimerClicked(tracker: DisplayTracker) {
        viewModel.playTimer(tracker)
        requestNotificationPermission(requireContext())
    }

    private fun onStopTimerClicked(tracker: DisplayTracker) {
        //Due to a bug with the GridLayoutManager when you stop a timer and the timer text disappears
        // the views heights are not properly re-calculated and we need to call notifyDataSetChanged
        // to get the view heights right again
        forceNextNotifyDataSetChanged = true
        viewModel.stopTimer(tracker)
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

    private fun onTrackerAddClicked(tracker: DisplayTracker, useDefault: Boolean = true) {
        if (tracker.hasDefaultValue && useDefault) {
            requireContext().performTrackVibrate()
            viewModel.addDefaultTrackerValue(tracker)
        } else addDataPointsDialogViewModel.showAddDataPointDialog(trackerId = tracker.id)
    }

    private fun onTrackerDescriptionClicked(tracker: DisplayTracker) {
        groupDialogsViewModel.showFeatureDescriptionDialog(tracker)
    }

    private fun onTrackerMoveClicked(tracker: DisplayTracker) {
        val dialog = MoveToDialogFragment()
        val args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_TRACK)
        args.putLong(MOVE_DIALOG_GROUP_KEY, tracker.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_dialog") }
    }

    private fun onTrackerEditClicked(tracker: DisplayTracker) {
        navigate(GroupFragmentDirections.actionAddTracker(args.groupId, tracker.id))
    }

    private fun onQueueAddAllClicked() {
        viewModel.trackers.let { trackers ->
            addDataPointsDialogViewModel.showAddDataPointsDialog(trackerIds = trackers.map { it.id })
        }
    }

    private fun initializeGridLayout() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels / dm.density
        val itemSize = (screenWidth / 2f).coerceAtMost(180f)
        val spanCount = (screenWidth / itemSize).coerceAtLeast(2f).toInt()
        val gridLayoutManager = GridLayoutManager(context, spanCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (val spanMode = adapter.getSpanModeForItem(position)) {
                    SpanMode.FullWidth -> spanCount
                    is SpanMode.NumSpans -> spanMode.spans
                    null -> 1
                }
            }
        }
        recyclerView.layoutManager = gridLayoutManager
    }

    private fun listenToViewModel() {
        viewModel.allChildren.observe(viewLifecycleOwner) {
            adapter.submitList(it, forceNextNotifyDataSetChanged)
            forceNextNotifyDataSetChanged = false
        }

        viewModel.showDurationInputDialog.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            addDataPointsDialogViewModel.showAddDataPointDialog(
                trackerId = it.trackerId,
                customInitialValue = it.duration.seconds.toDouble()
            )
            viewModel.onConsumedShowDurationInputDialog()
        }

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
        if (!viewModel.hasTrackers.value) {
            groupDialogsViewModel.showNoTrackersDialog()
        } else {
            navigate(GroupFragmentDirections.actionGraphStatInput(groupId = args.groupId))
        }
    }

    private fun onAddGroupClicked() {
        addGroupDialogViewModel.show(
            parentGroupId = args.groupId,
            groupId = null
        )
    }

    private fun onTrackerDeleteClicked(tracker: DisplayTracker) {
        groupDialogsViewModel.showDeleteTrackerDialog(tracker)
    }

    private fun setupFabScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    showFab.value = false
                } else if (dy < 0) {
                    showFab.value = true
                }
            }
        })
    }
}
