package com.samco.trackandgraph.rateapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.samco.trackandgraph.R

class RateAppRedirectFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navController = container?.findNavController()
        val url = getString(R.string.play_store_page_link)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
        navController?.popBackStack()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}