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

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.backupandrestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

interface BackupAndRestoreViewModel {

    val backupError: StateFlow<Int?>
    val backupSuccessful: StateFlow<Boolean>
    val restoreError: StateFlow<Int?>
    val restoreSuccessful: StateFlow<Boolean>
    val autoBackupEnabled: StateFlow<Boolean>
    val inProgress: StateFlow<Boolean>

    fun onBackupSuccessfulConsumed()
    fun onBackupErrorConsumed()
    fun onRestoreErrorConsumed()
    fun exportDatabase(uri: Uri?)
    fun restoreDatabase(uri: Uri?)
    fun disableAutoBackup()
}

@HiltViewModel
class BackupAndRestoreViewModelImpl @Inject constructor(
    private val backupRestoreInteractor: BackupRestoreInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), BackupAndRestoreViewModel {

    private sealed class OperationState {
        object Idle : OperationState()
        object InProgress : OperationState()
        object Success : OperationState()
        data class Error(val stringResource: Int) : OperationState()
    }

    private val onBackupRequest = MutableSharedFlow<Uri?>(extraBufferCapacity = 1)
    private val onRestoreRequested = MutableSharedFlow<Uri?>(extraBufferCapacity = 1)

    private val backupState = merge(
        onRestoreRequested.map { OperationState.Idle },
        onBackupRequest
            .flatMapLatest { uri ->
                flow {
                    emit(OperationState.InProgress)

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
    )
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationState.Idle)

    override val autoBackupEnabled: StateFlow<Boolean> = backupRestoreInteractor
        .autoBackupConfig
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val restoreState = merge(
        onBackupRequest.map { OperationState.Idle },
        onRestoreRequested
            .flatMapLatest { uri ->
                flow {
                    emit(OperationState.InProgress)

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
    )
        .stateIn(viewModelScope, SharingStarted.Eagerly, OperationState.Idle)

    private val onBackupSuccessfulConsumed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val onBackupErrorConsumed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val onRestoreErrorConsumed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val backupError: StateFlow<Int?> =
        merge(
            onBackupErrorConsumed.map { null },
            backupState.mapNotNull { (it as? OperationState.Error)?.stringResource },
        ).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val backupSuccessful: StateFlow<Boolean> = merge(
        onBackupSuccessfulConsumed.map { false },
        backupState.mapNotNull { (it is OperationState.Success) },
    ).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val restoreError: StateFlow<Int?> =
        merge(
            onRestoreErrorConsumed.map { null },
            restoreState.mapNotNull { (it as? OperationState.Error)?.stringResource },
        ).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val restoreSuccessful: StateFlow<Boolean> = merge(
        restoreState.mapNotNull { (it is OperationState.Success) },
    ).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val inProgress = combine(restoreState, backupState) { restore, backup ->
        restore == OperationState.InProgress || backup == OperationState.InProgress
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onBackupSuccessfulConsumed() {
        viewModelScope.launch { onBackupSuccessfulConsumed.emit(Unit) }
    }

    override fun onBackupErrorConsumed() {
        viewModelScope.launch { onBackupErrorConsumed.emit(Unit) }
    }

    override fun onRestoreErrorConsumed() {
        viewModelScope.launch { onRestoreErrorConsumed.emit(Unit) }
    }

    override fun exportDatabase(uri: Uri?) {
        viewModelScope.launch { onBackupRequest.emit(uri) }
    }

    override fun restoreDatabase(uri: Uri?) {
        viewModelScope.launch { onRestoreRequested.emit(uri) }
    }

    override fun disableAutoBackup() = backupRestoreInteractor.disableAutoBackup()
}