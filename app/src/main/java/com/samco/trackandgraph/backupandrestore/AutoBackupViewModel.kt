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

package com.samco.trackandgraph.backupandrestore

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject

interface AutoBackupViewModel {
    val autoBackupLocation: StateFlow<String?>
    val autoBackupFirstDate: StateFlow<OffsetDateTime>
    val autoBackupIntervalTextFieldValue: State<TextFieldValue>
    val autoBackupUnit: StateFlow<ChronoUnit>
    val autoBackupConfigValid: StateFlow<Boolean>

    fun onConfirmAutoBackup()
    fun onBackupLocationChanged(uri: Uri?)
    fun onBackupIntervalChanged(text: TextFieldValue)
    fun onBackupUnitChanged(unit: ChronoUnit)
    fun onAutoBackupFirstDateChanged(offsetDateTime: OffsetDateTime)
    fun onAutoBackupFirstDateChanged(selectedTime: SelectedTime)
    fun onCancelConfig()
}

@HiltViewModel
class AutoBackupViewModelImpl @Inject constructor(
    private val interactor: BackupRestoreInteractor
) : ViewModel(), AutoBackupViewModel {

    private val onUserSetUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)

    private val onCancelEdit = MutableSharedFlow<Unit>()

    private val storedBackupConfig = merge(
        interactor.autoBackupConfig.distinctUntilChanged(),
        onCancelEdit.map { interactor.getAutoBackupInfo() }
    ).shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val uri: StateFlow<Uri?> = merge(
        onUserSetUri,
        storedBackupConfig.map { it?.uri }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val autoBackupLocation: StateFlow<String?> = uri
        .map { it?.toString() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val onUserSetAutoBackupDate = MutableSharedFlow<OffsetDateTime>(extraBufferCapacity = 1)

    override val autoBackupFirstDate = merge(
        onUserSetAutoBackupDate,
        storedBackupConfig.mapNotNull { it?.nextScheduled }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, OffsetDateTime.now().plusHours(1))

    override var autoBackupIntervalTextFieldValue: MutableState<TextFieldValue> =
        mutableStateOf(TextFieldValue("1", TextRange(0, 1)))

    private val onUserSetAutoBackupUnit = MutableSharedFlow<ChronoUnit>(extraBufferCapacity = 1)

    override val autoBackupUnit = merge(
        onUserSetAutoBackupUnit,
        storedBackupConfig.mapNotNull { it?.units }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, ChronoUnit.DAYS)

    init {
        //Shame to use a side effect for this but we can't separate TextFieldValue into a flow
        // it causes havoc if it is not modified synchronously in the UI
        viewModelScope.launch {
            storedBackupConfig.filterNotNull().collect {
                autoBackupIntervalTextFieldValue.value = TextFieldValue(
                    it.interval.toString(),
                    TextRange(0, it.interval.toString().length)
                )
            }
        }
    }

    private val currentBackupConfig: StateFlow<BackupConfig?> = combine(
        uri,
        autoBackupFirstDate,
        snapshotFlow { autoBackupIntervalTextFieldValue.value.text },
        autoBackupUnit
    ) { uri, firstDate, interval, unit ->
        val intervalInt = interval.toIntOrNull() ?: return@combine null
        val uriNonNull = uri ?: return@combine null
        val config = BackupConfig(uriNonNull, firstDate, intervalInt, unit)
        if (interactor.validAutoBackupConfiguration(config)) config else null
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BackupConfig(Uri.EMPTY, OffsetDateTime.now(), 1, ChronoUnit.DAYS)
    )

    override val autoBackupConfigValid: StateFlow<Boolean> = currentBackupConfig
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onConfirmAutoBackup() {
        currentBackupConfig.value?.let {
            viewModelScope.launch { interactor.backupNowAndSetAutoBackupConfig(it) }
        }
    }

    override fun onBackupLocationChanged(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch { onUserSetUri.emit(uri) }
    }

    private fun moveUntilValid(odt: OffsetDateTime): OffsetDateTime {
        var new = odt
        while (new.isBefore(OffsetDateTime.now())) {
            new = new.plus(1, autoBackupUnit.value)
        }
        return new
    }

    override fun onBackupIntervalChanged(text: TextFieldValue) {
        val filtered = text.copy(text = text.text.filter { it.isDigit() })
        if (autoBackupIntervalTextFieldValue.value != filtered)
            autoBackupIntervalTextFieldValue.value = filtered
    }

    override fun onBackupUnitChanged(unit: ChronoUnit) {
        viewModelScope.launch { onUserSetAutoBackupUnit.emit(unit) }
    }

    override fun onAutoBackupFirstDateChanged(offsetDateTime: OffsetDateTime) {
        val current = autoBackupFirstDate.value
        val new = moveUntilValid(
            offsetDateTime
                .withHour(current.hour)
                .withMinute(current.minute)
                .withSecond(current.second)
                .withNano(current.nano)
        )
        viewModelScope.launch { onUserSetAutoBackupDate.emit(new) }
    }

    override fun onAutoBackupFirstDateChanged(selectedTime: SelectedTime) {
        val current = autoBackupFirstDate.value
        val new = moveUntilValid(
            current
                .withHour(selectedTime.hour)
                .withMinute(selectedTime.minute)
        )
        viewModelScope.launch { onUserSetAutoBackupDate.emit(new) }
    }

    override fun onCancelConfig() {
        viewModelScope.launch { onCancelEdit.emit(Unit) }
    }
}
