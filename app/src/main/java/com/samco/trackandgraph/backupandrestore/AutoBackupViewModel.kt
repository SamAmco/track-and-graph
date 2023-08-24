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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject

interface AutoBackupViewModel {
    val autoBackupFirstDate: StateFlow<OffsetDateTime>
    val autoBackupIntervalTextFieldValue: State<TextFieldValue>
    val autoBackupUnit: StateFlow<ChronoUnit>
    val autoBackupConfigValid: StateFlow<Boolean>

    fun onConfirmAutoBackup()
    fun onBackupIntervalChanged(text: TextFieldValue)
    fun onBackupUnitChanged(unit: ChronoUnit)
    fun onAutoBackupFirstDateChanged(offsetDateTime: OffsetDateTime)
    fun onAutoBackupFirstDateChanged(selectedTime: SelectedTime)
}

@HiltViewModel
class AutoBackupViewModelImpl @Inject constructor() : ViewModel(), AutoBackupViewModel {

    override val autoBackupFirstDate = MutableStateFlow(OffsetDateTime.now().plusHours(1))

    override var autoBackupIntervalTextFieldValue: MutableState<TextFieldValue> = mutableStateOf(
        TextFieldValue(
            "1", TextRange(0, 1)
        )
    )

    override val autoBackupUnit = MutableStateFlow(ChronoUnit.DAYS)

    override val autoBackupConfigValid: StateFlow<Boolean> = combine(
        autoBackupFirstDate.map { it.isAfter(OffsetDateTime.now()) },
        snapshotFlow { autoBackupIntervalTextFieldValue.value }
            .map { tfv -> tfv.text.toIntOrNull()?.let { it > 0 } == true },
        autoBackupUnit.map {
            setOf(ChronoUnit.HOURS, ChronoUnit.DAYS, ChronoUnit.WEEKS).contains(it)
        }
    ) { a, b, c -> a && b && c }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    override fun onConfirmAutoBackup() {
        if (!autoBackupConfigValid.value) return

        //TODO
    }

    override fun onBackupIntervalChanged(text: TextFieldValue) {
        val filtered = text.copy(text = text.text.filter { it.isDigit() })
        if (autoBackupIntervalTextFieldValue.value != filtered)
            autoBackupIntervalTextFieldValue.value = filtered
    }

    override fun onBackupUnitChanged(unit: ChronoUnit) {
        autoBackupUnit.value = unit
    }

    override fun onAutoBackupFirstDateChanged(offsetDateTime: OffsetDateTime) {
        val current = autoBackupFirstDate.value
        autoBackupFirstDate.value = offsetDateTime
            .withHour(current.hour)
            .withMinute(current.minute)
            .withSecond(current.second)
            .withNano(current.nano)
    }

    override fun onAutoBackupFirstDateChanged(selectedTime: SelectedTime) {
        val current = autoBackupFirstDate.value
        autoBackupFirstDate.value = current
            .withHour(selectedTime.hour)
            .withMinute(selectedTime.minute)
    }
}
