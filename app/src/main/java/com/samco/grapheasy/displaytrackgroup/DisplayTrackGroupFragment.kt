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
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureTrackGroupJoin
import com.samco.grapheasy.database.FeatureType
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.databinding.FragmentDisplayTrackGroupBinding
import com.samco.grapheasy.selecttrackgroup.AddTrackGroupDialogFragment
import kotlinx.coroutines.*
import timber.log.Timber

class DisplayTrackGroupFragment : Fragment(),
    AddTrackGroupDialogFragment.AddTrackGroupDialogListener {
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


        val adapter = FeatureAdapter(FeatureClickListener())
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.featureList.adapter = adapter
        //TODO should we use a different column count for horizontal orientation?
        binding.featureList.layoutManager = GridLayoutManager(context, 2)

        setHasOptionsMenu(true)
        return binding.root
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
                Timber.d("RECOVERED FEATURES: ${it.joinToString { f -> f.name }}")
                adapter.submitList(it)
            }
        })
    }

    private fun onAddClicked() {
        //TODO create custom dialog fragment
        val dialog = AddTrackGroupDialogFragment()
        childFragmentManager.let { dialog.show(it, "add_track_group_dialog") }
    }

    override fun onAddTrackGroup(name: String) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                Timber.d("Adding feature: $name")
                val feature = Feature(
                    0,
                    name,
                    FeatureType.CONTINUOUS,
                    ""
                )
                val featureId = dao.insertFeature(feature)
                dao.insertFeatureTrackGroupJoin(FeatureTrackGroupJoin(0, featureId, args.trackGroup))
            }
        }
    }

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