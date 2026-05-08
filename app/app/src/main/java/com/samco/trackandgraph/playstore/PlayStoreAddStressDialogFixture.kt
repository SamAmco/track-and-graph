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
package com.samco.trackandgraph.playstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samco.trackandgraph.adddatapoint.AddDataPointTutorialViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModel
import com.samco.trackandgraph.adddatapoint.FieldLockState
import com.samco.trackandgraph.adddatapoint.SuggestedValueViewData
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.group.GroupChild
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.threeten.bp.OffsetDateTime

@Composable
internal fun PlayStoreAddStressDialogOverlay() {
    val viewModel = remember { PlayStoreAddStressDialogViewModel() }
    AddDataPointsDialog(
        viewModel = viewModel,
        dialogWidth = 330.dp,
        scrollInputContent = false,
    )
}

internal class PlayStoreAddStressDialogViewModel : AddDataPointsViewModel {
    override val hidden: LiveData<Boolean> = MutableLiveData(false)
    override val showTutorial: LiveData<Boolean> = MutableLiveData(false)
    override val updateMode: LiveData<Boolean> = MutableLiveData(false)
    override val indexText: LiveData<String> = MutableLiveData("9 / 9")
    override val skipButtonVisible: LiveData<Boolean> = MutableLiveData(true)
    override val currentPageIndex: LiveData<Int> = MutableLiveData(8)
    override val tutorialViewModel: AddDataPointTutorialViewModel = PlayStoreAddDataPointTutorialViewModel()
    override val showCancelConfirmDialog: LiveData<Boolean> = MutableLiveData(false)
    override val dismissEvents: Flow<Unit> = emptyFlow()
    override val pageViewModels: StateFlow<List<AddDataPointViewModel>> = MutableStateFlow(
        playStoreDailyChildren()
            .filterIsInstance<GroupChild.ChildTracker>()
            .map { child ->
                PlayStoreAddDataPointViewModel(
                    name = child.displayTracker.name,
                    tracker = child.displayTracker.toTracker(),
                    suggestedValues = if (child.displayTracker.name == "Stress") playStoreStressSuggestions()
                    else emptyList(),
                )
            }
    )
    override val trackedCount: StateFlow<Int> = MutableStateFlow(0)
    override val dataPointAddedEvent: Flow<Unit> = emptyFlow()

    override fun onTutorialButtonPressed() {}
    override fun onCancelClicked() {}
    override fun onConfirmCancelConfirmed() {}
    override fun onConfirmCancelDismissed() {}
    override fun onSkipClicked() {}
    override fun onAddClicked() {}
    override fun updateCurrentPage(page: Int) {}
}

internal class PlayStoreAddDataPointViewModel(
    name: String,
    internal val tracker: Tracker,
    suggestedValues: List<SuggestedValueViewData>,
) : AddDataPointViewModel.NumericalDataPointViewModel {
    override val name: LiveData<String> = MutableLiveData(name)
    override val timestamp: LiveData<OffsetDateTime> = MutableLiveData(PREVIEW_END_TIME)
    override var label: TextFieldValue = TextFieldValue()
    override var note: TextFieldValue = TextFieldValue()
    override val suggestedValues: LiveData<List<SuggestedValueViewData>?> = MutableLiveData(suggestedValues)
    override val currentValueAsSuggestion: LiveData<SuggestedValueViewData?> = MutableLiveData(null)
    override val lockState: FieldLockState = FieldLockState()
    override val focusOnValueEvent: Flow<Unit> = emptyFlow()
    override val oldDataPoint: DataPoint? = null
    override var value: TextFieldValue = TextFieldValue()

    override fun getTracker(): Tracker = tracker
    override fun updateLabel(label: TextFieldValue) {
        this.label = label
    }
    override fun updateNote(note: TextFieldValue) {
        this.note = note
    }
    override fun updateTimestamp(timestamp: OffsetDateTime) {}
    override fun addDataPoint() {}
    override fun onSuggestedValueSelected(suggestedValue: SuggestedValueViewData) {}
    override fun onSuggestedValueLongPress(suggestedValue: SuggestedValueViewData) {}
    override fun toggleValueLock() {}
    override fun toggleLabelLock() {}
    override fun toggleNoteLock() {}
    override fun setValueText(value: TextFieldValue) {
        this.value = value
    }
}

internal class PlayStoreAddDataPointTutorialViewModel : AddDataPointTutorialViewModel {
    override val currentPage: LiveData<Int> = MutableLiveData(0)
    override fun onButtonClicked() {}
    override fun onSwipeToPage(page: Int) {}
    override fun onNavigateToFaqClicked() {}
    override fun reset() {}
}

internal fun DisplayTracker.toTracker() = Tracker(
    id = id,
    name = name,
    featureId = featureId,
    description = description,
    dataType = dataType,
    hasDefaultValue = hasDefaultValue,
    defaultValue = defaultValue,
    defaultLabel = defaultLabel,
)

internal fun playStoreStressSuggestions() = listOf(
    SuggestedValueViewData(value = 0.0, valueStr = "0", label = "None"),
    SuggestedValueViewData(value = 1.0, valueStr = "1", label = "Low"),
    SuggestedValueViewData(value = 2.0, valueStr = "2", label = "Medium"),
    SuggestedValueViewData(value = 3.0, valueStr = "3", label = "High"),
)
