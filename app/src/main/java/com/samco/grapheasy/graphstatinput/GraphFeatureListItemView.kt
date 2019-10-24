package com.samco.grapheasy.graphstatinput

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.marginBottom
import androidx.core.widget.addTextChangedListener
import com.samco.grapheasy.R
import com.samco.grapheasy.database.FeatureAndTrackGroup
import com.samco.grapheasy.database.LineGraphFeature
import com.samco.grapheasy.database.LineGraphFeatureMode
import com.samco.grapheasy.databinding.ListItemLineGraphFeatureBinding
import java.text.DecimalFormat

class GraphFeatureListItemView(
    context: Context,
    features: List<FeatureAndTrackGroup>,
    colorsList: List<Int>,
    private val lineGraphFeature: LineGraphFeature
) : LinearLayout(context) {
    private var onRemoveListener: ((GraphFeatureListItemView) -> Unit)? = null
    private var onUpdatedListener: ((GraphFeatureListItemView) -> Unit)? = null
    private val decimalFormat = DecimalFormat("0.###############")

    init {
        val binding = ListItemLineGraphFeatureBinding.inflate(LayoutInflater.from(context), this, true)
        val itemNames = features.map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.featureSpinner.adapter = adapter
        var featureSpinnerStartIndex = features.indexOfFirst { f -> f.id == lineGraphFeature.featureId}
        if (featureSpinnerStartIndex == -1) featureSpinnerStartIndex = 0
        binding.featureSpinner.setSelection(featureSpinnerStartIndex)
        binding.featureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.featureId = features[index].id
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
        var modeSpinnerStartIndex = LineGraphFeatureMode.values().indexOfFirst { m -> m == lineGraphFeature.mode}
        if (modeSpinnerStartIndex == -1) modeSpinnerStartIndex = 0
        binding.modeSpinner.setSelection(modeSpinnerStartIndex)
        binding.modeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.mode = LineGraphFeatureMode.values()[index]
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
        binding.offsetInput.setText(decimalFormat.format(lineGraphFeature.offset))
        binding.offsetInput.addTextChangedListener { text ->
            lineGraphFeature.offset = text.toString().toDoubleOrNull() ?: 0.toDouble()
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
        binding.scaleInput.setText(decimalFormat.format(lineGraphFeature.scale))
        binding.scaleInput.addTextChangedListener { text ->
            lineGraphFeature.scale = text.toString().toDoubleOrNull() ?: 1.toDouble()
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
        binding.removeButton.setOnClickListener {
            onRemoveListener?.invoke(this)
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
        binding.colorSpinner.adapter = CustomColorSpinnerAdapter(context, colorsList)
        binding.colorSpinner.setSelection(colorsList.indexOf(lineGraphFeature.colorId))
        binding.colorSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.colorId = colorsList[index]
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    fun setOnUpdateListener(onUpdatedListener: (GraphFeatureListItemView) -> Unit) {
        this.onUpdatedListener = onUpdatedListener
    }

    fun setOnRemoveListener(onRemoveListener: (GraphFeatureListItemView) -> Unit) {
        this.onRemoveListener = onRemoveListener
    }

    private class CustomColorSpinnerAdapter(context: Context, val imageIds: List<Int>)
        : ArrayAdapter<Int>(context, R.layout.spinner_item_color) {

        override fun getCount() = imageIds.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.spinner_item_color, parent, false)
            view.findViewById<ImageView>(R.id.image).setColorFilter(getColor(context, imageIds[position]))
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }
    }
}
