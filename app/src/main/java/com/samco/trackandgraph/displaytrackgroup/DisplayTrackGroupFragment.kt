package com.samco.trackandgraph.displaytrackgroup

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentDisplayTrackGroupBinding
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import kotlinx.coroutines.*

const val TRACK_GROUP_ID_KEY = "TRACK_GROUP_ID_KEY"
const val TRACK_GROUP_NAME_KEY = "TRACK_GROUP_NAME_KEY"

class DisplayTrackGroupFragment : Fragment(),
    RenameFeatureDialogFragment.RenameFeatureDialogListener,
    YesCancelDialogFragment.YesCancelDialogListener {

    private var navController: NavController? = null
    private val args: DisplayTrackGroupFragmentArgs by navArgs()

    private lateinit var binding: FragmentDisplayTrackGroupBinding
    private lateinit var adapter: FeatureAdapter
    private lateinit var viewModel: DisplayTrackGroupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_display_track_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProviders.of(this).get(DisplayTrackGroupViewModel::class.java)
        viewModel.initViewModel(activity!!, args.trackGroup)

        adapter = FeatureAdapter(FeatureClickListener(
            this::onFeatureRenameClicked,
            this::onFeatureDeleteClicked,
            this::onFeatureAddClicked,
            this::onFeatureHistoryClicked
        ))
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.featureList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.featureList)
        registerForContextMenu(binding.featureList)
        initializeGridLayout()

        binding.queueAddAllButton.setOnClickListener { onQueueAddAllClicked() }

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = args.trackGroupName
        return binding.root
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

    private fun observeFeatureDataAndUpdate(displayTrackGroupViewModel: DisplayTrackGroupViewModel, adapter: FeatureAdapter) {
        displayTrackGroupViewModel.features.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it.toMutableList())
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.featureList.smoothScrollToPosition(0)
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

    override fun getFeature(): Feature {
        val f = viewModel.currentActionFeature!!
        return Feature.create(f.id, f.name, f.trackGroupId, f.featureType, f.discreteValues, f.displayIndex)
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

    //TODO allow people to rename the discrete values if it is a discrete feature
    private fun onFeatureRenameClicked(feature: DisplayFeature) {
        viewModel.currentActionFeature = feature
        val dialog = RenameFeatureDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_feature_dialog") }
    }

    override fun getMaxFeatureNameChars(): Int = MAX_FEATURE_NAME_LENGTH

    private fun onDeleteFeature(feature: DisplayFeature) { viewModel.deleteFeature(feature) }

    override fun onRenameFeature(newName: String) {
        val f = viewModel.currentActionFeature!!
        val newFeature = Feature.create(f.id, newName, f.trackGroupId,
            f.featureType, f.discreteValues, f.displayIndex)
        viewModel.updateFeature(newFeature)
    }

    private fun onFeatureAddClicked(feature: DisplayFeature) {
        val argBundle = Bundle()
        argBundle.putLongArray(FEATURE_LIST_KEY, longArrayOf(feature.id))
        showAddDataPoint(argBundle)
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
        argBundle.putLong(TRACK_GROUP_ID_KEY, args.trackGroup)
        argBundle.putString(TRACK_GROUP_NAME_KEY, args.trackGroupName)
        dialog.arguments = argBundle
        childFragmentManager.let { dialog.show(it, "export_features_dialog") }
    }

    private fun onImportClicked() {
        val dialog = ImportFeaturesDialog()
        val argBundle = Bundle()
        argBundle.putLong(TRACK_GROUP_ID_KEY, args.trackGroup)
        argBundle.putString(TRACK_GROUP_NAME_KEY, args.trackGroupName)
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
    private var trackGroupId: Long = -1

    var currentActionFeature: DisplayFeature? = null
    lateinit var features: LiveData<List<DisplayFeature>>
        private set

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    fun initViewModel(activity: Activity, trackGroupId: Long) {
        if (dataSource != null) return
        this.trackGroupId = trackGroupId
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        features = dataSource!!.getDisplayFeaturesForTrackGroup(trackGroupId)
    }

    override fun onCleared() {
        super.onCleared()
        updateJob.cancel()
    }

    fun deleteFeature(feature: DisplayFeature) = ioScope.launch {
        dataSource?.deleteFeature(feature.id)
    }

    fun updateFeature(feature: Feature) = ioScope.launch {
        dataSource?.updateFeature(feature)
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

    private fun toFeature(df: DisplayFeature) = Feature.create(df.id, df.name, df.trackGroupId,
        df.featureType, df.discreteValues, df.displayIndex)
}
