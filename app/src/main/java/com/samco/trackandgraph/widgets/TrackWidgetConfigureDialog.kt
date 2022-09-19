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

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.databinding.TrackWidgetConfigureDialogBinding
import com.samco.trackandgraph.ui.FeaturePathProvider
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return activity?.let {
            binding = TrackWidgetConfigureDialogBinding.inflate(inflater, container, false)
            listener = activity as TrackWidgetConfigureDialogListener

            binding.cancelButton.setOnClickListener {
                dismiss()
                listener.onDismiss()
            }
            observeAllFeatures()
            observeOnCreateClicked()

            dialog?.setCanceledOnTouchOutside(true)
            binding.root
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun observeOnCreateClicked() {
        binding.createButton.setOnClickListener {
            viewModel.onCreateClicked()
        }
        viewModel.onCreateWidget.observe(viewLifecycleOwner) {
            listener.onCreateWidget(it)
        }
    }

    private fun observeAllFeatures() =
        viewModel.featurePathProvider.observe(viewLifecycleOwner) { featurePathProvider ->
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
