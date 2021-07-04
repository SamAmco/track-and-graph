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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.room.withTransaction
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.DisplayFeature
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.databinding.FragmentGroupBinding
import com.samco.trackandgraph.displaytrackgroup.*
import com.samco.trackandgraph.graphclassmappings.graphStatTypes
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.*
import com.samco.trackandgraph.util.performTrackVibrate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime

//TODO there are two todo's in this class
class GroupFragment : Fragment(), YesCancelDialogFragment.YesCancelDialogListener,
    AddGroupDialogFragment.AddGroupDialogListener {
    private var navController: NavController? = null
    private val args: GroupFragmentArgs by navArgs()

    private lateinit var binding: FragmentGroupBinding
    private lateinit var adapter: GroupAdapter
    private val viewModel by viewModels<GroupViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.navController = container?.findNavController()
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val database = TrackAndGraphDatabase
            .getInstance(requireActivity().application)
        viewModel.initViewModel(database, args.groupId)

        binding.emptyGroupText.visibility = View.INVISIBLE

        adapter = GroupAdapter(
            createFeatureClickListener(),
            createGraphStatClickListener(),
            createGroupClickListener()
        )
        binding.itemList.adapter = adapter
        ItemTouchHelper(DragTouchHelperCallback(
            { start: Int, end: Int -> adapter.moveItem(start, end) },
            { viewModel.adjustDisplayIndexes(adapter.getItems()) }
        )).attachToRecyclerView(binding.itemList)
        initializeGridLayout()

        binding.queueAddAllButton.hide()
        binding.queueAddAllButton.setOnClickListener { onQueueAddAllClicked() }
        registerForContextMenu(binding.itemList)

        setHasOptionsMenu(true)
        args.groupName?.let { (activity as AppCompatActivity).supportActionBar?.title = it }

        listenToViewModel()
        return binding.root
    }

    private fun createGroupClickListener() = GroupClickListener(
        this::onGroupSelected,
        this::onRenameGroupClicked,
        this::onDeleteGroupClicked
    )

    private fun onDeleteGroupClicked(group: Group) {
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_group))
        args.putString("id", group.id.toString())
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_group_fragment") }
    }

    private fun onRenameGroupClicked(group: Group) {
        //TODO implement rename group dialog
    }

    private fun onGroupSelected(group: Group) {
        navController?.navigate(
            GroupFragmentDirections.actionSelectGroup(group.id, group.name)
        )
    }

    private fun createGraphStatClickListener() = GraphStatClickListener(
        this::onDeleteGraphStatClicked,
        this::onEditGraphStat,
        this::onGraphStatClicked,
        this::onMoveGraphStatClicked,
        viewModel::duplicateGraphOrStat
    )

    private fun onDeleteGraphStatClicked(graphOrStat: IGraphStatViewData) {
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_graph))
        args.putString("id", graphOrStat.graphOrStat.id.toString())
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_group_fragment") }
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
        navController?.navigate(GroupFragmentDirections.actionViewGraphStat(graphOrStat.graphOrStat.id))
    }

    private fun onEditGraphStat(graphOrStat: IGraphStatViewData) {
        navController?.navigate(
            GroupFragmentDirections.actionGraphStatInput(
                args.groupId,
                graphOrStat.graphOrStat.id
            )
        )
    }

    private fun createFeatureClickListener() = FeatureClickListener(
        this::onFeatureEditClicked,
        this::onFeatureDeleteClicked,
        this::onFeatureMoveToClicked,
        this::onFeatureDescriptionClicked,
        this::onFeatureAddClicked,
        this::onFeatureHistoryClicked
    )

    private fun onFeatureHistoryClicked(feature: DisplayFeature) {
        navController?.navigate(
            GroupFragmentDirections.actionFeatureHistory(feature.id, feature.name)
        )
    }

    private fun onFeatureAddClicked(feature: DisplayFeature, useDefault: Boolean = true) {
        /**
         * @param useDefault: if false the default value will be ignored and the user will be queried for the value
         */
        if (feature.hasDefaultValue && useDefault) {
            requireContext().performTrackVibrate()
            viewModel.addDefaultFeatureValue(feature)
        } else {
            val argBundle = Bundle()
            argBundle.putLongArray(FEATURE_LIST_KEY, longArrayOf(feature.id))
            showAddDataPoint(argBundle)
        }
    }

    private fun onFeatureDescriptionClicked(feature: DisplayFeature) {
        showFeatureDescriptionDialog(requireContext(), feature.name, feature.description)
    }

    private fun onFeatureMoveToClicked(feature: DisplayFeature) {
        val dialog = MoveToDialogFragment()
        val args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_TRACK)
        args.putLong(MOVE_DIALOG_GROUP_KEY, feature.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_dialog") }
    }

    private fun onFeatureEditClicked(feature: DisplayFeature) {
        val featureNames = viewModel.features.map { f -> f.name }.toTypedArray()
        navController?.navigate(
            GroupFragmentDirections
                .actionAddFeature(args.groupId, featureNames, feature.id)
        )
    }

    private fun onQueueAddAllClicked() {
        viewModel.features.let { feats ->
            if (feats.isEmpty()) return
            val argBundle = Bundle()
            argBundle.putLongArray(FEATURE_LIST_KEY, feats.map { f -> f.id }.toLongArray())
            showAddDataPoint(argBundle)
        }
    }

    private fun showAddDataPoint(argBundle: Bundle) {
        val dialog = InputDataPointDialog()
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "input_data_points_dialog") }
    }

    private fun initializeGridLayout() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels / dm.density
        val itemSize = 180f
        val gridLayout = GridLayoutManager(context, (screenWidth / itemSize).toInt())
        gridLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter.getSpanSizeAtPosition(position)
            }
        }
        binding.itemList.layoutManager = gridLayout
    }

    private fun listenToViewModel() {
        viewModel.groupChildren.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            updateShowQueueTrackButton()
            binding.emptyGroupText.visibility = if (it.isEmpty()) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateShowQueueTrackButton() {
        if (viewModel.features.isNotEmpty()) binding.queueAddAllButton.show()
        else binding.queueAddAllButton.hide()
    }

    override fun onStart() {
        super.onStart()
        requireActivity().toolbar.overflowIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.add_icon)
    }

    override fun onStop() {
        super.onStop()
        if (navController?.currentDestination?.id != R.id.groupFragment) {
            requireActivity().toolbar.overflowIcon =
                ContextCompat.getDrawable(requireContext(), R.drawable.list_menu_icon)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_tracker -> onAddTrackerClicked()
            R.id.add_graph_stat -> onAddGraphStatClicked()
            R.id.add_group -> onAddGroupClicked()
            R.id.export_button -> onExportClicked()
            R.id.import_button -> onImportClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onExportClicked() {
        val dialog = ExportFeaturesDialog()
        val argBundle = Bundle()
        argBundle.putLong(GROUP_ID_KEY, args.groupId)
        argBundle.putString(GROUP_NAME_KEY, args.groupName)
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "export_features_dialog") }
    }

    private fun onImportClicked() {
        val dialog = ImportFeaturesDialog()
        val argBundle = Bundle()
        argBundle.putLong(GROUP_ID_KEY, args.groupId)
        argBundle.putString(GROUP_NAME_KEY, args.groupName)
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "import_features_dialog") }
    }

    private fun onAddTrackerClicked() {
        val featureNames = viewModel.features.map { f -> f.name }.toTypedArray()
        navController?.navigate(
            GroupFragmentDirections.actionAddFeature(args.groupId, featureNames)
        )
    }

    private fun onAddGraphStatClicked() {
        navController?.navigate(
            GroupFragmentDirections.actionGraphStatInput(groupId = args.groupId)
        )
    }

    private fun onAddGroupClicked() {
        val dialog = AddGroupDialogFragment()
        val args = Bundle()
        args.putString("title", getString(R.string.add_group))
        args.putString("hint", getString(R.string.group_name))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "add_group_dialog") }
    }

    override fun onAddGroup(name: String, colorIndex: Int) {
        viewModel.addGroup(
            Group(0, name, 0, args.groupId, colorIndex)
        )
    }

    private fun onFeatureDeleteClicked(feature: DisplayFeature) {
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_feature))
        args.putString("id", feature.id.toString())
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_feature_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment, id: String?) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_feature) -> id?.let { viewModel.onDeleteFeature(it.toLong()) }
            getString(R.string.ru_sure_del_group) -> id?.let { viewModel.onDeleteGroup(it.toLong()) }
            getString(R.string.ru_sure_del_graph) -> id?.let { viewModel.onDeleteGraphStat(it.toLong()) }
        }
    }
}

