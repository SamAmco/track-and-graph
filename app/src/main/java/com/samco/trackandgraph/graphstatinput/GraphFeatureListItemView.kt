/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.graphstatinput

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.ListItemLineGraphFeatureBinding
import timber.log.Timber
import java.text.DecimalFormat

class GraphFeatureListItemView(
    context: Context,
    private val features: List<FeatureAndTrackGroup>,
    private val colorsList: List<Int>,
    private val lineGraphFeature: LineGraphFeature
) : LinearLayout(context) {
    private val binding = ListItemLineGraphFeatureBinding.inflate(LayoutInflater.from(context), this, true)
    private var onRemoveListener: ((GraphFeatureListItemView) -> Unit)? = null
    private var onUpdatedListener: ((GraphFeatureListItemView) -> Unit)? = null
    private val decimalFormat = DecimalFormat("0.###############")

    init {
        setupFeatureSpinner()
        setupGraphFeatureName()
        setupAveragingModeSpinner()
        setupPlottingModeSpinner()
        setupOffsetInput()
        setupScaleInput()
        setupRemoveButton()
        setupColorSpinner(context)
    }

    private fun setupGraphFeatureName() {
        binding.lineGraphFeatureName.filters = arrayOf(InputFilter.LengthFilter(MAX_LINE_GRAPH_FEATURE_NAME_LENGTH))
        if (lineGraphFeature.name.isNotEmpty()) binding.lineGraphFeatureName.setText(lineGraphFeature.name)
        binding.lineGraphFeatureName.addTextChangedListener { text ->
            if(lineGraphFeature.name != text.toString()) {
                lineGraphFeature.name = text.toString()
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    private fun setupColorSpinner(context: Context) {
        binding.colorSpinner.adapter = CustomColorSpinnerAdapter(context, colorsList)
        binding.colorSpinner.setSelection(lineGraphFeature.colorIndex)
        binding.colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.colorIndex = index
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    private fun setupRemoveButton() {
        binding.removeButton.setOnClickListener {
            onRemoveListener?.invoke(this)
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
    }

    private fun setupScaleInput() {
        binding.scaleInput.setText(decimalFormat.format(lineGraphFeature.scale))
        binding.scaleInput.addTextChangedListener { text ->
            lineGraphFeature.scale = text.toString().toDoubleOrNull() ?: 1.toDouble()
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
    }

    private fun setupOffsetInput() {
        binding.offsetInput.setText(decimalFormat.format(lineGraphFeature.offset))
        binding.offsetInput.addTextChangedListener { text ->
            lineGraphFeature.offset = text.toString().toDoubleOrNull() ?: 0.toDouble()
            onUpdatedListener?.invoke(this@GraphFeatureListItemView)
        }
    }

    private fun setupPlottingModeSpinner() {
        var modeSpinnerIndex = LineGraphPlottingModes.values().indexOfFirst { m -> m == lineGraphFeature.plottingMode }
        if (modeSpinnerIndex == -1) modeSpinnerIndex = 0
        binding.plottingModeSpinner.setSelection(modeSpinnerIndex)
        binding.plottingModeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.plottingMode = LineGraphPlottingModes.values()[index]
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    private fun setupAveragingModeSpinner() {
        var modeSpinnerStartIndex = LineGraphAveraginModes.values().indexOfFirst { m -> m == lineGraphFeature.averagingMode}
        if (modeSpinnerStartIndex == -1) modeSpinnerStartIndex = 0
        binding.averagingModeSpinner.setSelection(modeSpinnerStartIndex)
        binding.averagingModeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.averagingMode = LineGraphAveraginModes.values()[index]
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    private fun setupFeatureSpinner() {
        val itemNames = features.map { ft -> "${ft.trackGroupName} -> ${ft.name}" }
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.featureSpinner.adapter = adapter
        var featureSpinnerStartIndex = features.indexOfFirst { f -> f.id == lineGraphFeature.featureId}
        if (featureSpinnerStartIndex == -1) featureSpinnerStartIndex = 0
        binding.featureSpinner.setSelection(featureSpinnerStartIndex)
        binding.featureSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                onFeatureChangeNameUpdate(lineGraphFeature.featureId, lineGraphFeature.name, features[index].name)
                lineGraphFeature.featureId = features[index].id
                onUpdatedListener?.invoke(this@GraphFeatureListItemView)
            }
        }
    }

    private fun onFeatureChangeNameUpdate(oldFeatureId: Long, oldFeatureName: String, newFeatureName: String) {
        if (oldFeatureId == -1L || oldFeatureName == "") binding.lineGraphFeatureName.setText(newFeatureName)
        val oldFeatureDBName = features.firstOrNull { f -> f.id == oldFeatureId }?.name ?: return
        if (oldFeatureDBName == oldFeatureName || oldFeatureName == "") {
            binding.lineGraphFeatureName.setText(newFeatureName)
        }
    }

    fun setOnUpdateListener(onUpdatedListener: (GraphFeatureListItemView) -> Unit) {
        this.onUpdatedListener = onUpdatedListener
    }

    fun setOnRemoveListener(onRemoveListener: (GraphFeatureListItemView) -> Unit) {
        this.onRemoveListener = onRemoveListener
    }

    private class CustomColorSpinnerAdapter(context: Context, val colorIds: List<Int>)
        : ArrayAdapter<Int>(context, R.layout.spinner_item_color) {

        override fun getCount() = colorIds.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.spinner_item_color, parent, false)
            view.findViewById<ImageView>(R.id.image).setColorFilter(getColor(context, colorIds[position]))
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }
    }
}
