package com.samco.trackandgraph.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor
import com.samco.trackandgraph.database.dataVisColorList
import com.samco.trackandgraph.databinding.ListItemGraphLegendBinding

class GraphLegendItemView(context: Context, colorId: Int, text: String): FrameLayout(context) {
    private var binding = ListItemGraphLegendBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        binding.circleImage.setColorFilter(getColor(context, colorId))
        binding.labelText.text = text
    }
}