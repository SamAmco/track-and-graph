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

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LuaGraphConfigViewModel
import com.samco.trackandgraph.graphstatinput.configviews.viewmodel.LuaGraphConfigViewModel.NetworkError
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngTypography
import com.samco.trackandgraph.ui.compose.ui.AddBarButton
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.IconTextButton
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LuaScriptEditDialog
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TextSubtitle2
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.luaCodeVisualTransformation
import com.samco.trackandgraph.ui.compose.ui.slimOutlinedTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.samco.trackandgraph.R as AppR

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

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        for (networkError in viewModel.networkErrorToastEvents) {
            showErrorToast(context, networkError)
        }
    }

    val showEditScriptDialog = rememberSaveable { mutableStateOf(false) }

    LuaGraphConfigView(
        scrollState = scrollState,
        featureMap = viewModel.featureMap,
        script = viewModel.script,
        scriptPreview = viewModel.scriptPreview
            .collectAsStateWithLifecycle().value,
        selectedFeatures = viewModel.selectedFeatures,
        getTextFieldFor = viewModel::getTextFieldFor,
        onUpdateFeatureName = viewModel::onUpdateFeatureName,
        onRemoveFeatureClicked = viewModel::onRemoveFeatureClicked,
        onSelectFeatureClicked = viewModel::onSelectFeatureClicked,
        onAddFeatureClicked = viewModel::onAddFeatureClicked,
        setScriptText = viewModel::setScriptText,
        onReadFile = viewModel::readFile,
        onUpdateScriptFromClipboard = viewModel::updateScriptFromClipboard,
        onOpenCommunityScripts = viewModel::openCommunityScripts,
        onUserConfirmDeepLink = viewModel::onUserConfirmDeepLink,
        onUserCancelDeepLink = viewModel::onUserCancelDeepLink,
        showEditScriptDialog = showEditScriptDialog,
        onClickInPreview = viewModel::onClickInPreview,
        showUserConfirmDeepLinkDialog = viewModel.showUserConfirmDeepLink
            .collectAsStateWithLifecycle(),
    )
}

