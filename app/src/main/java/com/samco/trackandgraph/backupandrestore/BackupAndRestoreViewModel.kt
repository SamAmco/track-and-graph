@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.backupandrestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.BackupRestoreInteractor
import com.samco.trackandgraph.base.model.BackupResult
import com.samco.trackandgraph.base.model.RestoreResult
import com.samco.trackandgraph.base.model.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.samco.trackandgraph.backupandrestore.BackupAndRestoreViewModel.OperationState as OperationState

interface BackupAndRestoreViewModel {
    sealed class OperationState {
        object Idle : OperationState()
        object InProgress : OperationState()
        object Success : OperationState()
        data class Error(val stringResource: Int) : OperationState()
    }

    val restoreState: StateFlow<OperationState>
    val backupState: StateFlow<OperationState>
    val inProgress: StateFlow<Boolean>

    fun exportDatabase(uri: Uri?)
    fun restoreDatabase(uri: Uri?)
}

@HiltViewModel
class BackupAndRestoreViewModelImpl @Inject constructor(
    private val backupRestoreInteractor: BackupRestoreInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), BackupAndRestoreViewModel {

    private val onExportRequested = MutableSharedFlow<Uri?>(extraBufferCapacity = 1)
    private val onRestoreRequested = MutableSharedFlow<Uri?>(extraBufferCapacity = 1)

    override val backupState = onExportRequested
        .flatMapLatest { uri ->
            flow {
                if (uri == null) {
                    emit(OperationState.Error(R.string.backup_error_could_not_write_to_file))
                    return@flow
                }

                when (backupRestoreInteractor.performManualBackup(uri)) {
                    BackupResult.SUCCESS -> emit(OperationState.Success)

                    BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE ->
                        emit(OperationState.Error(R.string.backup_error_could_not_write_to_file))

                    BackupResult.FAIL_COULD_NOT_FIND_DATABASE ->
                        emit(OperationState.Error(R.string.backup_error_could_not_find_database_file))

                    BackupResult.FAIL_COULD_NOT_COPY ->
                        emit(OperationState.Error(R.string.backup_error_failed_to_copy_database))
                }
            }
        }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationState.Idle)

    override val restoreState = onRestoreRequested
        .flatMapLatest { uri ->
            flow {
                if (uri == null) {
                    emit(OperationState.Error(R.string.restore_error_could_not_read_from_database_file))
                    return@flow
                }

                when (backupRestoreInteractor.performManualRestore(uri)) {
                    RestoreResult.SUCCESS -> emit(OperationState.Success)

                    RestoreResult.FAIL_INVALID_DATABASE ->
                        emit(OperationState.Error(R.string.restore_error_invalid_database_file))

                    RestoreResult.FAIL_COULD_NOT_FIND_OR_READ_DATABASE_FILE ->
                        emit(OperationState.Error(R.string.restore_error_could_not_read_from_database_file))

                    RestoreResult.FAIL_COULD_NOT_COPY ->
                        emit(OperationState.Error(R.string.restore_error_failed_to_copy_database))
                }
            }
        }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationState.Idle)

    override val inProgress = combine(restoreState, backupState) { restore, backup ->
        restore == OperationState.InProgress || backup == OperationState.InProgress
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun exportDatabase(uri: Uri?) {
        viewModelScope.launch { onExportRequested.emit(uri) }
    }

    override fun restoreDatabase(uri: Uri?) {
        viewModelScope.launch { onRestoreRequested.emit(uri) }
    }
}