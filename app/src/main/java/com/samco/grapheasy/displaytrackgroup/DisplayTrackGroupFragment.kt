package com.samco.grapheasy.displaytrackgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.grapheasy.R
import com.samco.grapheasy.databinding.FragmentDisplayTrackGroupBinding

class DisplayTrackGroupFragment : Fragment(){
    private var navController: NavController? = null
    private val args: DisplayTrackGroupFragmentArgs by navArgs()

    private lateinit var binding: FragmentDisplayTrackGroupBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_display_track_group, container, false)


        return binding.root
    }
}