private fun showErrorToast(context: Context, networkError: NetworkError) {
    val text = when (networkError) {
        is NetworkError.Uri -> context.getString(R.string.failed_to_download_file, networkError.uri)
        is NetworkError.Generic -> context.getString(R.string.network_generic_error)
    }
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

@Composable
private fun LuaGraphConfigView(
    scrollState: ScrollState,
    featureMap: Map<Long, String>?,
    script: TextFieldValue,
    scriptPreview: TextFieldValue,
    selectedFeatures: List<LuaGraphFeature>,
    getTextFieldFor: (Int) -> TextFieldValue,
    onUpdateFeatureName: (Int, TextFieldValue) -> Unit,
    onRemoveFeatureClicked: (Int) -> Unit,
    onSelectFeatureClicked: (Int, Long) -> Unit,
    onAddFeatureClicked: () -> Unit,
    setScriptText: (TextFieldValue) -> Unit,
    onReadFile: (Uri?) -> Unit,
    onUpdateScriptFromClipboard: (String) -> Unit,
    onOpenCommunityScripts: () -> Unit,
    onUserConfirmDeepLink: () -> Unit,
    onUserCancelDeepLink: () -> Unit,
    showEditScriptDialog: MutableState<Boolean>,
    onClickInPreview: (TextFieldValue) -> Unit,
    showUserConfirmDeepLinkDialog: State<Boolean>,
) {
    Dialogs(
        showEditScriptDialog = showEditScriptDialog,
        script = script,
        setScriptText = setScriptText,
        showUserConfirmDeepLink = showUserConfirmDeepLinkDialog.value,
        onUserConfirmDeepLink = onUserConfirmDeepLink,
        onUserCancelDeepLink = onUserCancelDeepLink,
    )

    DialogInputSpacing()

    Buttons(
        onReadFile = onReadFile,
        onUpdateScriptFromClipboard = onUpdateScriptFromClipboard,
        onOpenCommunityScripts = onOpenCommunityScripts,
    )

    InputSpacingLarge()

    ScriptTextInputPreview(
        scriptPreview = scriptPreview,
        script = script,
        onScriptPreviewClicked = {
            showEditScriptDialog.value = true
            onClickInPreview(it)
        }
    )

    InputSpacingLarge()

    Divider()

    InputSpacingLarge()

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
}

@Composable
private fun Dialogs(
    showEditScriptDialog: MutableState<Boolean>,
    script: TextFieldValue,
    setScriptText: (TextFieldValue) -> Unit,
    showUserConfirmDeepLink: Boolean,
    onUserConfirmDeepLink: () -> Unit,
    onUserCancelDeepLink: () -> Unit,
) {
    if (showEditScriptDialog.value) {
        LuaScriptEditDialog(
            script = script,
            onDismiss = { showEditScriptDialog.value = false },
            onValueChanged = { setScriptText(it) }
        )
    }

    if (showUserConfirmDeepLink) {
        LuaUserConfirmDeepLinkDialog(
            onConfirm = onUserConfirmDeepLink,
            onCancel = onUserCancelDeepLink
        )
    }
}

@Composable
private fun LuaUserConfirmDeepLinkDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) = ContinueCancelDialog(
    body = R.string.confirm_deep_link,
    onDismissRequest = onCancel,
    onConfirm = onConfirm,
    continueText = R.string.yes,
    cancelText = R.string.cancel,
)

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
) = Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Column(
        modifier = Modifier.padding(cardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FullWidthTextField(
                modifier = Modifier
                    .padding(start = cardPadding)
                    .weight(1f),
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

        DialogInputSpacing()

        TextMapSpinner(
            modifier = Modifier.padding(start = 4.dp),
            strings = features,
            selectedItem = lgf.featureId,
            onItemSelected = onChangeSelectedFeatureId
        )
    }
}

@Composable
private fun ScriptTextInputPreview(
    scriptPreview: TextFieldValue,
    script: TextFieldValue,
    onScriptPreviewClicked: (TextFieldValue) -> Unit,
) = Box {
    val interactionSource = remember { MutableInteractionSource() }

    val showEllipsis = scriptPreview.text.length != script.text.length

    val scriptPreviewText = remember(showEllipsis, scriptPreview.text) {
        if (showEllipsis) {
            scriptPreview.copy(text = scriptPreview.text + "\n")
        } else {
            scriptPreview
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    val localFocusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .slimOutlinedTextField()
            .drawWithContent {
                drawContent()
                drawRect(color = surfaceColor.copy(alpha = 0.3f))
            },
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        value = scriptPreviewText,
        onValueChange = {
            onScriptPreviewClicked(it)
            localFocusManager.clearFocus()
        },
        placeholder = {
            Text(
                text = stringResource(R.string.lua_script_input_hint),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        visualTransformation = luaCodeVisualTransformation(),
        textStyle = MaterialTheme.tngTypography.code,
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        )
    )

    if (showEllipsis) {
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp),
            text = "...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun Buttons(
    onReadFile: (Uri?) -> Unit,
    onUpdateScriptFromClipboard: (String) -> Unit,
    onOpenCommunityScripts: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
) {

    val clipboardManager = LocalClipboardManager.current
    IconTextButton(
        onClick = {
            val text = clipboardManager.getText()?.text
                ?: return@IconTextButton
            onUpdateScriptFromClipboard(text)
        },
        icon = AppR.drawable.content_paste,
        text = stringResource(R.string.paste)
    )

    DialogInputSpacing()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { onReadFile(it) }

    IconTextButton(
        onClick = { launcher.launch("*/*") },
        icon = AppR.drawable.folder_open,
        text = stringResource(R.string.file)
    )

    DialogInputSpacing()

    IconTextButton(
        onClick = { onOpenCommunityScripts() },
        icon = AppR.drawable.github_mark,
        text = stringResource(R.string.github)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLuaGraphConfigView() = TnGComposeTheme {
    Column {
        LuaGraphConfigView(
            scrollState = rememberScrollState(),
            featureMap = mapOf(1L to "Feature 1", 2L to "Feature 2"),
            script = TextFieldValue(),
            scriptPreview = TextFieldValue(
                """
                function main()
                    print("Hello, World!")
                end
            """.trimIndent()
            ),
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
            onReadFile = {},
            onUpdateScriptFromClipboard = {},
            onOpenCommunityScripts = {},
            onUserConfirmDeepLink = {},
            onUserCancelDeepLink = {},
            onClickInPreview = {},
            showEditScriptDialog = remember { mutableStateOf(false) },
            showUserConfirmDeepLinkDialog = remember { mutableStateOf(false) },
        )
    }
}
