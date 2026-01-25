package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.cardPadding

class GraphStatClickListener(
    val onDelete: (graphStat: IGraphStatViewData) -> Unit,
    val onEdit: (graphStat: IGraphStatViewData) -> Unit,
    val onClick: (graphStat: IGraphStatViewData) -> Unit,
    val onMove: (graphStat: IGraphStatViewData) -> Unit,
    val onDuplicate: (graphStat: IGraphStatViewData) -> Unit
)

@Composable
fun GraphStatCardView(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    graphStatViewData: IGraphStatViewData,
    clickListener: GraphStatClickListener? = null,
) = Box(
    modifier = Modifier
        .padding(cardMarginSmall)
        .fillMaxWidth()
) {
    Surface(
        modifier = modifier
            .testTag("graphStatCard")
            .fillMaxWidth(),
        shadowElevation = if (isElevated) cardElevation * 3f else cardElevation,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (clickListener != null) {
                        it.clickable { clickListener.onClick(graphStatViewData) }
                    } else it
                }
        ) {
            if (clickListener != null) {
                MenuSection(
                    modifier = Modifier.align(Alignment.TopEnd),
                    clickListener = clickListener,
                    graphStatViewData = graphStatViewData
                )
            }

            Box(
                modifier = Modifier
                    .padding(cardPadding)
                    .fillMaxWidth()
            ) {
                ListItemGraphStatView(graphStatViewData = graphStatViewData)
            }
        }
    }
}

@Composable
private fun MenuSection(
    modifier: Modifier = Modifier,
    clickListener: GraphStatClickListener,
    graphStatViewData: IGraphStatViewData
) = Box(modifier = modifier.size(buttonSize)) {

    var expanded by remember { mutableStateOf(false) }

    IconButton(
        modifier = Modifier.size(buttonSize),
        onClick = { expanded = true }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.list_menu_icon),
            contentDescription = null
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.delete)) },
            onClick = {
                clickListener.onDelete(graphStatViewData)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.edit)) },
            onClick = {
                clickListener.onEdit(graphStatViewData)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.move_to)) },
            onClick = {
                clickListener.onMove(graphStatViewData)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.duplicate)) },
            onClick = {
                clickListener.onDuplicate(graphStatViewData)
                expanded = false
            }
        )
    }
}