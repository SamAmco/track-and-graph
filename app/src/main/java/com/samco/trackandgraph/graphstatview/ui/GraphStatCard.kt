package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.group.GraphStatClickListener
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall

@Composable
fun GraphStatCardView(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    graphStatViewData: IGraphStatViewData,
    clickListener: GraphStatClickListener? = null
) = Box(
    modifier = Modifier
        .padding(dimensionResource(id = R.dimen.card_margin_small))
        .fillMaxWidth()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = if (isElevated)
            dimensionResource(id = R.dimen.card_elevation) * 3f
        else dimensionResource(R.dimen.card_elevation)
    ) {
        //TODO add a try catch around the graph views and show an error message if it fails
        Box(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_padding))
                .fillMaxWidth()
        ) {
            //TODO add menu button (only enable if clickListener is not null)
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