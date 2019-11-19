package com.samco.trackandgraph.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.samco.trackandgraph.databinding.FaqPageBinding

class FAQFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FaqPageBinding.inflate(inflater, container, false)
        val navController = container?.findNavController()
        binding.faq1.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq1()) }
        binding.faq2.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq2()) }
        binding.faq3.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq3()) }
        binding.faq4.setOnClickListener { navController?.navigate(FAQFragmentDirections.actionFaq4()) }
        binding.faq5.setOnClickListener { }
        return binding.root
    }
}