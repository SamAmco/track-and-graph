package com.samco.trackandgraph.addtracker

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.TrackerHelper
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_3)
fun AddTrackerView() {
    TnGComposeTheme {
        AddTrackerView(viewModel = object : AddTrackerViewModel {
            override val trackerName = MutableLiveData("Tracker name")
            override val trackerDescription = MutableLiveData("Tracker description")
            override val isDuration = MutableLiveData(false)
            override val isLoading = MutableLiveData(false)
            override val hasDefaultValue = MutableLiveData(true)
            override val defaultValue = MutableLiveData(1.0)
            override val defaultLabel = MutableLiveData("")
            override val createButtonEnabled = MutableLiveData(false)
            override val errorText: LiveData<Int?> = MutableLiveData(null)
            override val durationNumericConversionMode =
                MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
            override val isUpdateMode = MutableLiveData(false)

            override fun onTrackerNameChanged(name: String) {}

            override fun onTrackerDescriptionChanged(description: String) {}

            override fun onIsDurationCheckChanged(isDuration: Boolean) {}

            override fun onHasDefaultValueChanged(hasDefaultValue: Boolean) {}

            override fun onDefaultValueChanged(defaultValue: Double) {}

            override fun onDefaultLabelChanged(defaultLabel: String) {}

            override fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode) {}

            override fun onCreateClicked() {}
        })
    }
}

@Composable
fun AddTrackerView(viewModel: AddTrackerViewModel) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.card_padding))
            .width(IntrinsicSize.Max)
            .verticalScroll(state = rememberScrollState())
    ) {
        val trackerName = viewModel.trackerName.observeAsState("")
        val trackerDescription = viewModel.trackerDescription.observeAsState("")

        InputSpacingSmall()

        OutlinedTextField(
            value = trackerName.value,
            label = {
                Text(text = stringResource(id = R.string.tracker_name))
            },
            onValueChange = { viewModel.onTrackerNameChanged(it) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        InputSpacingLarge()
        OutlinedTextField(
            value = trackerDescription.value,
            label = {
                Text(stringResource(id = R.string.add_a_longer_description_optional))
            },
            onValueChange = { viewModel.onTrackerDescriptionChanged(it) },
            modifier = Modifier.fillMaxWidth()
        )

        InputSpacingLarge()

        val isDuration = viewModel.isDuration.observeAsState(false)
        RowCheckbox(
            checked = isDuration.value,
            onCheckedChange = { viewModel.onIsDurationCheckChanged(it) },
            text = stringResource(id = R.string.tracker_type)
        )

        InputSpacingLarge()

        val hasDefaultValue = viewModel.hasDefaultValue.observeAsState(true)
        val defaultValue = viewModel.defaultValue.observeAsState(1.0)
        val defaultLabel = viewModel.defaultLabel.observeAsState("")

        RowCheckbox(
            checked = hasDefaultValue.value,
            onCheckedChange = { viewModel.onHasDefaultValueChanged(it) },
            text = stringResource(id = R.string.use_default_value)
        )

        if (hasDefaultValue.value) {

            if (isDuration.value) {
                LabeledRow(label = stringResource(id = R.string.value_colon)) {
                    DurationInput(
                        seconds = defaultValue.value,
                        onValueChange = { viewModel.onDefaultValueChanged(it) }
                    )
                }
            } else {
                LabeledRow(label = stringResource(id = R.string.value_colon)) {
                    OutlinedTextField(
                        value = defaultValue.value.toString(),
                        onValueChange = {
                            val double = it.toDoubleOrNull() ?: return@OutlinedTextField
                            viewModel.onDefaultValueChanged(double)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            InputSpacingSmall()
            LabeledRow(label = stringResource(id = R.string.label_colon)) {
                OutlinedTextField(
                    value = defaultLabel.value,
                    onValueChange = { viewModel.onDefaultLabelChanged(it) },
                    singleLine = true
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
fun DurationInputPreview() = DurationInput(
    seconds = 3663.0,
    onValueChange = {}
)

@Composable
fun DurationInput(
    seconds: Double,
    onValueChange: (Double) -> Unit
) = Row(
    modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.card_padding)),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.Center
) {
    val hours = (seconds / (60 * 60)).toInt()
    var remainder = seconds - (hours * 60 * 60)
    val minutes = (remainder / 60).toInt()
    remainder -= (minutes * 60)
    val secondsRemainder = remainder.toInt()

    DurationInputComponent(
        value = hours.toString(),
        onValueChange = {
            val int = it.asDurationComponent() ?: return@DurationInputComponent
            onValueChange(
                (int * 60 * 60) + (minutes * 60.0) + secondsRemainder
            )
        }, suffix = stringResource(id = R.string.hours_suffix)
    )
    DurationInputComponent(
        value = minutes.toString(),
        onValueChange = {
            val int = it.asDurationComponent() ?: return@DurationInputComponent
            onValueChange(
                (hours * 60 * 60) + (int * 60.0) + secondsRemainder
            )
        }, suffix = stringResource(id = R.string.minutes_suffix)
    )
    DurationInputComponent(
        value = secondsRemainder.toString(),
        onValueChange = {
            val int = it.asDurationComponent() ?: return@DurationInputComponent
            onValueChange(
                (hours * 60 * 60) + (minutes * 60.0) + int
            )
        }, suffix = stringResource(id = R.string.seconds_suffix)
    )
}

@Composable
private fun DurationInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent
        ),
        singleLine = true,
        modifier = Modifier
            .sizeIn(minWidth = 40.dp, maxWidth = 80.dp)
            .wrapContentWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Text(text = suffix)
}

private fun String.asDurationComponent() = this.filter { it.isDigit() || it == '-' }.toIntOrNull()

@Composable
private fun LabeledRow(
    label: String,
    input: @Composable () -> Unit
) = Row(
    modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.card_padding)),
    verticalAlignment = Alignment.CenterVertically
)
{
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall
    )
    InputSpacingLarge()
    input()
}

@Composable
private fun RowCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    text: String
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .clickable { onCheckedChange?.invoke(!checked) }
        .padding(end = 14.dp)
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
    Text(text = text)
}

@Composable
private fun InputSpacingSmall() =
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.dialog_input_spacing)))

@Composable
private fun InputSpacingLarge() =
    Spacer(
        modifier = Modifier
            .height(dimensionResource(id = R.dimen.input_spacing_large))
            .width(dimensionResource(id = R.dimen.input_spacing_large))
    )

//TODO this should be moved out to a more generic place
@Composable
fun ExpandingTextField(
    modifier: Modifier = Modifier,
    value: String = "",
    hint: String = "",
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = false,
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        label = {
            Text(hint)
        },
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.card_corner_radius))
            )
    )
}