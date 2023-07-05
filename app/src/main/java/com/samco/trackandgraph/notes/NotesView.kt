package com.samco.trackandgraph.notes

import androidx.compose.runtime.Composable
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
    GlobalNoteInputDialogView(viewModel = globalNoteDialogViewModel)
}
