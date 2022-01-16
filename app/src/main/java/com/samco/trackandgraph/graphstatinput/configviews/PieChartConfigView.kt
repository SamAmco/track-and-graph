/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatinput.configviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.database.entity.PieChart
import com.samco.trackandgraph.database.entity.maxGraphPeriodDurations
import com.samco.trackandgraph.databinding.PieChartInputViewBinding
import com.samco.trackandgraph.graphstatinput.ValidationException

internal class PieChartConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GraphStatConfigView(
    context,
    attrs,
    defStyleAttr
) {
    private val binding: PieChartInputViewBinding = PieChartInputViewBinding
        .inflate(LayoutInflater.from(context), this, true)

    private lateinit var configData: PieChart

    override fun initFromConfigData(configData: Any?) {
        this.configData = configData as PieChart? ?: createEmptyConfig()
        initFromPieChart()
    }

    private fun discreteFeatures(): List<Feature> {
        return allFeatures.filter { ftg -> ftg.featureType == DataType.DISCRETE }
    }

    private fun getCurrentFeature(): Feature? {
        return allFeatures.firstOrNull { it.id == configData.featureId }
    }

    private fun initFromPieChart() {
        binding.sampleDurationSpinner.setSelection(maxGraphPeriodDurations.indexOf(configData.duration))
        listenToTimeDuration(this, binding.sampleDurationSpinner) {
            configData = configData.copy(duration = it)
        }
        binding.endDateSpinner.setSelection(if (configData.endDate == null) 0 else 1)
        listenToEndDate(this, binding.endDateSpinner, { configData.endDate }) {
            configData = configData.copy(endDate = it)
            updateEndDateText(this, binding.customEndDateText, it)
        }

        listenToFeatureSpinner(this, binding.pieChartFeatureSpinner, configData.featureId, {
            ftg -> ftg.featureType == DataType.DISCRETE
        }, {
            configData = configData.copy(featureId = it.id)
        })
    }

    private fun createEmptyConfig(): PieChart = PieChart(
        0,
        0,
        discreteFeatures().getOrElse(0) { null }?.id ?: -1,
        null,
        null
    )

    override fun validateConfig(): ValidationException? {
        if (discreteFeatures().isNullOrEmpty()) {
            binding.pieChartSingleFeatureSelectLabel.visibility = View.GONE
            binding.pieChartFeatureSpinner.visibility = View.GONE
            return ValidationException(R.string.no_discrete_features_pie_chart)
        } else {
            binding.pieChartSingleFeatureSelectLabel.visibility = View.VISIBLE
            binding.pieChartFeatureSpinner.visibility = View.VISIBLE
        }
        val currFeature = getCurrentFeature()
        if (currFeature == null || currFeature.featureType != DataType.DISCRETE) {
            return ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        }
        return null
    }

    override fun getConfigData(): Any = configData
}
