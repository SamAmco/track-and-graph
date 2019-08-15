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
import com.samco.grapheasy.databinding.AddTrackGroupOverlayBinding
import kotlinx.coroutines.*

class AddTrackGroupOverlayFragment : Fragment() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)
    private lateinit var binding: AddTrackGroupOverlayBinding
    private lateinit var dataSource: GraphEasyDatabaseDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.add_track_group_overlay, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val application = requireNotNull(this.activity).application
        dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao

        binding.addButton.setOnClickListener { onAddClicked() }
        binding.executePendingBindings()
        retainInstance = true
        return binding.root
    }

    private fun onAddClicked() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val trackGroup = TrackGroup(0, binding.nameInput.text.toString())
                dataSource.insertTrackGroup(trackGroup)
            }
            fragmentManager?.beginTransaction()?.remove(this@AddTrackGroupOverlayFragment)?.commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob.cancel()
    }
}