package com.samco.trackandgraph.addtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.TrackerHelper
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.disabledAlpha
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModelImpl

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
            override val defaultValue = MutableLiveData("1.0")
            override val defaultLabel = MutableLiveData("")
            override val createButtonEnabled = MutableLiveData(false)
            override val errorText: LiveData<Int?> = MutableLiveData(null)
            override val durationNumericConversionMode =
                MutableLiveData(TrackerHelper.DurationNumericConversionMode.HOURS)
            override val shouldShowDurationConversionModeSpinner = MutableLiveData(true)
            override val isUpdateMode = MutableLiveData(true)
            override val hours = MutableLiveData("")
            override val minutes = MutableLiveData("")
            override val seconds = MutableLiveData("")

            override fun onTrackerNameChanged(name: String) {}

            override fun onTrackerDescriptionChanged(description: String) {}

            override fun onIsDurationCheckChanged(isDuration: Boolean) {}

            override fun onHasDefaultValueChanged(hasDefaultValue: Boolean) {}

            override fun onDefaultValueChanged(defaultValue: String) {}

            override fun onDefaultLabelChanged(defaultLabel: String) {}

            override fun onDurationNumericConversionModeChanged(durationNumericConversionMode: TrackerHelper.DurationNumericConversionMode) {}

            override fun onCreateClicked() {}

            override fun setHours(value: String) {}

            override fun setMinutes(value: String) {}

            override fun setSeconds(value: String) {}

            override fun getDurationAsDouble() = 0.0
        })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddTrackerView(viewModel: AddTrackerViewModel) {

    val focusRequester = FocusRequester()
    val isUpdateMode by viewModel.isUpdateMode.observeAsState(false)

    Column(Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_padding))
                .fillMaxWidth()
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(state = rememberScrollState())
        ) {
            val trackerName = viewModel.trackerName.observeAsState("")
            val trackerDescription = viewModel.trackerDescription.observeAsState("")
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            val isDuration = viewModel.isDuration.observeAsState(false)
            val hasDefaultValue = viewModel.hasDefaultValue.observeAsState(true)
            val defaultValue = viewModel.defaultValue.observeAsState("")
            val defaultLabel = viewModel.defaultLabel.observeAsState("")

            InputSpacingSmall()

            NameInput(
                trackerName.value,
                viewModel,
                focusManager,
                focusRequester,
                keyboardController
            )

            InputSpacingLarge()

            DescriptionInput(trackerDescription.value, focusManager, viewModel)

            InputSpacingLarge()

            DurationCheckbox(isDuration.value, viewModel)

            val shouldShowConversionSpinner =
                viewModel.shouldShowDurationConversionModeSpinner.observeAsState(true)
            val durationConversionMode = viewModel.durationNumericConversionMode.observeAsState()

            if (shouldShowConversionSpinner.value) {
                InputSpacingSmall()
                DurationConversionModeInput(
                    isDuration.value,
                    durationConversionMode.value,
                    viewModel
                )
            }

            InputSpacingLarge()

            DefaultValueCheckbox(hasDefaultValue.value, viewModel)

            if (hasDefaultValue.value) {

                if (isDuration.value) DurationInputRow(viewModel)
                else ValueInputRow(defaultValue.value, viewModel, focusManager)

                InputSpacingSmall()

                LabelInputRow(defaultLabel.value, viewModel)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val errorText by viewModel.errorText.observeAsState()

                Text(
                    text = errorText?.let { stringResource(id = it) } ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .fillMaxWidth()
                )

                Button(
                    onClick = viewModel::onCreateClicked,
                    shape = MaterialTheme.shapes.small,
                    enabled = errorText == null,
                    modifier = Modifier.padding(end = dimensionResource(id = R.dimen.card_margin_small))
                ) {
                    val buttonText =
                        if (isUpdateMode) stringResource(id = R.string.update)
                        else stringResource(id = R.string.create)
                    Text(buttonText)
                }
            }
        }
    }

    LaunchedEffect(true) { focusRequester.requestFocus() }
}

