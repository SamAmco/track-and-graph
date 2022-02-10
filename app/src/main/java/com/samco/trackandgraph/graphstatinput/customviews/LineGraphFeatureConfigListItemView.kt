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
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.samco.trackandgraph.graphstatinput.customviews

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.databinding.ListItemLineGraphFeatureBinding
import com.samco.trackandgraph.ui.ColorSpinnerAdapter
import com.samco.trackandgraph.ui.FeaturePathProvider
import com.samco.trackandgraph.util.getDoubleFromText
import java.text.DecimalFormat

class LineGraphFeatureConfigListItemView(
    context: Context,
    private val featurePathProvider: FeaturePathProvider,
    private val lineGraphFeature: LineGraphFeatureConfig
) : LinearLayout(context) {
    private val binding =
        ListItemLineGraphFeatureBinding.inflate(LayoutInflater.from(context), this, true)
    private var onRemoveListener: ((LineGraphFeature) -> Unit)? = null
    private var onUpdatedListener: ((LineGraphFeature) -> Unit)? = null
    private val decimalFormat = DecimalFormat("0.###############")
    private var pointStyleSpinnerAdapter: PointStyleSpinnerAdapter? = null

    init {
        setupFeatureSpinner()
        setupGraphFeatureName()
        setupAveragingModeSpinner()
        setupPlottingModeSpinner()
        setupOffsetInput()
        setupScaleInput()
        setupRemoveButton()
        setupColorSpinner(context)
        setupPointStyleSpinner(context)
    }

    private fun setupGraphFeatureName() {
        if (lineGraphFeature.name.isNotEmpty()) binding.lineGraphFeatureName.setText(
            lineGraphFeature.name
        )
        binding.lineGraphFeatureName.addTextChangedListener { text ->
            if (lineGraphFeature.name != text.toString()) {
                lineGraphFeature.name = text.toString()
                onUpdatedListener?.invoke(LineGraphFeatureConfig.toLineGraphFeature(lineGraphFeature))
            }
        }
    }

    private fun setupColorSpinner(context: Context) {
        binding.colorSpinner.adapter = ColorSpinnerAdapter(context, dataVisColorList)
        binding.colorSpinner.setSelection(lineGraphFeature.colorIndex)
        binding.colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                lineGraphFeature.colorIndex = index
                onUpdatedListener?.invoke(LineGraphFeatureConfig.toLineGraphFeature(lineGraphFeature))
            }
        }
    }

    private fun setupPointStyleSpinner(context: Context) {
        val pointStyleImages = mapOf(
            LineGraphPointStyle.NONE to pointStyleDrawableResources[0],
            LineGraphPointStyle.CIRCLES to pointStyleDrawableResources[1],
            LineGraphPointStyle.CIRCLES_AND_NUMBERS to pointStyleDrawableResources[2]
        )
        pointStyleSpinnerAdapter = PointStyleSpinnerAdapter(context, pointStyleImages)
        binding.pointStyleSpinner.adapter = pointStyleSpinnerAdapter
        binding.pointStyleSpinner.setSelection(lineGraphFeature.pointStyle.ordinal)
        binding.pointStyleSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    lineGraphFeature.pointStyle = pointStyleImages.keys.elementAt(index)
                    onUpdatedListener?.invoke(
                        LineGraphFeatureConfig.toLineGraphFeature(
                            lineGraphFeature
                        )
                    )
                }
            }
    }

    private fun setupRemoveButton() {
        binding.removeButton.setOnClickListener {
            onRemoveListener?.invoke(LineGraphFeatureConfig.toLineGraphFeature(lineGraphFeature))
        }
    }

    private fun setupScaleInput() {
        binding.scaleInput.setText(decimalFormat.format(lineGraphFeature.scale))
        binding.scaleInput.addTextChangedListener { text ->
            lineGraphFeature.scale = getDoubleFromText(text.toString())
            onUpdatedListener?.invoke(LineGraphFeatureConfig.toLineGraphFeature(lineGraphFeature))
        }
    }

    private fun setupOffsetInput() {
        binding.offsetInput.setText(decimalFormat.format(lineGraphFeature.offset))
        binding.offsetInput.addTextChangedListener { text ->
            lineGraphFeature.offset = getDoubleFromText(text.toString())
            onUpdatedListener?.invoke(LineGraphFeatureConfig.toLineGraphFeature(lineGraphFeature))
        }
    }

    private fun setupPlottingModeSpinner() {
        var modeSpinnerIndex =
            LineGraphPlottingModes.values().indexOfFirst { m -> m == lineGraphFeature.plottingMode }
        if (modeSpinnerIndex == -1) modeSpinnerIndex = 0
        binding.plottingModeSpinner.setSelection(modeSpinnerIndex)
        binding.plottingModeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    lineGraphFeature.plottingMode = LineGraphPlottingModes.values()[index]
                    onUpdatedListener?.invoke(
                        LineGraphFeatureConfig.toLineGraphFeature(
                            lineGraphFeature
                        )
                    )
                }
            }
    }

    private fun setupAveragingModeSpinner() {
        var modeSpinnerStartIndex = LineGraphAveraginModes.values()
            .indexOfFirst { m -> m == lineGraphFeature.averagingMode }
        if (modeSpinnerStartIndex == -1) modeSpinnerStartIndex = 0
        binding.averagingModeSpinner.setSelection(modeSpinnerStartIndex)
        binding.averagingModeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    lineGraphFeature.averagingMode = LineGraphAveraginModes.values()[index]
                    onUpdatedListener?.invoke(
                        LineGraphFeatureConfig.toLineGraphFeature(
                            lineGraphFeature
                        )
                    )
                }
            }
    }

    private fun setupFeatureSpinner() {
        val items = featurePathProvider.featuresSortedAlphabetically().flatMap { getSpinnerItemsForFeature(it) }
        val itemNames = items.map { it.third }
        val adapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
        binding.featureSpinner.adapter = adapter
        var featureSpinnerStartIndex =
            items.indexOfFirst { it.first.id == lineGraphFeature.featureId && it.second == lineGraphFeature.durationPlottingMode }
        if (featureSpinnerStartIndex == -1) featureSpinnerStartIndex = 0
        binding.featureSpinner.setSelection(featureSpinnerStartIndex)
        binding.featureSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    onFeatureChangeNameUpdate(
                        lineGraphFeature.featureId,
                        lineGraphFeature.name,
                        items[index].first.name
                    )
                    lineGraphFeature.featureId = items[index].first.id
                    lineGraphFeature.durationPlottingMode = items[index].second
                    adjustPointStyleSpinnerForFeature()
                    onUpdatedListener?.invoke(
                        LineGraphFeatureConfig.toLineGraphFeature(
                            lineGraphFeature
                        )
                    )
                }
            }
    }

    private fun adjustPointStyleSpinnerForFeature() {
        if (lineGraphFeature.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE) {
            pointStyleSpinnerAdapter?.disableNumbers()
            val selected = binding.pointStyleSpinner.selectedItemPosition
            if (pointStyleSpinnerAdapter?.isEnabled(selected) == false) {
                binding.pointStyleSpinner.setSelection(selected - 1)
            }
        } else {
            pointStyleSpinnerAdapter?.enableNumbers()
        }
    }

    private fun getSpinnerItemsForFeature(feature: Feature)
            : List<Triple<Feature, DurationPlottingMode, String>> {
        val name = featurePathProvider.getPathForFeature(feature.id)
        return if (feature.featureType == DataType.DURATION) {
            val time = context.getString(R.string.time_duration)
            val hours = context.getString(R.string.hours)
            val minutes = context.getString(R.string.minutes)
            val seconds = context.getString(R.string.seconds)
            listOf(
                Triple(feature, DurationPlottingMode.DURATION_IF_POSSIBLE, "$name ($time)"),
                Triple(feature, DurationPlottingMode.HOURS, "$name ($hours)"),
                Triple(feature, DurationPlottingMode.MINUTES, "$name ($minutes)"),
                Triple(feature, DurationPlottingMode.SECONDS, "$name ($seconds)")
            )
        } else listOf(Triple(feature, DurationPlottingMode.NONE, name))
    }

    private fun onFeatureChangeNameUpdate(
        oldFeatureId: Long,
        oldFeatureName: String,
        newFeatureName: String
    ) {
        if (oldFeatureId == -1L || oldFeatureName == "") binding.lineGraphFeatureName.setText(
            newFeatureName
        )
        val oldFeatureDBName =
            featurePathProvider.features.firstOrNull { f -> f.id == oldFeatureId }?.name ?: return
        if (oldFeatureDBName == oldFeatureName || oldFeatureName == "") {
            binding.lineGraphFeatureName.setText(newFeatureName)
        }
    }

    fun setOnUpdateListener(onUpdatedListener: (LineGraphFeature) -> Unit) {
        this.onUpdatedListener = onUpdatedListener
    }

    fun setOnRemoveListener(onRemoveListener: (LineGraphFeature) -> Unit) {
        this.onRemoveListener = onRemoveListener
    }

    private class PointStyleSpinnerAdapter(
        context: Context,
        val pointStyleImages: Map<LineGraphPointStyle, Int>
    ) :
        ArrayAdapter<Int>(context, R.layout.circular_spinner_item) {

        var enabledStyles = pointStyleImages.keys
        var imageIds = pointStyleImages.values

        override fun getCount(): Int = imageIds.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.circular_spinner_item, parent, false)
            view.findViewById<ImageView>(R.id.image).setImageResource(imageIds.elementAt(position))
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = getView(position, convertView, parent)
            if (!isEnabled(position)) view.alpha = 0.5f
            else view.alpha = 1.0f
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            return enabledStyles.contains(pointStyleImages.keys.elementAt(position))
        }

        fun disableNumbers() {
            enabledStyles = pointStyleImages
                .filter { kvp -> kvp.key != LineGraphPointStyle.CIRCLES_AND_NUMBERS }
                .keys
        }

        fun enableNumbers() {
            enabledStyles = pointStyleImages.keys
        }
    }
}

data class LineGraphFeatureConfig(
    var featureId: Long,
    var name: String,
    var colorIndex: Int,
    var averagingMode: LineGraphAveraginModes,
    var plottingMode: LineGraphPlottingModes,
    var pointStyle: LineGraphPointStyle,
    var offset: Double,
    var scale: Double,
    var durationPlottingMode: DurationPlottingMode
) {
    companion object {
        fun fromLineGraphFeature(lgf: LineGraphFeature) = LineGraphFeatureConfig(
            lgf.featureId,
            lgf.name,
            lgf.colorIndex,
            lgf.averagingMode,
            lgf.plottingMode,
            lgf.pointStyle,
            lgf.offset,
            lgf.scale,
            lgf.durationPlottingMode
        )

        fun toLineGraphFeature(
            lgfc: LineGraphFeatureConfig,
            id: Long = 0L,
            lineGraphId: Long = 0L
        ) = LineGraphFeature(
            id,
            lineGraphId,
            lgfc.featureId,
            lgfc.name,
            lgfc.colorIndex,
            lgfc.averagingMode,
            lgfc.plottingMode,
            lgfc.pointStyle,
            lgfc.offset,
            lgfc.scale,
            lgfc.durationPlottingMode
        )
    }
}
