/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.decorators

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.helpers.formatDayWeekDayMonthYearHourMinuteOneLine
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueData
import com.samco.trackandgraph.ui.compose.ui.DataPointValueAndDescription
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatLastValueDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ILastValueData>(listMode) {
    private lateinit var binding: GraphStatViewBinding
    private lateinit var context: Context
    private var isDuration: Boolean = false
    private lateinit var dataPoint: DataPoint

    override fun decorate(view: IDecoratableGraphStatView, data: ILastValueData) {
        binding = view.getBinding()
        context = view.getContext()

        dataPoint = data.lastDataPoint
            ?: throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_stat)

        isDuration = data.isDuration

        binding.composeView.setContent { constructView() }
        binding.composeView.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun setTimeMarker(time: OffsetDateTime) {}

    override fun update() {
        super.update()

        binding.composeView.setContent { constructView() }
    }

    @Composable
    private fun constructView() = Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!::dataPoint.isInitialized) {
            //Don't think this should ever happen, but just in case
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_stat)
        }

        val context = LocalContext.current

        val weekdayNames = getWeekDayNames(context)

        Text(
            text = formatDayWeekDayMonthYearHourMinuteOneLine(
                context,
                weekdayNames,
                dataPoint.timestamp
            ),
            style = MaterialTheme.typography.body1
        )

        SpacingSmall()

        val duration = Duration.between(dataPoint.timestamp, OffsetDateTime.now())

        Text(
            text = formatTimeToDaysHoursMinutesSeconds(context, duration.toMillis(), false),
            style = MaterialTheme.typography.body1
        )

        SpacingSmall()

        DataPointValueAndDescription(dataPoint = dataPoint, isDuration = isDuration)
    }
}