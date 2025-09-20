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
package com.samco.trackandgraph.functions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.functions.viewmodel.Node
import com.samco.trackandgraph.functions.viewmodel.ValidationError
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.WideButton
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
internal fun OutputNode(
    node: Node.Output,
    onCreateOrUpdate: () -> Unit = {}
) {
    val hasNameError = remember(node.validationErrors) { ValidationError.MISSING_NAME in node.validationErrors }
    val hasNoInputsError = remember(node.validationErrors) { ValidationError.NO_INPUTS in node.validationErrors }

    Column(
        Modifier
            .width(nodeCardContentWidth)
            .padding(horizontal = connectorSize / 2, vertical = cardPadding),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        // The extra button size row wrapper here makes sure the
        // text padding aligns with other cards which have a delete button
        Row(
            modifier = Modifier.heightIn(min = buttonSize),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.output),
                style = MaterialTheme.typography.titleMedium
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = node.name.value,
            onValueChange = { node.name.value = it },
            label = { Text(stringResource(id = R.string.function_name)) },
            maxLines = Int.MAX_VALUE,
            isError = hasNameError,
            colors = if (hasNameError) {
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = MaterialTheme.colorScheme.error,
                    focusedLabelColor = MaterialTheme.colorScheme.error,
                    unfocusedLabelColor = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            }
        )

        AnimatedVisibility(
            visible = hasNameError,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = stringResource(R.string.feature_name_cannot_be_null),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = node.description.value,
            onValueChange = { node.description.value = it },
            label = { Text(stringResource(R.string.add_a_longer_description_optional)) },
            maxLines = Int.MAX_VALUE
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { node.isDuration.value = !node.isDuration.value }
                .heightIn(min = buttonSize)
                .fillMaxWidth()
        ) {
            Checkbox(
                checked = node.isDuration.value,
                onCheckedChange = null
            )
            DialogInputSpacing()
            Text(stringResource(R.string.this_is_a_time_or_duration))
        }

        AnimatedVisibility(
            visible = hasNoInputsError,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = stringResource(R.string.add_at_least_one_input),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        WideButton(
            text = stringResource(
                if (node.isUpdateMode) R.string.update else R.string.create
            ),
            onClick = onCreateOrUpdate,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OutputNodePreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.Output(
                id = 1,
                name = remember { mutableStateOf(TextFieldValue("Sample Function")) },
                description = remember { mutableStateOf(TextFieldValue("This is a sample function description that shows how the output node looks.")) },
                isDuration = remember { mutableStateOf(false) },
                isUpdateMode = false,
                validationErrors = emptyList()
            )

            OutputNode(
                node = sampleNode,
                onCreateOrUpdate = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutputNodeUpdateModePreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.Output(
                id = 2,
                name = remember { mutableStateOf(TextFieldValue("Existing Function")) },
                description = remember { mutableStateOf(TextFieldValue("This function is being updated.")) },
                isDuration = remember { mutableStateOf(true) },
                isUpdateMode = true,
                validationErrors = emptyList()
            )

            OutputNode(
                node = sampleNode,
                onCreateOrUpdate = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutputNodeWithNameErrorPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.Output(
                id = 3,
                name = remember { mutableStateOf(TextFieldValue("")) },
                description = remember { mutableStateOf(TextFieldValue("Function with missing name")) },
                isDuration = remember { mutableStateOf(false) },
                isUpdateMode = false,
                validationErrors = listOf(ValidationError.MISSING_NAME)
            )

            OutputNode(
                node = sampleNode,
                onCreateOrUpdate = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutputNodeWithNoInputsErrorPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.Output(
                id = 4,
                name = remember { mutableStateOf(TextFieldValue("Function Name")) },
                description = remember { mutableStateOf(TextFieldValue("Function with no inputs connected")) },
                isDuration = remember { mutableStateOf(false) },
                isUpdateMode = false,
                validationErrors = listOf(ValidationError.NO_INPUTS)
            )

            OutputNode(
                node = sampleNode,
                onCreateOrUpdate = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutputNodeWithAllErrorsPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleNode = Node.Output(
                id = 5,
                name = remember { mutableStateOf(TextFieldValue("")) },
                description = remember { mutableStateOf(TextFieldValue("Function with all validation errors")) },
                isDuration = remember { mutableStateOf(true) },
                isUpdateMode = false,
                validationErrors = listOf(ValidationError.MISSING_NAME, ValidationError.NO_INPUTS)
            )

            OutputNode(
                node = sampleNode,
                onCreateOrUpdate = { }
            )
        }
    }
}
