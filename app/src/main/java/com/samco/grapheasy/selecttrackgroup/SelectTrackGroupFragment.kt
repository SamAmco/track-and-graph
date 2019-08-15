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
import com.samco.grapheasy.databinding.FragmentSelectTrackGroupBinding

class SelectTrackGroupFragment : Fragment() {
    lateinit var binding: FragmentSelectTrackGroupBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_select_track_group, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val selectTrackGroupViewModel = createViewModel()
        binding.selectTrackGroupViewModel = selectTrackGroupViewModel

        val adapter = TrackGroupAdapter(TrackGroupListener { groupId ->
            selectTrackGroupViewModel.onTrackGroupSelected(groupId)
        })
        binding.groupList.adapter = adapter
        binding.groupList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        observeTrackGroupDataAndUpdate(selectTrackGroupViewModel, adapter)
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
    }

    private fun createViewModel(): SelectTrackGroupViewModel {
        val application = requireNotNull(this.activity).application
        val dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        val viewModelFactory = SelectTrackGroupViewModelFactory(dataSource, application)
        return ViewModelProviders.of(this, viewModelFactory).get(SelectTrackGroupViewModel::class.java)
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
        fragmentManager?.apply {
            beginTransaction().apply {
                add(R.id.overlay_container, AddTrackGroupOverlayFragment())
                addToBackStack(null)
                commit()
            }
        }
    }
}