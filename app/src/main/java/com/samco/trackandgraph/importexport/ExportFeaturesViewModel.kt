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
package com.samco.trackandgraph.importexport

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.data.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

enum class ExportState {
    LOADING,
    WAITING,
    EXPORTING,
    DONE
}

data class FeatureDto(
    val featureId: Long,
    val name: String
)

interface ExportFeaturesViewModel {
    val exportState: StateFlow<ExportState>
    val selectedFileUri: StateFlow<Uri?>
    val availableFeatures: StateFlow<List<FeatureDto>>
    val selectedFeatures: StateFlow<List<FeatureDto>>

    fun loadFeatures(groupId: Long)
    fun reset()
    fun setSelectedFileUri(uri: Uri?)
    fun toggleFeatureSelection(feature: FeatureDto)
    fun beginExport()
}

@HiltViewModel
class ExportFeaturesViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @IODispatcher private val io: CoroutineDispatcher,
    private val contentResolver: ContentResolver
) : ViewModel(), ExportFeaturesViewModel {

    private val _exportState = MutableStateFlow(ExportState.WAITING)
    override val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    override val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()

    private val featuresSet = AtomicBoolean(false)

    private val _availableFeatures = MutableStateFlow<List<FeatureDto>>(emptyList())
    override val availableFeatures: StateFlow<List<FeatureDto>> = _availableFeatures.asStateFlow()

    private val _selectedFeatures = MutableStateFlow<List<FeatureDto>>(emptyList())
    override val selectedFeatures: StateFlow<List<FeatureDto>> = _selectedFeatures.asStateFlow()

    override fun loadFeatures(groupId: Long) {
        if (featuresSet.get() ||_exportState.value != ExportState.WAITING) return
        
        viewModelScope.launch {
            _exportState.value = ExportState.LOADING
            
            try {
                val features = withContext(io) {
                    dataInteractor.getFeaturesForGroupSync(groupId)
                }
                val featureDtos = features.map { it.toFeatureDto() }
                
                _availableFeatures.value = featureDtos
                _selectedFeatures.value = featureDtos // Select all by default
                featuresSet.set(true)
            } finally {
                _exportState.value = ExportState.WAITING
            }
        }
    }

    override fun reset() {
        _exportState.value = ExportState.WAITING
        _selectedFileUri.value = null
        _availableFeatures.value = emptyList()
        _selectedFeatures.value = emptyList()
        featuresSet.set(false)
    }

    override fun setSelectedFileUri(uri: Uri?) {
        _selectedFileUri.value = uri
    }

    override fun toggleFeatureSelection(feature: FeatureDto) {
        val currentSelected = _selectedFeatures.value
        _selectedFeatures.value = if (currentSelected.contains(feature)) {
            currentSelected - feature
        } else {
            currentSelected + feature
        }
    }

    override fun beginExport() {
        _selectedFileUri.value?.let { uri ->
            viewModelScope.launch(ui) {
                _exportState.value = ExportState.EXPORTING
                doExport(uri)
                _exportState.value = ExportState.DONE
            }
        }
    }

    private suspend fun doExport(uri: Uri) = runCatching {
        withContext(io) {
            contentResolver.openOutputStream(uri)?.let { outStream ->
                val featureIds = _selectedFeatures.value.map { it.featureId }
                dataInteractor.writeFeaturesToCSV(outStream, featureIds)
            }
        }
    }

    private fun Feature.toFeatureDto() = FeatureDto(
        featureId = featureId,
        name = name
    )
}
