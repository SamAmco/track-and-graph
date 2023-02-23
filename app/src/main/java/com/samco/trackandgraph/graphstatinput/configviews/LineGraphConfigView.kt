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

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatDurationSpinner
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
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

    GraphStatEndingAtSpinner(
        modifier,
        viewModel.sampleEndingAt
    ) { viewModel.setSampleEnding(it) }
}

