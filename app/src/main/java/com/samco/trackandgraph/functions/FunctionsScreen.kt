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
package com.samco.trackandgraph.functions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.AddCreateBar
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FullWidthTextField
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable

@Serializable
data class FunctionsNavKey(
    val groupId: Long = 0L,
    val functionId: Long? = null
) : NavKey

@Composable
fun FunctionsScreen(
    navArgs: FunctionsNavKey,
    onPopBack: () -> Unit
) {
    val viewModel: FunctionsViewModel = hiltViewModel<FunctionsViewModelImpl>()

    // Initialize ViewModel with the arguments from NavKey
    LaunchedEffect(navArgs.groupId, navArgs.functionId) {
        viewModel.init(navArgs.groupId, navArgs.functionId)
    }

    // Handle navigation back when complete
    LaunchedEffect(viewModel.complete) {
        viewModel.complete.receiveAsFlow().collect {
            onPopBack()
        }
    }

    TopAppBarContent(navArgs)

    val errorText by viewModel.errorText.collectAsState()

    FunctionsView(
        functionName = viewModel.functionName,
        functionDescription = viewModel.functionDescription,
        errorText = errorText,
        onFunctionNameChanged = viewModel::onFunctionNameChanged,
        onFunctionDescriptionChanged = viewModel::onFunctionDescriptionChanged,
        onCreateClicked = viewModel::onCreateClicked
    )
}

@Composable
private fun TopAppBarContent(
    navKey: FunctionsNavKey
) {
    val topBarController = LocalTopBarController.current
    val title = stringResource(R.string.function)

    topBarController.Set(
        navKey,
        AppBarConfig(
            title = title,
            backNavigationAction = true,
            appBarPinned = true
        )
    )
}

@Composable
private fun FunctionsView(
    functionName: TextFieldValue,
    functionDescription: TextFieldValue,
    errorText: Int?,
    onFunctionNameChanged: (TextFieldValue) -> Unit,
    onFunctionDescriptionChanged: (TextFieldValue) -> Unit,
    onCreateClicked: () -> Unit
) = TnGComposeTheme {
    val focusRequester = remember { FocusRequester() }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize()
        ) {
            FunctionsInputForm(
                modifier = Modifier.weight(1f),
                functionName = functionName,
                functionDescription = functionDescription,
                onFunctionNameChanged = onFunctionNameChanged,
                onFunctionDescriptionChanged = onFunctionDescriptionChanged,
                focusRequester = focusRequester
            )

            AddCreateBar(
                errorText = errorText,
                onCreateUpdateClicked = onCreateClicked,
                isUpdateMode = false
            )

            LaunchedEffect(true) { focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun FunctionsInputForm(
    modifier: Modifier,
    functionName: TextFieldValue,
    functionDescription: TextFieldValue,
    onFunctionNameChanged: (TextFieldValue) -> Unit,
    onFunctionDescriptionChanged: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .verticalScroll(state = rememberScrollState())
        .padding(
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal)
                .asPaddingValues()
        )
        .then(Modifier.padding(cardPadding)),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DialogInputSpacing()

    FullWidthTextField(
        textFieldValue = functionName,
        onValueChange = onFunctionNameChanged,
        label = stringResource(id = R.string.name_your_function),
        focusManager = focusManager,
        focusRequester = focusRequester,
        keyboardController = keyboardController
    )

    InputSpacingLarge()

    FullWidthTextField(
        textFieldValue = functionDescription,
        onValueChange = onFunctionDescriptionChanged,
        label = stringResource(id = R.string.add_a_longer_description_optional),
        singleLine = false
    )
}

@Preview(showBackground = true)
@Composable
fun FunctionsScreenPreview() {
    FunctionsView(
        functionName = TextFieldValue("Sample Function"),
        functionDescription = TextFieldValue("This is a sample function description that shows how the UI will look with some content."),
        errorText = null,
        onFunctionNameChanged = {},
        onFunctionDescriptionChanged = {},
        onCreateClicked = {}
    )
}

@Preview(showBackground = true)
@Composable
fun FunctionsScreenEmptyPreview() {
    FunctionsView(
        functionName = TextFieldValue(""),
        functionDescription = TextFieldValue(""),
        errorText = null,
        onFunctionNameChanged = {},
        onFunctionDescriptionChanged = {},
        onCreateClicked = {}
    )
}

@Preview(showBackground = true)
@Composable
fun FunctionsScreenErrorPreview() {
    FunctionsView(
        functionName = TextFieldValue(""),
        functionDescription = TextFieldValue(""),
        errorText = R.string.function_name_empty,
        onFunctionNameChanged = {},
        onFunctionDescriptionChanged = {},
        onCreateClicked = {}
    )
}
