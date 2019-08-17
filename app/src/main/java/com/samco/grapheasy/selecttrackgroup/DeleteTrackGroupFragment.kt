package com.samco.grapheasy.selecttrackgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.database.TrackGroup
import com.samco.grapheasy.databinding.DeleteTrackGroupOverlayBinding
import kotlinx.coroutines.*

class DeleteTrackGroupFragment(val trackGroup: TrackGroup) : Fragment() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)
    private lateinit var binding: DeleteTrackGroupOverlayBinding
    private lateinit var dataSource: GraphEasyDatabaseDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.delete_track_group_overlay, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val application = requireNotNull(this.activity).application
        dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao

        binding.deleteButton.setOnClickListener { onDeleteClicked() }
        binding.background.setOnClickListener { deleteFragment() }

        retainInstance = true
        return binding.root
    }

    private fun onDeleteClicked() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dataSource.deleteTrackGroup(trackGroup)
            }
            deleteFragment()
        }
    }

    private fun deleteFragment() {
        fragmentManager?.beginTransaction()?.remove(this@DeleteTrackGroupFragment)?.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob.cancel()
    }
}