package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.group.GraphStatClickListener
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall

@Composable
fun GraphStatCardView(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    graphStatViewData: IGraphStatViewData,
    clickListener: GraphStatClickListener? = null,
) = Box(
    modifier = Modifier
        .padding(dimensionResource(id = R.dimen.card_margin_small))
        .fillMaxWidth()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (clickListener != null) it.clickable { clickListener.onClick(graphStatViewData) }
                else it
            },
        elevation = if (isElevated)
            dimensionResource(id = R.dimen.card_elevation) * 3f
        else dimensionResource(R.dimen.card_elevation)
    ) {
        Box(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_padding))
                .fillMaxWidth()
        ) {
            if (clickListener != null) {
                MenuSection(
                    modifier = Modifier.align(Alignment.TopEnd),
                    clickListener = clickListener,
                    graphStatViewData = graphStatViewData
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = graphStatViewData.graphOrStat.name,
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center
                )
                SpacingSmall()
                if (graphStatViewData.state == IGraphStatViewData.State.LOADING) {
                    CircularProgressIndicator()
                } else {
                    GraphStatView(
                        graphStatViewData = graphStatViewData,
                        listMode = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSection(
    modifier: Modifier = Modifier,
    clickListener: GraphStatClickListener,
    graphStatViewData: IGraphStatViewData
) = Box(modifier = modifier) {

    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Icon(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { expanded = true },
        painter = painterResource(id = R.drawable.list_menu_icon),
        contentDescription = null
    )

    DropdownMenu(
        modifier = Modifier
            .widthIn(min = 180.dp)
            .background(MaterialTheme.colors.background),
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(onClick = {
            clickListener.onDelete(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(
                text = stringResource(id = R.string.delete),
                style = MaterialTheme.typography.body1
            )
        }
        DropdownMenuItem(onClick = {
            clickListener.onEdit(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(
                text = stringResource(id = R.string.edit),
                style = MaterialTheme.typography.body1
            )
        }
        DropdownMenuItem(onClick = {
            clickListener.onMoveGraphStat(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(
                text = stringResource(id = R.string.move_to),
                style = MaterialTheme.typography.body1
            )
        }
        DropdownMenuItem(onClick = {
            clickListener.onDuplicate(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(
                text = stringResource(id = R.string.duplicate),
                style = MaterialTheme.typography.body1
            )
        }
    }
}