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

import android.app.DatePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.helpers.formatDayMonthYear
import com.samco.trackandgraph.graphstatinput.ValidationException
import com.samco.trackandgraph.maxGraphPeriodDurations
import com.samco.trackandgraph.ui.ExtendedSpinner
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

abstract class GraphStatConfigView constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(
    context,
    attrs,
    defStyleAttr
) {
    protected lateinit var featureDataProvider: FeatureDataProvider

    protected val allFeatureData: List<FeatureDataProvider.FeatureData> get() = featureDataProvider.featureData

    private var configChangedListener: ((Any?, ValidationException?) -> Unit)? = null
    protected var onScrollListener: ((Int) -> Unit)? = null
    protected var onHideKeyboardListener: (() -> Unit)? = null

    abstract fun initFromConfigData(configData: Any?)

    internal fun initFromConfigData(configData: Any?, featurePathProvider: FeatureDataProvider) {
        this.featureDataProvider = featurePathProvider
        initFromConfigData(configData)
    }

    internal fun setConfigChangedListener(configChangedListener: (Any?, ValidationException?) -> Unit) {
        this.configChangedListener = configChangedListener
    }

    internal fun setOnScrollListener(onScrollListener: (Int) -> Unit) {
        this.onScrollListener = onScrollListener
    }

    internal fun setOnHideKeyboardListener(onHideKeyboardListener: () -> Unit) {
        this.onHideKeyboardListener = onHideKeyboardListener
    }

    protected abstract fun validateConfig(): ValidationException?
    protected abstract fun getConfigData(): Any

    protected fun emitConfigChange() {
        val validationException = validateConfig()
        configChangedListener?.invoke(getConfigData(), validationException)
    }

    companion object {

        internal fun listenToFeatureSpinner(
            view: GraphStatConfigView,
            spinner: AppCompatSpinner,
            selectedId: Long,
            featureFilter: (Feature) -> Boolean,
            onItemSelected: (Feature) -> Unit
        ) {
            val allFeatures = view.featureDataProvider.featuresSortedAlphabetically().filter(featureFilter)
            val context = view.context
            val itemNames = allFeatures.map { ft -> view.featureDataProvider.getPathForFeature(ft.id) }
            val adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, itemNames)
            spinner.adapter = adapter
            val selected = allFeatures.indexOfFirst { it.id == selectedId }
            if (selected >= 0) spinner.setSelection(selected)
            spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        index: Int,
                        p3: Long
                    ) {
                        onItemSelected.invoke(allFeatures[index])
                        view.emitConfigChange()
                    }
                }
        }

        internal fun listenToFeatureSpinner(
            view: GraphStatConfigView,
            spinner: AppCompatSpinner,
            selectedId: Long,
            onItemSelected: (Feature) -> Unit
        ) {
            listenToFeatureSpinner(view, spinner, selectedId, { true }, onItemSelected)
        }

        internal fun listenToTimeDuration(
            view: GraphStatConfigView,
            sampleDurationSpinner: AppCompatSpinner,
            onItemSelected: (Duration?) -> Unit
        ) {
            sampleDurationSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        index: Int,
                        p3: Long
                    ) {
                        onItemSelected(maxGraphPeriodDurations[index])
                        view.emitConfigChange()
                    }
                }
        }

        internal fun updateEndDateText(
            view: GraphStatConfigView,
            textView: TextView,
            endDate: OffsetDateTime?
        ) {
            textView.text = endDate?.let { "(${formatDayMonthYear(view.context, it)})" } ?: ""
            textView.visibility = if (endDate == null) View.GONE else View.VISIBLE
        }

        internal fun listenToEndDate(
            view: GraphStatConfigView,
            endDateSpinner: ExtendedSpinner,
            getCurrentEndDate: () -> OffsetDateTime?,
            onItemSelected: (OffsetDateTime?) -> Unit
        ) {
            val onItemListener = { index: Int ->
                when (index) {
                    0 -> {
                        onItemSelected(null)
                        view.emitConfigChange()
                    }
                    else -> onUserSelectedCustomEndDate(view, getCurrentEndDate(), onItemSelected)
                }
            }
            endDateSpinner.setOnItemClickedListener(
                object : ExtendedSpinner.OnItemClickedListener {
                    override fun onItemClicked(index: Int) {
                        onItemListener(index)
                    }
                }
            )
        }

        private fun onUserSelectedCustomEndDate(
            view: GraphStatConfigView,
            currentEndDate: OffsetDateTime?,
            onItemSelected: (OffsetDateTime?) -> Unit
        ) {
            val suggestedDate = currentEndDate ?: OffsetDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusDays(1)
                .minusNanos(1)//The very end of today
            onItemSelected(suggestedDate)
            view.emitConfigChange()
            val picker = DatePickerDialog(
                view.context, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val selectedDate =
                        ZonedDateTime.of(suggestedDate.toLocalDateTime(), ZoneId.systemDefault())
                            .withYear(year)
                            .withMonth(month + 1)
                            .withDayOfMonth(day)
                            .toOffsetDateTime()
                    onItemSelected(selectedDate)
                    view.emitConfigChange()
                }, suggestedDate.year, suggestedDate.monthValue - 1, suggestedDate.dayOfMonth
            )
            picker.show()
        }
    }
}