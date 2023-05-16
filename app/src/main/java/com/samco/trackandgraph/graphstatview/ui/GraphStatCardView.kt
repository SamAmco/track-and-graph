package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.group.GraphStatClickListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow


//This fixes a bug where the card would show a ripple effect when the user long pressed on the card
// but not remove the ripple effect when the user released the long press. I assume this is because
// we are wrapping composable interaction code in recycler view item touch helper code and item
// touch helper does not effectively cancel the interaction when the user goes into long press/dragging
// mode.
@Stable
private class AutoCancelMutableInteractionSourceImpl : MutableInteractionSource {
    private val interactionsInput = MutableSharedFlow<Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val interactions: Flow<Interaction> = interactionsInput
        .flatMapLatest {
            flow {
                emit(it)
                if (it is PressInteraction.Press) {
                    delay(1000)
                    emit(PressInteraction.Release(PressInteraction.Press(Offset(0f, 0f))))
                }
            }
        }


    override suspend fun emit(interaction: Interaction) {
        interactionsInput.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        return interactionsInput.tryEmit(interaction)
    }
}

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
                if (clickListener != null) {
                    it.clickable(
                        indication = LocalIndication.current,
                        interactionSource = AutoCancelMutableInteractionSourceImpl()
                    ) { clickListener.onClick(graphStatViewData) }
                } else it
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

            GraphStatView(
                graphStatViewData = graphStatViewData,
                listMode = true
            )
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