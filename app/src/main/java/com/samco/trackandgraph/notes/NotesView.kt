package com.samco.trackandgraph.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl

@Composable
fun NotesView(
    addDataPointsDialogViewModel : AddDataPointsViewModelImpl,
    globalNoteDialogViewModel: GlobalNoteInputViewModel
) {
    AddDataPointsDialog(
        viewModel = addDataPointsDialogViewModel,
        onDismissRequest = { addDataPointsDialogViewModel.reset() }
    )
    if (globalNoteDialogViewModel.show.observeAsState(false).value) {
        GlobalNoteInputDialogView(viewModel = globalNoteDialogViewModel)
    }
}
