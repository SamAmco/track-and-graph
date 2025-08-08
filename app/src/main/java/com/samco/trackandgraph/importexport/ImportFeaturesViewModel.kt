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
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.ImportFeaturesException
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ImportState { WAITING, IMPORTING, DONE }

interface ImportFeaturesModuleViewModel {
    val selectedFileUri: StateFlow<Uri?>
    val importState: StateFlow<ImportState>
    val importException: StateFlow<ImportFeaturesException?>
    
    fun setSelectedFileUri(uri: Uri?)
    fun beginImport(trackGroupId: Long)
    fun clearException()
    fun reset()
}

@HiltViewModel
class ImportFeaturesModuleViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val contentResolver: ContentResolver,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), ImportFeaturesModuleViewModel {

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    override val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()

    private val _importState = MutableStateFlow(ImportState.WAITING)
    override val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _importException = MutableStateFlow<ImportFeaturesException?>(null)
    override val importException: StateFlow<ImportFeaturesException?> = _importException.asStateFlow()

    override fun setSelectedFileUri(uri: Uri?) {
        _selectedFileUri.value = uri
    }

    override fun beginImport(trackGroupId: Long) {
        if (_importState.value == ImportState.IMPORTING) return
        val uri = _selectedFileUri.value ?: return
        viewModelScope.launch(ui) {
            _importState.value = ImportState.IMPORTING
            doImport(uri, trackGroupId).exceptionOrNull()?.let {
                if (it is ImportFeaturesException) _importException.value = it
                else _importException.value = ImportFeaturesException.Unknown()
            }
            _importState.value = ImportState.DONE
        }
    }

    override fun clearException() {
        _importException.value = null
    }

    override fun reset() {
        _selectedFileUri.value = null
        _importException.value = null
        _importState.value = ImportState.WAITING
    }

    private suspend fun doImport(uri: Uri, trackGroupId: Long) = runCatching {
        withContext(io) {
            contentResolver.openInputStream(uri)?.let { inputStream ->
                dataInteractor.readFeaturesFromCSV(inputStream, trackGroupId)
            }
        }
    }
}