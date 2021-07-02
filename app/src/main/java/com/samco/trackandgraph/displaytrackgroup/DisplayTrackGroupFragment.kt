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
package com.samco.trackandgraph.displaytrackgroup

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.dto.DisplayFeature
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.databinding.FragmentDisplayTrackGroupBinding
import com.samco.trackandgraph.ui.*
import com.samco.trackandgraph.util.performTrackVibrate
import com.samco.trackandgraph.widgets.TrackWidgetProvider
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

const val GROUP_ID_KEY = "GROUP_ID_KEY"
const val GROUP_NAME_KEY = "GROUP_NAME_KEY"

class DisplayTrackGroupFragment : Fragment(),
    YesCancelDialogFragment.YesCancelDialogListener {

    private var navController: NavController? = null
    private val args: DisplayTrackGroupFragmentArgs by navArgs()

    private lateinit var binding: FragmentDisplayTrackGroupBinding
    private lateinit var adapter: FeatureAdapter
    private val viewModel by viewModels<DisplayTrackGroupViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_display_track_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.initViewModel(requireActivity(), args.trackGroup)

        adapter = FeatureAdapter(FeatureClickListener(
            this::onFeatureEditClicked,
            this::onFeatureDeleteClicked,
            this::onFeatureMoveToClicked,
            this::onFeatureDescriptionClicked,
            this::onFeatureAddClicked,
            this::onFeatureHistoryClicked
        ))
        observeFeatureDataAndUpdate()
        binding.featureList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.featureList)
        registerForContextMenu(binding.featureList)
        initializeGridLayout()

        binding.queueAddAllButton.hide()
        binding.queueAddAllButton.setOnClickListener { onQueueAddAllClicked() }

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = args.trackGroupName
        return binding.root
    }

    private fun onFeatureDescriptionClicked(feature: DisplayFeature) {
        showFeatureDescriptionDialog(requireContext(), feature.name, feature.description)
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN
                        or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (viewHolder != null && viewHolder is FeatureViewHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.elevateCard()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as FeatureViewHolder).dropCard()
            viewModel.adjustDisplayIndexes(adapter.getItems())
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
    }

    private fun initializeGridLayout() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels / dm.density
        val itemSize = 180f
        val gridLayout = GridLayoutManager(context, (screenWidth / itemSize).toInt())
        binding.featureList.layoutManager = gridLayout
    }

    private fun observeFeatureDataAndUpdate() {
        viewModel.features.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
            if (it.isNullOrEmpty()) {
                binding.noFeaturesHintText.text = getString(R.string.no_features_hint)
                binding.noFeaturesHintText.visibility = View.VISIBLE
                binding.queueAddAllButton.hide()
            } else {
                if (it.size > viewModel.numFeatures) {
                    binding.featureList.post { binding.featureList.scrollToPosition(0) }
                }
                viewModel.numFeatures = it.size

                binding.queueAddAllButton.show()
                binding.noFeaturesHintText.visibility = View.INVISIBLE
            }
        })
    }

    private fun onAddClicked() {
        val featureNames = viewModel.features.value?.map{f -> f.name}?.toTypedArray() ?: arrayOf()
        navController?.navigate(
            DisplayTrackGroupFragmentDirections
                .actionAddFeature(args.trackGroup, featureNames)
        )
    }

    private fun onFeatureMoveToClicked(feature: DisplayFeature) {
        val dialog = MoveToDialogFragment()
        var args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_TRACK)
        args.putLong(MOVE_DIALOG_GROUP_KEY, feature.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_track_group_dialog") }
    }

    private fun onFeatureDeleteClicked(feature: DisplayFeature) {
        viewModel.currentActionFeature = feature
        val dialog = YesCancelDialogFragment()
        var args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_feature))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_feature_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_feature) -> onDeleteFeature(viewModel.currentActionFeature!!)
        }
    }

    private fun onFeatureEditClicked(feature: DisplayFeature) {
        val featureNames = viewModel.features.value?.map{f -> f.name}?.toTypedArray() ?: arrayOf()
        navController?.navigate(
            DisplayTrackGroupFragmentDirections
                .actionAddFeature(args.trackGroup, featureNames, feature.id)
        )
    }

    private fun onDeleteFeature(feature: DisplayFeature) {
        viewModel.deleteFeature(feature)

        // Since the app has no data repository (yet?), notify the widgets here...
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, TrackWidgetProvider::class.java)
        intent.putExtra(com.samco.trackandgraph.widgets.DELETE_FEATURE_ID, feature.id)
        activity?.sendBroadcast(intent)
    }

    private fun onFeatureAddClicked(feature: DisplayFeature, useDefault: Boolean = true) {
        /**
         * @param useDefault: if false the default value will be ignored and the user will be queried for the value
         */
        if (feature.hasDefaultValue && useDefault) {
            requireContext().performTrackVibrate()
            viewModel.addDefaultValue(feature)
        } else {
            val argBundle = Bundle()
            argBundle.putLongArray(FEATURE_LIST_KEY, longArrayOf(feature.id))
            showAddDataPoint(argBundle)
        }
    }

    private fun onQueueAddAllClicked() {
        viewModel.features.value?.let { feats ->
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

    private fun onFeatureHistoryClicked(feature: DisplayFeature) {
        navController?.navigate(
            DisplayTrackGroupFragmentDirections
                .actionFeatureHistory(feature.id, feature.name)
        )
    }

    private fun onExportClicked() {
        val dialog = ExportFeaturesDialog()
        val argBundle = Bundle()
        argBundle.putLong(GROUP_ID_KEY, args.trackGroup)
        argBundle.putString(GROUP_NAME_KEY, args.trackGroupName)
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "export_features_dialog") }
    }

    private fun onImportClicked() {
        val dialog = ImportFeaturesDialog()
        val argBundle = Bundle()
        argBundle.putLong(GROUP_ID_KEY, args.trackGroup)
        argBundle.putString(GROUP_NAME_KEY, args.trackGroupName)
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "import_features_dialog") }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> onAddClicked()
            R.id.exportButton -> onExportClicked()
            R.id.importButton -> onImportClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.display_track_group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}

class DisplayTrackGroupViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    //TODO can be nullable now
    private var groupId: Long = -1

    var numFeatures = -1

    var currentActionFeature: DisplayFeature? = null
    lateinit var features: LiveData<List<DisplayFeature>>
        private set

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    fun initViewModel(activity: Activity, groupId: Long) {
        if (dataSource != null) return
        this.groupId = groupId
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        features = dataSource!!.getDisplayFeaturesForGroup(groupId)
    }

    override fun onCleared() {
        super.onCleared()
        updateJob.cancel()
    }

    fun deleteFeature(feature: DisplayFeature) = ioScope.launch {
        dataSource?.deleteFeature(feature.id)
    }


    fun addDefaultValue(feature: DisplayFeature) = ioScope.launch {
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
        dataSource?.insertDataPoint(newDataPoint)
    }

    fun adjustDisplayIndexes(displayFeatures: List<DisplayFeature>) {
        ioScope.launch {
            features.value?.let { oldList ->
                val newList = displayFeatures.mapIndexed { i, df ->
                    toFeature(oldList.first { f -> f.id == df.id }.copy(displayIndex = i))
                }
                dataSource!!.updateFeatures(newList)
            }
        }
    }

    private fun toFeature(df: DisplayFeature) = Feature.create(
        df.id, df.name, df.groupId,
        df.featureType, df.discreteValues, df.hasDefaultValue, df.defaultValue, df.displayIndex,
        df.description
    )
}
