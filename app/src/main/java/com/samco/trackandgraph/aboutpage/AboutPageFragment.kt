package com.samco.trackandgraph.aboutpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.samco.trackandgraph.databinding.AboutPageBinding
import android.content.Intent
import android.net.Uri
import com.samco.trackandgraph.R


class AboutPageFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = AboutPageBinding.inflate(inflater, container, false)
        val url = getString(R.string.github_link)
        binding.githubLinkButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
        return binding.root
    }
}