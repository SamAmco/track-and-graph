package com.samco.grapheasy.graphsandstats

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel

import com.samco.grapheasy.R

class GraphsAndStatsFragment : Fragment() {
    private lateinit var viewModel: GraphsAndStatsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        return inflater.inflate(R.layout.graphs_and_stats_fragment, container, false)
    }
}

class GraphsAndStatsViewModel : ViewModel() {
    // TODO: Implement GraphsAndStatsViewModel ViewModel
}
