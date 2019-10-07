package com.samco.grapheasy.displaytrackgroup

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import com.samco.grapheasy.databinding.FragmentDisplayTrackGroupBinding
import com.samco.grapheasy.ui.YesCancelDialogFragment
import kotlinx.coroutines.*
import timber.log.Timber

class DisplayTrackGroupFragment : Fragment(),
    AddFeatureDialogFragment.AddFeatureDialogListener,
    RenameFeatureDialogFragment.RenameFeatureDialogListener,
    YesCancelDialogFragment.YesCancelDialogListener,
    AddDataPointDialog.AddDataPointDialogListener
{

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
        ), viewModel)
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.featureList.adapter = adapter
        registerForContextMenu(binding.featureList)
        initializeGridLayout()

        setHasOptionsMenu(true)
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
        val viewModelFactory = DisplayTrackGroupViewModelFactory(args.trackGroup, getString(R.string.no_data), dataSource)
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
        val args = Bundle()
        args.putString(EXISTING_FEATURES_ARG_KEY,
            viewModel.features.value?.joinToString(EXISTING_FEATURES_DELIM) { f -> f.name }
        )
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "add_feature_dialog") }
    }

    override fun onAddFeature(name: String, featureType: FeatureType, discreteValues: List<String>) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val feature = Feature(
                    0,
                    name,
                    featureType,
                    discreteValues
                )
                val featureId = dao.insertFeature(feature)
                dao.insertFeatureTrackGroupJoin(FeatureTrackGroupJoin(0, featureId, args.trackGroup))
            }
        }
    }

    override fun getFeature(): Feature {
        return viewModel.currentActionFeature!!
    }

    private fun onFeatureDeleteClicked(feature: Feature) {
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

    private fun onDeleteFeature(feature: Feature) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteFeature(feature)
            }
        }
    }

    private fun onFeatureRenameClicked(feature: Feature) {
        viewModel.currentActionFeature = feature
        val dialog = RenameFeatureDialogFragment()
        childFragmentManager?.let { dialog.show(it, "rename_feature_dialog") }
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

    private fun onFeatureAddClicked(feature: Feature) {
        viewModel.currentActionFeature = feature
        val dialog = AddDataPointDialog()
        childFragmentManager.let { dialog.show(it, "") }
    }

    override fun onAddDataPoint(dataPoint: DataPoint) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                Timber.d("adding data point: ${dataPoint.value} to feature ${dataPoint.featureId}")
                dao.insertDataPoint(dataPoint)
            }
        }
    }

    //TODO implement onFeatureHistoryClicked
    private fun onFeatureHistoryClicked(feature: Feature) { }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> onAddClicked()
        }
        return true
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