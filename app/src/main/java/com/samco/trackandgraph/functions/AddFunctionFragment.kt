package com.samco.trackandgraph.functions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.FragmentAddFunctionBinding
import com.samco.trackandgraph.group.GroupFragmentArgs
import com.samco.trackandgraph.util.bindingForViewLifecycle

class AddFunctionFragment : Fragment() {

    private var binding: FragmentAddFunctionBinding by bindingForViewLifecycle()
    private val args: GroupFragmentArgs by navArgs()
    private val viewModel by viewModels<AddFunctionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initViewModel(args.groupId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddFunctionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val activity = (requireActivity() as MainActivity)
        activity.setActionBarConfig(
            NavButtonStyle.UP,
            getString(R.string.add_function)
        )
    }
}