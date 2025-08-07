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

    private suspend fun doImport(uri: Uri, trackGroupId: Long) = runCatching {
        withContext(io) {
            contentResolver.openInputStream(uri)?.let { inputStream ->
                dataInteractor.readFeaturesFromCSV(inputStream, trackGroupId)
            }
        }
    }
}