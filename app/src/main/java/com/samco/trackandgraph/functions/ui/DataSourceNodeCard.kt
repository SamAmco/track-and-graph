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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.functions.viewmodel.Node
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
internal fun DataSourceNode(
    node: Node.DataSource,
    onDeleteNode: () -> Unit = {},
) {
    Column(
        Modifier
            .width(nodeCardContentWidth)
            .padding(horizontal = connectorSize / 2, vertical = cardPadding),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.data_source),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                modifier = Modifier.size(buttonSize),
                onClick = onDeleteNode
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }

        var showSelectDialog by rememberSaveable { mutableStateOf(false) }

        val featurePath = remember(node.selectedFeatureId.value) {
            node.featurePathMap[node.selectedFeatureId.value] ?: ""
        }

        SelectorButton(
            modifier = Modifier.fillMaxWidth(),
            text = featurePath,
            onClick = { showSelectDialog = true }
        )

        if (showSelectDialog) {
            SelectItemDialog(
                title = stringResource(R.string.select_a_feature),
                selectableTypes = setOf(SelectableItemType.FEATURE),
                onFeatureSelected = { selectedFeatureId ->
                    node.selectedFeatureId.value = selectedFeatureId
                    showSelectDialog = false
                },
                onDismissRequest = { showSelectDialog = false }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSourceNodePreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleFeaturePathMap = mapOf(
                1L to "Health / Weight",
                2L to "Exercise / Running Distance",
                3L to "Mood / Daily Rating",
                4L to "Sleep / Hours Slept"
            )

            val sampleNode = Node.DataSource(
                id = 3,
                selectedFeatureId = remember { mutableLongStateOf(1L) },
                featurePathMap = sampleFeaturePathMap
            )

            DataSourceNode(
                node = sampleNode,
                onDeleteNode = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSourceNodeNoSelectionPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            val sampleFeaturePathMap = mapOf(
                1L to "Health / Weight",
                2L to "Exercise / Running Distance",
                3L to "Mood / Daily Rating"
            )

            val sampleNode = Node.DataSource(
                id = 4,
                selectedFeatureId = remember { mutableStateOf(-1L) }, // No selection
                featurePathMap = sampleFeaturePathMap
            )

            DataSourceNode(
                node = sampleNode,
                onDeleteNode = { }
            )
        }
    }
}
