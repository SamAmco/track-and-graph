package com.samco.trackandgraph.featurehistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.base.helpers.formatDayWeekDayMonthYearHourMinuteOneLine
import com.samco.trackandgraph.base.helpers.getDisplayValue
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_3)
fun FeatureHistoryViewPreview() {
    TnGComposeTheme {
        FeatureHistoryView(viewModel = object : FeatureHistoryViewModel {
            override val isDuration = MutableLiveData(false)
            override val isTracker = MutableLiveData(false)
            override val dataPoints = MutableLiveData(emptyList<DataPoint>())
            override val showFeatureInfo = MutableLiveData<Feature?>(null)
            override val showDataPointInfo = MutableLiveData<DataPoint?>(null)

            override fun deleteDataPoint() {}
            override fun onEditClicked(dataPoint: DataPoint) {}
            override fun onDeleteClicked(dataPoint: DataPoint) {}
            override fun onDataPointClicked(dataPoint: DataPoint) {}
            override fun onDismissDataPoint() {}

            override fun onShowFeatureInfo() {}
            override fun onHideFeatureInfo() {}
        })
    }
}

@Composable
fun FeatureHistoryView(viewModel: FeatureHistoryViewModel) {
    val dataPoints = viewModel.dataPoints.observeAsState(emptyList())
    val weekdayNames = getWeekDayNames(LocalContext.current)
    val isDuration by viewModel.isDuration.observeAsState(false)
    val isTracker by viewModel.isTracker.observeAsState(false)
    val featureInfo by viewModel.showFeatureInfo.observeAsState()
    val dataPointInfo by viewModel.showDataPointInfo.observeAsState()

    LazyColumn(modifier = Modifier.padding(dimensionResource(id = R.dimen.card_margin_small))) {
        items(dataPoints.value) {
            DataPoint(
                dataPoint = it,
                viewModel = viewModel,
                weekdayNames = weekdayNames,
                isDuration = isDuration,
                isTracker = isTracker
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    featureInfo?.let {
        FeatureInfoDialog(
            feature = it,
            onDismissRequest = viewModel::onHideFeatureInfo
        )
    }

    dataPointInfo?.let {
        DataPointInfoDialog(
            dataPoint = it,
            weekdayNames = weekdayNames,
            isDuration = isDuration,
            onDismissRequest = viewModel::onDismissDataPoint
        )
    }
}

@Composable
fun DataPointInfoDialog(
    dataPoint: DataPoint,
    isDuration: Boolean,
    weekdayNames: List<String>,
    onDismissRequest: () -> Unit
) = InfoDialog(onDismissRequest) {
    Text(
        formatDayWeekDayMonthYearHourMinuteOneLine(
            LocalContext.current,
            weekdayNames,
            dataPoint.timestamp
        ),
        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
        fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
    )
    SpacingSmall()
    Text(dataPoint.getDisplayValue(isDuration))
    Text(dataPoint.note)
}

@Composable
fun FeatureInfoDialog(
    feature: Feature,
    onDismissRequest: () -> Unit
) = InfoDialog(onDismissRequest) {
    Text(
        feature.name,
        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
        fontWeight = MaterialTheme.typography.headlineSmall.fontWeight
    )
    SpacingSmall()
    Text(feature.description)
}

@Composable
fun InfoDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) = Dialog(
    onDismissRequest = onDismissRequest
) {
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(dimensionResource(id = R.dimen.card_padding)),
            content = content
        )
    }
}

@Composable
private fun DataPoint(
    dataPoint: DataPoint,
    viewModel: FeatureHistoryViewModel,
    weekdayNames: List<String>,
    isDuration: Boolean,
    isTracker: Boolean
) = ElevatedCard(
    modifier = Modifier.clickable { viewModel.onDataPointClicked(dataPoint) }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.card_margin_small))
    ) {
        Text(
            text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                LocalContext.current,
                weekdayNames,
                dataPoint.timestamp
            ),
            textAlign = TextAlign.Center
        )
        SpacingSmall()
        DataPointValueAndDescription(
            modifier = Modifier.weight(1f),
            dataPoint = dataPoint,
            isDuration = isDuration
        )
        if (isTracker) {
            IconButton(onClick = { viewModel.onEditClicked(dataPoint) }) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = stringResource(id = R.string.edit_data_point_button_content_description),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { viewModel.onDeleteClicked(dataPoint) }) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_data_point_button_content_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DataPointValueAndDescription(
    modifier: Modifier,
    dataPoint: DataPoint,
    isDuration: Boolean
) = Column(modifier = modifier) {
    Text(
        text = dataPoint.getDisplayValue(isDuration),
        fontSize = MaterialTheme.typography.labelLarge.fontSize,
        fontWeight = MaterialTheme.typography.labelLarge.fontWeight,
    )
    if (dataPoint.note.isNotEmpty()) {
        SpacingSmall()
        Text(
            text = dataPoint.note,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}