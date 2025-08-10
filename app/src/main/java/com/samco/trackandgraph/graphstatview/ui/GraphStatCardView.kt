package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.group.GraphStatClickListener
import com.samco.trackandgraph.ui.compose.ui.cardElevation
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
        elevation = if (isElevated) cardElevation * 3f else cardElevation
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

            ListItemGraphStatView(graphStatViewData = graphStatViewData)
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
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(onClick = {
            clickListener.onDelete(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(stringResource(id = R.string.delete))
        }
        DropdownMenuItem(onClick = {
            clickListener.onEdit(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(stringResource(id = R.string.edit))
        }
        DropdownMenuItem(onClick = {
            clickListener.onMoveGraphStat(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(stringResource(id = R.string.move_to))
        }
        DropdownMenuItem(onClick = {
            clickListener.onDuplicate(graphStat = graphStatViewData)
            expanded = false
        }) {
            Text(stringResource(id = R.string.duplicate))
        }
    }
}