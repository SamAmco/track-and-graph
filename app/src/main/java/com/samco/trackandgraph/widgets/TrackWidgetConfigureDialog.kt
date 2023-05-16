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

package com.samco.trackandgraph.widgets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.databinding.TrackWidgetConfigureDialogBinding
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TrackWidgetConfigureDialog : DialogFragment() {
    private val viewModel by viewModels<TrackWidgetConfigureDialogViewModel>()
    private lateinit var binding: TrackWidgetConfigureDialogBinding
    private lateinit var listener: TrackWidgetConfigureDialogListener

    internal interface TrackWidgetConfigureDialogListener {
        fun onCreateWidget(featureId: Long?)
        fun onNoFeatures()
        fun onDismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = TrackWidgetConfigureDialogBinding.inflate(it.layoutInflater, null, false)
            listener = activity as TrackWidgetConfigureDialogListener

            val alertDialog = MaterialAlertDialogBuilder(it, R.style.AppTheme_AlertDialogTheme)
                .setView(binding.root)
                .setPositiveButton(R.string.create) { _, _ ->
                    viewModel.onCreateClicked()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    dismiss()
                    listener.onDismiss()
                }
                .create()

            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener { setAlertDialogShowListeners() }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setAlertDialogShowListeners() {
        observeAllFeatures()
        observeOnCreateClicked()
    }

    private fun observeOnCreateClicked() {
        viewModel.onCreateWidget.observe(this) {
            listener.onCreateWidget(it)
        }
    }

    private fun observeAllFeatures() =
        viewModel.featurePathProvider.observe(this) { featurePathProvider ->
            val sortedFeatures = featurePathProvider.sortedPaths()
            if (sortedFeatures.isEmpty()) listener.onNoFeatures()
            val itemNames = sortedFeatures.map { it.second }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                itemNames
            )
            binding.trackerSpinner.adapter = adapter
            binding.trackerSpinner.setSelection(0)
            binding.trackerSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        viewModel.onFeatureSelected(sortedFeatures[position].first.featureId)
                    }
                }
        }

    override fun onDismiss(dialog: DialogInterface) {
        listener.onDismiss()
    }
}

@HiltViewModel
class TrackWidgetConfigureDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _featurePathProvider = MutableLiveData<FeaturePathProvider>()
    val featurePathProvider: LiveData<FeaturePathProvider> get() = _featurePathProvider

    private val _onCreateWidget = MutableLiveData<Long?>()
    val onCreateWidget: LiveData<Long?> get() = _onCreateWidget

    private var selectedFeatureId: Long? = null

    init {
        viewModelScope.launch(io) {
            val groups = dataInteractor.getAllGroupsSync()
            val trackers = dataInteractor.getAllTrackersSync()
            withContext(ui) {
                _featurePathProvider.value = FeaturePathProvider(trackers, groups)
            }
        }
    }

    fun onCreateClicked() {
        _onCreateWidget.value = selectedFeatureId
    }

    fun onFeatureSelected(featureId: Long) {
        selectedFeatureId = featureId
    }
}
