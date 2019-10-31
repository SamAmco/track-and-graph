package com.samco.grapheasy.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.samco.grapheasy.databinding.GraphStatCardViewBinding
import com.samco.grapheasy.databinding.GraphStatViewBinding

class GraphStatCardView : GraphStatViewBase {
    private lateinit var binding: GraphStatCardViewBinding

    constructor(context: Context) : super(context)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    var menuButtonClickListener: ((v: View) -> Unit)? = null

    init {
        listenToMenuButton()
    }

    private fun listenToMenuButton() {
        binding.menuButton.setOnClickListener {
            menuButtonClickListener?.invoke(binding.menuButton)
        }
    }

    fun hideMenuButton() {
        binding.menuButton.visibility = View.GONE
    }

    override fun getBinding(): GraphStatViewBinding {
        binding = GraphStatCardViewBinding.inflate(LayoutInflater.from(context), this, true)
        return binding.graphStatView
    }
}