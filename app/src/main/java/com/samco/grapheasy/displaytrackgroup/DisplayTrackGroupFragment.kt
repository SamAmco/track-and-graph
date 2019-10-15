package com.samco.grapheasy.displaytrackgroup

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import com.samco.grapheasy.databinding.FragmentDisplayTrackGroupBinding
import com.samco.grapheasy.ui.DataPointInputViewModel
import com.samco.grapheasy.ui.YesCancelDialogFragment
import kotlinx.coroutines.*
import timber.log.Timber

const val TRACK_GROUP_ID_KEY = "TRACK_GROUP_ID_KEY"
const val TRACK_GROUP_NAME_KEY = "TRACK_GROUP_NAME_KEY"

class DisplayTrackGroupFragment : Fragment(),
    RenameFeatureDialogFragment.RenameFeatureDialogListener,
    YesCancelDialogFragment.YesCancelDialogListener,
    InputDataPointDialog.InputDataPointDialogListener {

    private var navController: NavController? = null
    private val args: DisplayTrackGroupFragmentArgs by navArgs()

    private lateinit var binding: FragmentDisplayTrackGroupBinding
    private lateinit var viewModel: DisplayTrackGroupViewModel

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_display_track_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = createViewModel()
        binding.displayTrackGroupViewModel = viewModel

        val adapter = FeatureAdapter(FeatureClickListener(
            this::onFeatureRenameClicked,
            this::onFeatureDeleteClicked,
            this::onFeatureAddClicked,
            this::onFeatureHistoryClicked
        ))
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.featureList.adapter = adapter
        registerForContextMenu(binding.featureList)
        initializeGridLayout()

        binding.queueAddAllButton.setOnClickListener { onQueueAddAllClicked() }

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = args.trackGroupName
        return binding.root
    }

    private fun initializeGridLayout() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels / dm.density
        val itemSize = 180f
        val gridLayout = GridLayoutManager(context, (screenWidth / itemSize).toInt())
        binding.featureList.layoutManager = gridLayout
    }

    private fun createViewModel(): DisplayTrackGroupViewModel {
        val application = requireNotNull(this.activity).application
        val dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        val viewModelFactory = DisplayTrackGroupViewModelFactory(args.trackGroup, dataSource)
        return ViewModelProviders.of(this, viewModelFactory).get(DisplayTrackGroupViewModel::class.java)
    }

    private fun observeFeatureDataAndUpdate(displayTrackGroupViewModel: DisplayTrackGroupViewModel, adapter: FeatureAdapter) {
        displayTrackGroupViewModel.features.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    private fun onAddClicked() {
        val dialog = AddFeatureDialogFragment()
        val argsBundle = Bundle()
        argsBundle.putString(EXISTING_FEATURES_ARG_KEY,
            viewModel.features.value?.joinToString(EXISTING_FEATURES_DELIM) { f -> f.name }
        )
        argsBundle.putLong(TRACK_GROUP_ID_KEY, args.trackGroup)
        dialog.arguments = argsBundle
        childFragmentManager.let { dialog.show(it, "add_feature_dialog") }
    }


    override fun getFeature(): Feature {
        val f = viewModel.currentActionFeature!!
        return Feature(f.id, f.name, f.trackGroupId, f.featureType, f.discreteValues)
    }

    override fun getFeatures(): List<Feature> {
        val f = viewModel.currentActionFeatures!!
        return f.map { df -> Feature(df.id, df.name, df.trackGroupId, df.featureType, df.discreteValues) }
    }

    override fun getInputDataPoint(): DataPoint? = null

    override fun getViewModel() = viewModel

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

    private fun onDeleteFeature(feature: DisplayFeature) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteFeature(feature.id)
            }
        }
    }

    //TODO allow people to rename the discrete values if it is a discrete feature
    private fun onFeatureRenameClicked(feature: DisplayFeature) {
        viewModel.currentActionFeature = feature
        val dialog = RenameFeatureDialogFragment()
        childFragmentManager.let { dialog.show(it, "rename_feature_dialog") }
    }

    override fun onRenameFeature(feature: Feature) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateFeature(feature)
            }
        }
    }

    private fun onFeatureAddClicked(feature: DisplayFeature) {
        viewModel.currentActionFeatures = listOf(feature)
        val dialog = InputDataPointDialog()
        childFragmentManager.let { dialog.show(it, "input_data_point_dialog") }
    }

    private fun onQueueAddAllClicked() {
        viewModel.features.value?.let { feats ->
            if (feats.isEmpty()) return
            viewModel.currentActionFeatures = feats
            val dialog = InputDataPointDialog()
            childFragmentManager.let { dialog.show(it, "input_data_points_dialog") }
        }
    }

    override fun onDataPointInput(dataPoint: DataPoint) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                Timber.d("adding data point: ${dataPoint.value} to feature ${dataPoint.featureId}")
                dao.insertDataPoint(dataPoint)
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        updateJob.cancel()
    }
}

class DisplayTrackGroupViewModelFactory(
    private val trackGroupId: Long,
    private val dataSource: GraphEasyDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisplayTrackGroupViewModel::class.java)) {
            return DisplayTrackGroupViewModel(trackGroupId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DisplayTrackGroupViewModel(trackGroupId: Long, dataSource: GraphEasyDatabaseDao)
    : DataPointInputViewModel(), InputDataPointDialog.InputDataPointDialogViewModel {
    var currentActionFeature: DisplayFeature? = null
    var currentActionFeatures: List<DisplayFeature>? = null
    val features = dataSource.getDisplayFeaturesForTrackGroup(trackGroupId)
}
