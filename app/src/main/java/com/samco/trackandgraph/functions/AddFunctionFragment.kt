package com.samco.trackandgraph.functions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.FragmentAddFunctionBinding
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddFunctionFragment : Fragment() {

    private var binding: FragmentAddFunctionBinding by bindingForViewLifecycle()
    private val args: AddFunctionFragmentArgs by navArgs()
    private var navController: NavController? = null
    private val viewModel: AddFunctionViewModel by viewModels<AddFunctionViewModelImpl>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initViewModel(args.groupId, args.functionId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddFunctionBinding.inflate(inflater, container, false)
        navController = container?.findNavController()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindFunctionName()
        bindFunctionDescription()
        bindFunctionBody()
        bindFinishButton()
        bindLoadingOverlay()
    }

    private fun bindLoadingOverlay() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loadingOverlay.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun bindFinishButton() {
        viewModel.isUpdateMode.observe(viewLifecycleOwner) {
            binding.addBar.addButton.text =
                if (it) requireContext().getString(R.string.update)
                else requireContext().getString(R.string.finish)
        }
        viewModel.createUpdateButtonEnabled.observe(viewLifecycleOwner) {
            binding.addBar.addButton.isEnabled = it
        }
        binding.addBar.addButton.setOnClickListener {
            viewModel.onCreateOrUpdateClicked()
        }
        viewModel.finished.observe(viewLifecycleOwner) {
            if (it) navController?.popBackStack()
        }
    }

    private fun bindFunctionBody() {
        viewModel.functionScript.observe(viewLifecycleOwner) {
            if (binding.functionBodyText.text.toString() != it)
                binding.functionBodyText.setText(it)
        }
        binding.functionBodyText.addTextChangedListener { viewModel.setFunctionScript(it.toString()) }
    }

    private fun bindFunctionDescription() {
        viewModel.functionDescription.observe(viewLifecycleOwner) {
            if (binding.functionDescriptionText.text.toString() != it)
                binding.functionDescriptionText.setText(it)
        }
        binding.functionDescriptionText.addTextChangedListener { viewModel.setFunctionDescription(it.toString()) }
    }

    private fun bindFunctionName() {
        viewModel.functionName.observe(viewLifecycleOwner) {
            if (binding.functionNameText.text.toString() != it)
                binding.functionNameText.setText(it)
        }
        binding.functionNameText.addTextChangedListener { viewModel.setFunctionName(it.toString()) }
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