@Composable
private fun DurationConversionModeInput(
    isDuration: Boolean,
    durationConversionMode: TrackerHelper.DurationNumericConversionMode?,
    viewModel: AddTrackerViewModel
) {
    val strings = mapOf(
        TrackerHelper.DurationNumericConversionMode.HOURS to stringResource(id = R.string.hours),
        TrackerHelper.DurationNumericConversionMode.MINUTES to stringResource(id = R.string.minutes),
        TrackerHelper.DurationNumericConversionMode.SECONDS to stringResource(id = R.string.seconds)
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        val name =
            if (isDuration) stringResource(id = R.string.numeric_to_duration_mode_header)
            else stringResource(id = R.string.duration_to_numeric_mode_header)
        Text(
            text = name,
            modifier = Modifier
                .weight(1f)
                .padding(end = dimensionResource(id = R.dimen.card_padding))
        )
        TextMapSpinner(
            strings = strings,
            selectedItem = durationConversionMode
                ?: TrackerHelper.DurationNumericConversionMode.HOURS,
            onItemSelected = viewModel::onDurationNumericConversionModeChanged
        )
    }
}

@Composable
fun <T> TextMapSpinner(
    strings: Map<T, String>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    Spinner(
        items = strings.keys.toList(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        selectedItemFactory = { modifier, item, expanded ->
            Row(
                modifier = modifier
                    .padding(dimensionResource(id = R.dimen.card_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings[item] ?: "",
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        },
        dropdownItemFactory = { item, _ ->
            Text(
                text = strings[item] ?: "",
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                fontWeight = MaterialTheme.typography.labelSmall.fontWeight
            )
        }
    )
}

@Composable
fun <T> Spinner(
    modifier: Modifier = Modifier,
    dropDownModifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    selectedItemFactory: @Composable (Modifier, T, Boolean) -> Unit,
    dropdownItemFactory: @Composable (T, Int) -> Unit,
) {
    var expanded: Boolean by remember { mutableStateOf(false) }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        selectedItemFactory(
            Modifier.clickable { expanded = true },
            selectedItem,
            expanded
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = dropDownModifier
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            items.forEachIndexed { index, element ->
                DropdownMenuItem(
                    text = {
                        dropdownItemFactory(element, index)
                    },
                    onClick = {
                        onItemSelected(items[index])
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LabelInputRow(
    defaultLabel: String,
    viewModel: AddTrackerViewModel
) {
    LabeledRow(label = stringResource(id = R.string.label_colon)) {
        OutlinedTextField(
            value = defaultLabel,
            onValueChange = { viewModel.onDefaultLabelChanged(it) },
            singleLine = true
        )
    }
}

@Composable
private fun ValueInputRow(
    defaultValue: String,
    viewModel: AddTrackerViewModel,
    focusManager: FocusManager
) {
    val textField = remember { mutableStateOf(TextFieldValue(defaultValue)) }

    if (textField.value.text != defaultValue) {
        textField.value = textField.value.copy(text = defaultValue)
    }

    LabeledRow(label = stringResource(id = R.string.value_colon)) {
        OutlinedTextField(
            value = textField.value,
            onValueChange = {
                textField.value = it
                viewModel.onDefaultValueChanged(it.text)
            },
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .onFocusChanged { focusState ->
                    val textLength = textField.value.text.length
                    if (focusState.isFocused) {
                        textField.value = textField.value.copy(
                            selection = TextRange(0, textLength)
                        )
                    } else {
                        textField.value = textField.value.copy(
                            selection = TextRange(textLength, textLength)
                        )
                    }
                },
        )
    }
}

@Composable
private fun DurationInputRow(viewModel: AddTrackerViewModel) {
    LabeledRow(label = stringResource(id = R.string.value_colon)) {
        DurationInput(viewModel)
    }
}

@Composable
private fun DefaultValueCheckbox(
    hasDefaultValue: Boolean,
    viewModel: AddTrackerViewModel
) {
    RowCheckbox(
        checked = hasDefaultValue,
        onCheckedChange = { viewModel.onHasDefaultValueChanged(it) },
        text = stringResource(id = R.string.use_default_value)
    )
}

@Composable
private fun DurationCheckbox(
    isDuration: Boolean,
    viewModel: AddTrackerViewModel
) {
    RowCheckbox(
        checked = isDuration,
        onCheckedChange = { viewModel.onIsDurationCheckChanged(it) },
        text = stringResource(id = R.string.tracker_type)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DescriptionInput(
    trackerDescription: String,
    focusManager: FocusManager,
    viewModel: AddTrackerViewModel
) = FullWidthTextField(
    value = trackerDescription,
    onValueChange = viewModel::onTrackerDescriptionChanged,
    label = stringResource(id = R.string.add_a_longer_description_optional),
    focusManager = focusManager
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FullWidthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    focusManager: FocusManager? = null,
    focusRequester: FocusRequester? = null,
    keyboardController: SoftwareKeyboardController? = null
) {
    val textField = remember {
        mutableStateOf(
            TextFieldValue(
                value,
                TextRange(value.length, value.length)
            )
        )
    }

    if (textField.value.text != value) {
        textField.value = TextFieldValue(
            value,
            TextRange(value.length, value.length)
        )
    }

    OutlinedTextField(
        value = textField.value,
        label = { Text(text = label) },
        onValueChange = {
            textField.value = it
            onValueChange(it.text)
        },
        keyboardActions = KeyboardActions(
            onNext = { focusManager?.moveFocus(FocusDirection.Down) }
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.hasFocus) keyboardController?.show()
            }
            .let {
                if (focusRequester != null) it.focusRequester(focusRequester)
                else it
            }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NameInput(
    trackerName: String,
    viewModel: AddTrackerViewModel,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?
) = FullWidthTextField(
    value = trackerName,
    onValueChange = viewModel::onTrackerNameChanged,
    label = stringResource(id = R.string.tracker_name),
    focusManager = focusManager,
    focusRequester = focusRequester,
    keyboardController = keyboardController
)

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
fun DurationInputPreview() = DurationInput(
    DurationInputViewModelImpl()
)

@Composable
fun DurationInput(
    durationInputViewModel: DurationInputViewModel
) = Row(
    modifier = Modifier
        .padding(
            horizontal = dimensionResource(id = R.dimen.card_padding),
            vertical = dimensionResource(id = R.dimen.card_padding)
        ),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.Center
) {
    val hours = durationInputViewModel.hours.observeAsState("")
    val minutes = durationInputViewModel.minutes.observeAsState("")
    val seconds = durationInputViewModel.seconds.observeAsState("")
    DurationInputComponent(
        value = hours.value,
        onValueChange = { durationInputViewModel.setHours(it) },
        suffix = stringResource(id = R.string.hours_suffix),
        charLimit = 8
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
    )
    DurationInputComponent(
        value = minutes.value,
        onValueChange = { durationInputViewModel.setMinutes(it) },
        suffix = stringResource(id = R.string.minutes_suffix),
        charLimit = 3
    )
    Text(
        text = ":",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
    )
    DurationInputComponent(
        value = seconds.value,
        onValueChange = { durationInputViewModel.setSeconds(it) },
        suffix = stringResource(id = R.string.seconds_suffix),
        charLimit = 3
    )
}

@Composable
private fun DurationInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    charLimit: Int
) {
    val focusManager = LocalFocusManager.current
    val colors = TextFieldDefaults.textFieldColors()
    val interactionSource = remember { MutableInteractionSource() }
    val textField = remember { mutableStateOf(TextFieldValue(value)) }

    if (textField.value.text != value) {
        textField.value = TextFieldValue(value, TextRange(value.length, value.length))
    }

    BasicTextField(
        value = textField.value,
        onValueChange = {
            if (it.text != textField.value.text && it.text.length <= charLimit) {
                textField.value = it
                onValueChange.invoke(it.text)
            }
        },
        textStyle = MaterialTheme.typography.labelSmall.copy(
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = {
            if (textField.value.text == "") Text(
                "0",
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = MaterialTheme.colorScheme.disabledAlpha()
                )
            )
            else it()
        },
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Right) }
        ),
        singleLine = true,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .widthIn(min = 40.dp, max = 80.dp)
            .indicatorLine(
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = colors
            )
            .onFocusChanged { focusState ->
                val textLength = textField.value.text.length
                if (focusState.isFocused) {
                    textField.value = textField.value.copy(
                        selection = TextRange(0, textLength)
                    )
                } else {
                    textField.value = textField.value.copy(
                        selection = TextRange(textLength, textLength)
                    )
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
    )
    Text(
        text = suffix,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun LabeledRow(
    label: String,
    input: @Composable () -> Unit
) = Row(
    modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.card_padding)),
    verticalAlignment = Alignment.CenterVertically
) {
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
