package com.samco.trackandgraph.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.samco.trackandgraph.databinding.Faq4Binding

class FAQ4Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return Faq4Binding.inflate(inflater, container, false).root
    }
}
