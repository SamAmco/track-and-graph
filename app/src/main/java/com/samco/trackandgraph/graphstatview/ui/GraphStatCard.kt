package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.group.GraphStatClickListener

@Composable
fun GraphStatCardView(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    graphStatViewData: IGraphStatViewData,
    clickListener: GraphStatClickListener? = null
) = Card(
    modifier = modifier,
    elevation = if (isElevated)
        dimensionResource(id = R.dimen.card_elevation) * 3f
    else dimensionResource(R.dimen.card_elevation)
) {
    //TODO add a try catch around the graph views and show an error message if it fails
    Box(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding))
    ) {
        //TODO add menu button (only enable if clickListener is not null)
        Column {
            Text(
                text = graphStatViewData.graphOrStat.name,
                style = MaterialTheme.typography.h6
            )
            Box(modifier = Modifier.weight(1f)) {
                if (graphStatViewData.state == IGraphStatViewData.State.LOADING) {
                    //TODO add loading indicator
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