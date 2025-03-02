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
package com.samco.trackandgraph.graphstatinput.configviews.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LuaGraphConfigViewModel
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.AddBarButton
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.TextButton
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TextSubtitle2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LuaGraphConfigView(
    scrollState: ScrollState,
    viewModelStoreOwner: ViewModelStoreOwner,
    graphStatId: Long,
    onConfigEvent: (GraphStatConfigEvent?) -> Unit
) {
    val viewModel = hiltViewModel<LuaGraphConfigViewModel>(viewModelStoreOwner).apply {
        initFromGraphStatId(graphStatId)
    }

    LaunchedEffect(viewModel) {
        viewModel.getConfigFlow().collect { onConfigEvent(it) }
    }

    LuaGraphConfigView(
        scrollState = scrollState,
        featureMap = viewModel.featureMap,
        script = viewModel.script,
        selectedFeatures = viewModel.selectedFeatures,
        getTextFieldFor = viewModel::getTextFieldFor,
        onUpdateFeatureName = viewModel::onUpdateFeatureName,
        onRemoveFeatureClicked = viewModel::onRemoveFeatureClicked,
        onSelectFeatureClicked = viewModel::onSelectFeatureClicked,
        onAddFeatureClicked = viewModel::onAddFeatureClicked,
        setScriptText = viewModel::setScriptText,
        readFile = viewModel::readFile
    )
}

@Composable
private fun LuaGraphConfigView(
    scrollState: ScrollState,
    featureMap: Map<Long, String>?,
    script: TextFieldValue,
    selectedFeatures: List<LuaGraphFeature>,
    getTextFieldFor: (Int) -> TextFieldValue,
    onUpdateFeatureName: (Int, TextFieldValue) -> Unit,
    onRemoveFeatureClicked: (Int) -> Unit,
    onSelectFeatureClicked: (Int, Long) -> Unit,
    onAddFeatureClicked: () -> Unit,
    setScriptText: (TextFieldValue) -> Unit,
    readFile: (Uri?) -> Unit
) {
    LuaGraphFeaturesInputView(
        scrollState = scrollState,
        selectedFeatures = selectedFeatures,
        featureMap = featureMap,
        getTextFieldFor = getTextFieldFor,
        onUpdateFeatureName = onUpdateFeatureName,
        onRemoveFeatureClicked = onRemoveFeatureClicked,
        onSelectFeatureClicked = onSelectFeatureClicked,
        onAddFeatureClicked = onAddFeatureClicked,
    )

    Divider()

    DialogInputSpacing()

    ScriptTextInput(
        script = script,
        setScriptText = setScriptText
    )

    DialogInputSpacing()

    BottomButtons(
        readFile = readFile
    )

    DialogInputSpacing()
}

@Composable
private fun LuaGraphFeaturesInputView(
    scrollState: ScrollState,
    selectedFeatures: List<LuaGraphFeature>,
    featureMap: Map<Long, String>?,
    getTextFieldFor: (Int) -> TextFieldValue,
    onUpdateFeatureName: (Int, TextFieldValue) -> Unit,
    onRemoveFeatureClicked: (Int) -> Unit,
    onSelectFeatureClicked: (Int, Long) -> Unit,
    onAddFeatureClicked: () -> Unit
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .animateContentSize(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    TextSubtitle2(text = stringResource(id = R.string.add_some_data_sources))

    DialogInputSpacing()

    for (index in selectedFeatures.indices) {
        val lgf = selectedFeatures[index]
        LuaGraphFeatureInputView(
            lgf = lgf,
            features = featureMap ?: emptyMap(),
            nameTextField = getTextFieldFor(index),
            onUpdateName = { onUpdateFeatureName(index, it) },
            onRemove = { onRemoveFeatureClicked(index) },
            onChangeSelectedFeatureId = { onSelectFeatureClicked(index, it) }
        )
        DialogInputSpacing()
    }

    val coroutineScope = rememberCoroutineScope()

    AddBarButton(
        onClick = {
            onAddFeatureClicked()
            coroutineScope.launch {
                delay(200)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        },
    )

    DialogInputSpacing()
}

@Composable
private fun LuaGraphFeatureInputView(
    lgf: LuaGraphFeature,
    features: Map<Long, String>,
    nameTextField: TextFieldValue,
    onUpdateName: (TextFieldValue) -> Unit,
    onRemove: () -> Unit,
    onChangeSelectedFeatureId: (Long) -> Unit,
) = Card {
    Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FullWidthTextField(
                modifier = Modifier.weight(1f),
                textFieldValue = nameTextField,
                onValueChange = onUpdateName,
            )
            IconButton(onClick = { onRemove() }) {
                Icon(
                    painter = painterResource(id = com.samco.trackandgraph.R.drawable.delete_icon),
                    contentDescription = stringResource(
                        id = com.samco.trackandgraph.R.string.delete_input_button_content_description
                    )
                )
            }
        }

        TextMapSpinner(
            strings = features,
            selectedItem = lgf.featureId,
            onItemSelected = onChangeSelectedFeatureId
        )
    }
}

@Composable
private fun ScriptTextInput(
    script: TextFieldValue,
    setScriptText: (TextFieldValue) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    FullWidthTextField(
        modifier = Modifier.heightIn(max = 400.dp),
        textFieldValue = script,
        onValueChange = { setScriptText(it) },
        focusRequester = focusRequester,
        label = stringResource(id = R.string.lua_script_input_hint),
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        )
    )
}

@Composable
private fun BottomButtons(
    readFile: (Uri?) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { readFile(it) }

    TextButton(
        onClick = { launcher.launch("*/*") },
        text = stringResource(R.string.load_file).uppercase()
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLuaGraphConfigView() = TnGComposeTheme {
    Column {
        LuaGraphConfigView(
            scrollState = rememberScrollState(),
            featureMap = mapOf(1L to "Feature 1", 2L to "Feature 2"),
            script = TextFieldValue("Sample Script"),
            selectedFeatures = listOf(
                LuaGraphFeature(id = 1, luaGraphId = 1, featureId = 1, name = "Feature 1"),
                LuaGraphFeature(id = 2, luaGraphId = 1, featureId = 2, name = "Feature 2")
            ),
            getTextFieldFor = { TextFieldValue("Feature Name $it") },
            onUpdateFeatureName = { _, _ -> },
            onRemoveFeatureClicked = {},
            onSelectFeatureClicked = { _, _ -> },
            onAddFeatureClicked = {},
            setScriptText = {},
            readFile = {}
        )
    }
}