enum class GroupChildType { GROUP, FEATURE, GRAPH }

data class GroupChild(
    val type: GroupChildType,
    val obj: Any,
    val id: () -> Long,
    val displayIndex: () -> Int
)

class GroupViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)
    private var database: TrackAndGraphDatabase? = null
    private lateinit var dataSource: TrackAndGraphDatabaseDao

    private lateinit var _features: LiveData<List<DisplayFeature>>
    private lateinit var _groups: LiveData<List<Group>>
    private lateinit var _graphStats: GraphStatLiveData

    lateinit var groupChildren: LiveData<List<GroupChild>>

    val features
        get() = groupChildren.value
            ?.filter { it.type == GroupChildType.FEATURE }
            ?.map { it.obj as DisplayFeature }
            ?: emptyList()

    fun initViewModel(database: TrackAndGraphDatabase, groupId: Long) {
        if (this.database != null) return
        this.database
        this.dataSource = database.trackAndGraphDatabaseDao

        _features = dataSource.getDisplayFeaturesForGroup(groupId)
        _groups = dataSource.getGroupsForGroup(groupId)
        _graphStats = GraphStatLiveData(updateJob, groupId, dataSource)

        initGroupChildren()
    }

    private fun initGroupChildren() {
        val groupChildrenUnsorted =
            Transformations.map(_features, this::featuresToGroupChildren)
                .combineWith(
                    Transformations.map(_groups, this::groupsToGroupChildren),
                    this::combindLists
                )
                .combineWith(
                    Transformations.map(_graphStats, this::graphsToGroupChildren),
                    this::combindLists
                )

        groupChildren = Transformations.map(groupChildrenUnsorted) { x ->
            x.sortedBy { it.displayIndex() }
        }
    }

    private fun <T, K, R> LiveData<T>.combineWith(
        liveData: LiveData<K>,
        block: (T?, K?) -> R
    ): LiveData<R> {
        val result = MediatorLiveData<R>()
        result.addSource(this) {
            result.value = block(this.value, liveData.value)
        }
        result.addSource(liveData) {
            result.value = block(this.value, liveData.value)
        }
        return result
    }

    private fun combindLists(l1: List<GroupChild>?, l2: List<GroupChild>?): List<GroupChild> {
        val newList = mutableListOf<GroupChild>()
        l1?.let { newList.addAll(it) }
        l2?.let { newList.addAll(it) }
        return newList
    }

    private fun graphsToGroupChildren(graphs: List<Pair<Instant, IGraphStatViewData>>): List<GroupChild> {
        return graphs.map {
            GroupChild(
                GroupChildType.GRAPH,
                it,
                { it.second.graphOrStat.id },
                { it.second.graphOrStat.displayIndex }
            )
        }
    }

    private fun groupsToGroupChildren(groups: List<Group>): List<GroupChild> {
        return groups.map {
            GroupChild(GroupChildType.GROUP, it, it::id, it::displayIndex)
        }
    }

    private fun featuresToGroupChildren(displayFeatures: List<DisplayFeature>): List<GroupChild> {
        return displayFeatures.map {
            GroupChild(GroupChildType.FEATURE, it, it::id, it::displayIndex)
        }
    }

    fun addDefaultFeatureValue(feature: DisplayFeature) = ioScope.launch {
        val label = if (feature.featureType == FeatureType.DISCRETE) {
            feature.discreteValues[feature.defaultValue.toInt()].label
        } else ""
        val newDataPoint = DataPoint(
            OffsetDateTime.now(),
            feature.id,
            feature.defaultValue,
            label,
            ""
        )
        dataSource.insertDataPoint(newDataPoint)
        _graphStats.updateAllGraphStats()
    }

    fun onDeleteFeature(id: Long) = ioScope.launch {
        dataSource.deleteFeature(id)
        _graphStats.preenGraphStats()
    }

    fun addGroup(group: Group) = ioScope.launch { dataSource.insertGroup(group) }

    fun adjustDisplayIndexes(items: List<GroupChild>) {
        TODO("Not yet implemented")
    }

    fun onDeleteGraphStat(id: Long) = ioScope.launch { dataSource.deleteGraphOrStat(id) }

    fun onDeleteGroup(id: Long) = ioScope.launch { dataSource.deleteGroup(id) }

    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        ioScope.launch {
            database?.withTransaction {
                val gs = graphOrStatViewData.graphOrStat
                graphStatTypes[gs.type]?.dataSourceAdapter?.duplicateGraphOrStat(dataSource, gs)
            }
        }
    }
}