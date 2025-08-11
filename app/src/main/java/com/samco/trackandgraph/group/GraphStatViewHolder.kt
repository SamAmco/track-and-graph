/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

class GraphStatViewHolder(
    private val composeView: ComposeView,
) : GroupChildViewHolder(composeView) {

    private var isElevated by mutableStateOf(false)

    private lateinit var graphStatViewData: IGraphStatViewData
    private lateinit var clickListener: GraphStatClickListener

    fun bind(
        graphStat: IGraphStatViewData,
        clickListener: GraphStatClickListener
    ) {
        this.graphStatViewData = graphStat
        this.clickListener = clickListener

        composeView.setContent { GraphStatContent(graphStat = graphStat) }

        //This fixes a bug because compose views don't calculate their height immediately,
        // scrolling up through a recycler view causes jumpy behviour. See this issue:
        // https://issuetracker.google.com/issues/240449681
        composeView.getChildAt(0)?.requestLayout()
    }

    @Composable
    private fun GraphStatContent(graphStat: IGraphStatViewData) = TnGComposeTheme {
        key(graphStat) {
            GraphStatCardView(
                isElevated = isElevated,
                graphStatViewData = graphStatViewData,
                clickListener = clickListener
            )
        }
    }

    override fun elevateCard() {
        isElevated = true
    }

    override fun dropCard() {
        isElevated = false
    }
}

class GraphStatClickListener(
    val onDelete: (graphStat: IGraphStatViewData) -> Unit,
    val onEdit: (graphStat: IGraphStatViewData) -> Unit,
    val onClick: (graphStat: IGraphStatViewData) -> Unit,
    val onMove: (graphStat: IGraphStatViewData) -> Unit,
    val onDuplicate: (graphStat: IGraphStatViewData) -> Unit
)
