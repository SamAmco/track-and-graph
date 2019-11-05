package com.samco.trackandgraph.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import com.samco.trackandgraph.databinding.GraphStatCardViewBinding
import com.samco.trackandgraph.databinding.GraphStatViewBinding

class GraphStatCardView : GraphStatViewBase {
    private lateinit var binding: GraphStatCardViewBinding
    lateinit var cardView: CardView
        private set

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
        cardView = binding.demoCardView
        return binding.graphStatView
    }
}