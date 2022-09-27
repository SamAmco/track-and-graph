package com.samco.trackandgraph.addtracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R

@Composable
@Preview
fun AddTrackerView() {
    Column {
        Text(
            text = stringResource(id = R.string.tracker_name),
        )
    }
}