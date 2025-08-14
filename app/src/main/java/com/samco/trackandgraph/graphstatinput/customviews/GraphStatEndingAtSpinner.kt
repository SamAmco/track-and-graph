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
package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphEndDate
import com.samco.trackandgraph.helpers.formatDayMonthYear
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.ui.LabeledRow
import com.samco.trackandgraph.ui.compose.ui.Spinner
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.showDatePickerDialog
import org.threeten.bp.OffsetDateTime

enum class SampleEndingAtOption {
    NOW, LATEST, CUSTOM
}

sealed interface SampleEndingAt {
    val option: SampleEndingAtOption

    fun asGraphEndDate(): GraphEndDate

    companion object {
        fun fromGraphEndDate(endDate: GraphEndDate): SampleEndingAt {
            return when (endDate) {
                is GraphEndDate.Latest -> Latest
                is GraphEndDate.Now -> Now
                is GraphEndDate.Date -> Custom(endDate.date)
            }
        }
    }

    object Latest : SampleEndingAt {
        override val option = SampleEndingAtOption.LATEST
        override fun asGraphEndDate() = GraphEndDate.Latest
    }

    object Now : SampleEndingAt {
        override val option = SampleEndingAtOption.NOW
        override fun asGraphEndDate() = GraphEndDate.Now
    }

    data class Custom(val dateTime: OffsetDateTime) : SampleEndingAt {
        override val option = SampleEndingAtOption.CUSTOM
        override fun asGraphEndDate() = GraphEndDate.Date(dateTime)
    }
}

@Composable
fun GraphStatEndingAtSpinner(
    modifier: Modifier,
    sampleEndingAt: SampleEndingAt,
    onSampleEndingAtChanged: (SampleEndingAt) -> Unit
) {

    val strLatest = stringResource(id = R.string.ending_at_latest)
    val strCustom = stringResource(id = R.string.ending_at_custom_date)
    val strNow = stringResource(id = R.string.ending_at_now)

    LabeledRow(
        label = stringResource(id = R.string.ending_at_colon),
        paddingValues = PaddingValues(start = cardPadding)
    ) {
        val spinnerItems = mapOf(
            SampleEndingAtOption.LATEST to strLatest,
            SampleEndingAtOption.NOW to strNow,
            SampleEndingAtOption.CUSTOM to strCustom
        )

        val context = LocalContext.current
        val firstDayOfWeek = LocalSettings.current.firstDayOfWeek

        Spinner(
            modifier = modifier,
            items = spinnerItems.keys.toList(),
            selectedItem = sampleEndingAt.option,
            onItemSelected = { option ->
                when (option) {
                    SampleEndingAtOption.LATEST -> onSampleEndingAtChanged(SampleEndingAt.Latest)
                    SampleEndingAtOption.NOW -> onSampleEndingAtChanged(SampleEndingAt.Now)
                    SampleEndingAtOption.CUSTOM -> showDatePickerDialog(
                        context = context,
                        firstDayOfWeek = firstDayOfWeek,
                        onDateSelected = {
                            onSampleEndingAtChanged(SampleEndingAt.Custom(it))
                        }
                    )
                }
            },
            selectedItemFactory = { modifier, item, expanded ->
                val text = when (item) {
                    SampleEndingAtOption.LATEST -> strLatest
                    SampleEndingAtOption.NOW -> strNow
                    SampleEndingAtOption.CUSTOM -> {
                        if (sampleEndingAt is SampleEndingAt.Custom)
                            formatDayMonthYear(context, sampleEndingAt.dateTime)
                        else strCustom
                    }
                }

                Text(
                    modifier = modifier.weight(1f),
                    text = text,
                    textAlign = TextAlign.End,
                )
            },
            dropdownItemFactory = { item, _ ->
                Text(
                    text = spinnerItems[item] ?: "",
                    textAlign = TextAlign.End,
                )
            },
            dropdownContentAlignment = Alignment.End,
        )
    }
}