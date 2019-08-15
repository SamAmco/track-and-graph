package com.samco.grapheasy.selecttrackgroup

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.samco.grapheasy.R
import com.samco.grapheasy.databinding.FragmentSelectTrackGroupBinding

class SelectTrackGroupFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentSelectTrackGroupBinding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_select_track_group, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)

        val viewModelFactory = SelectTrackGroupViewModelFactory()

        val selectTrackGroupViewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(SelectTrackGroupViewModel::class.java)

        binding.selectTrackGroupViewModel = selectTrackGroupViewModel

        val adapter = TrackGroupAdapter(TrackGroupListener { groupId ->
            selectTrackGroupViewModel.onTrackGroupSelected(groupId)
        })
        binding.groupList.adapter = adapter

        setHasOptionsMenu(true)

        //TODO observe clicks for track groups
        return binding.root
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
        //TODO
    }
}