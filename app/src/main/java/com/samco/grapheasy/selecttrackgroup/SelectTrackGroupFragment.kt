package com.samco.grapheasy.selecttrackgroup

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.TrackGroup
import com.samco.grapheasy.databinding.FragmentSelectTrackGroupBinding
import com.samco.grapheasy.ui.YesCancelDialogFragment
import kotlinx.coroutines.*
import timber.log.Timber

class SelectTrackGroupFragment : Fragment(), YesCancelDialogFragment.YesCancelDialogListener, AddTrackGroupDialogFragment.AddTrackGroupDialogListener {
    private lateinit var binding: FragmentSelectTrackGroupBinding
    private lateinit var viewModel: SelectTrackGroupViewModel
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_select_track_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = createViewModel()
        binding.selectTrackGroupViewModel = viewModel

        val adapter = TrackGroupAdapter(
            TrackGroupListener(
                this::onTrackGroupSelected,
                this::onRenameTrackGroup,
                this::onDeleteTrackGroup)
        )
        binding.groupList.adapter = adapter
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        registerForContextMenu(binding.groupList)

        observeTrackGroupDataAndUpdate(viewModel, adapter)
        setHasOptionsMenu(true)
        //TODO observe clicks for track groups
        return binding.root
    }

    private fun observeTrackGroupDataAndUpdate(selectTrackGroupViewModel: SelectTrackGroupViewModel,
                                               adapter: TrackGroupAdapter) {
        selectTrackGroupViewModel.trackGroups.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.groupList.smoothScrollToPosition(0)
            }
        })
    }

    private fun createViewModel(): SelectTrackGroupViewModel {
        val application = requireNotNull(this.activity).application
        val dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        val viewModelFactory = SelectTrackGroupViewModelFactory(dataSource, application)
        return ViewModelProviders.of(this, viewModelFactory).get(SelectTrackGroupViewModel::class.java)
    }

    private fun onRenameTrackGroup(trackGroup: TrackGroup) {
        //TODO
        Timber.d("onRename: ${trackGroup.name}")
    }

    private fun onDeleteTrackGroup(trackGroup: TrackGroup) {
        Timber.d("onDelete: ${trackGroup.name}")
        viewModel.currentActionTrackGroup = trackGroup
        val dialog = YesCancelDialogFragment() //TODO add functionality for rename
        var args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_track_group))
        dialog.arguments = args
        childFragmentManager?.let { dialog.show(it, "ru_sure_del_track_group_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_track_group) -> deleteTrackGroup(viewModel.currentActionTrackGroup)
        }
    }

    private fun deleteTrackGroup(trackGroup: TrackGroup?) {
        if (trackGroup == null) return
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteTrackGroup(trackGroup)
            }
        }
    }

    private fun onTrackGroupSelected(trackGroup: TrackGroup) {
        //TODO respond to onTrackGroupSelected
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.select_track_group_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> onAddClicked()
        }
        return true
    }

    private fun onAddClicked() {
        val dialog = AddTrackGroupDialogFragment()
        childFragmentManager?.let { dialog.show(it, "add_track_group_dialog") }
    }

    override fun onAddTrackGroup(name: String) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val trackGroup = TrackGroup(0, name)
                dao.insertTrackGroup(trackGroup)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob.cancel()
    }
}