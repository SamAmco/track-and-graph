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
package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import com.samco.trackandgraph.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.showDateDialog
import org.threeten.bp.OffsetDateTime

enum class SampleEndingAtOption {
    LATEST, CUSTOM
}

sealed interface SampleEndingAt {
    val option: SampleEndingAtOption

    object Latest : SampleEndingAt {
        override val option = SampleEndingAtOption.LATEST
    }

    data class Custom(val dateTime: OffsetDateTime?) : SampleEndingAt {
        override val option = SampleEndingAtOption.CUSTOM
    }
}

@Composable
fun LineGraphConfigView(
    modifier: Modifier = Modifier,
    viewModel: LineGraphConfigViewModel
) {
    GraphStatDurationSpinner(
        modifier = modifier,
        selectedDuration = viewModel.selectedDuration,
        onDurationSelected = { viewModel.setDuration(it) }
    )

    //TODO extract this into a composable
    LabeledRow(label = stringResource(id = R.string.ending_at_colon)) {
        val strings = stringArrayResource(id = R.array.ending_at_values)

        val spinnerItems = mapOf(
            SampleEndingAtOption.LATEST to strings[0],
            SampleEndingAtOption.CUSTOM to strings[1]
        )

        val context = LocalContext.current
        //TODO the spinner item should say the date not custom
        TextMapSpinner(
            modifier = Modifier.padding(0.dp),
            strings = spinnerItems,
            selectedItem = viewModel.sampleEndingAt.option,
            onItemSelected = { option ->
                when (option) {
                    SampleEndingAtOption.LATEST -> viewModel.setSampleEnding(SampleEndingAt.Latest)
                    SampleEndingAtOption.CUSTOM -> showDateDialog(context) {
                        viewModel.setSampleEnding(SampleEndingAt.Custom(it))
                    }
                }
            }
        )
    }
}
