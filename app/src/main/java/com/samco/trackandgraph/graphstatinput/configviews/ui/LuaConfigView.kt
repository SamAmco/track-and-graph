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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LuaGraphConfigViewModel
import com.samco.trackandgraph.ui.compose.ui.AddBarButton
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.TextButton
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
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

    LuaGraphFeaturesInputView(scrollState, viewModel)

    Divider()

    ScriptTextInput(viewModel)

    BottomButtons(viewModel)
}

@Composable
private fun LuaGraphFeaturesInputView(
    scrollState: ScrollState,
    viewModel: LuaGraphConfigViewModel
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .animateContentSize(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    for (index in viewModel.selectedFeatures.indices) {
        val lgf = viewModel.selectedFeatures[index]
        LuaGraphFeatureInputView(
            lgf = lgf,
            features = viewModel.featureMap ?: emptyMap(),
            nameTextField = viewModel.getTextFieldFor(index),
            onUpdateName = { viewModel.onUpdateFeatureName(index, it) },
            onRemove = { viewModel.onRemoveFeatureClicked(index) },
            onChangeSelectedFeatureId = { viewModel.onSelectFeatureClicked(index, it) }
        )
        DialogInputSpacing()
    }

    val coroutineScope = rememberCoroutineScope()

    AddBarButton(
        onClick = {
            viewModel.onAddFeatureClicked()
            coroutineScope.launch {
                delay(200)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        },
    )
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
    viewModel: LuaGraphConfigViewModel,
) {
    val focusRequester = remember { FocusRequester() }
    FullWidthTextField(
        modifier = Modifier.heightIn(max = 400.dp),
        textFieldValue = viewModel.script,
        onValueChange = { viewModel.setScriptText(it) },
        focusRequester = focusRequester,
        label = stringResource(id = R.string.lua_script_input_hint),
        singleLine = false
    )
}

@Composable
private fun BottomButtons(
    viewModel: LuaGraphConfigViewModel
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { viewModel.readFile(it) }

    TextButton(
        onClick = { launcher.launch("*/*") },
        text = stringResource(R.string.load_file).uppercase()
    )
}