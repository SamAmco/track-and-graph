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

package com.samco.trackandgraph.group

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

class GroupViewHolder private constructor(
    private val composeView: ComposeView,
) : GroupChildViewHolder(composeView) {
    private var clickListener: GroupClickListener? = null
    private var group: Group? = null
    private var isElevated by mutableStateOf(false)

    fun bind(
        groupItem: Group,
        clickListener: GroupClickListener
    ) {
        this.group = groupItem
        this.clickListener = clickListener

        composeView.setContent {
            TnGComposeTheme {
                Group(
                    isElevated = isElevated,
                    group = groupItem,
                    onEdit = { clickListener.onEdit.invoke(it) },
                    onDelete = { clickListener.onDelete(it) },
                    onMoveTo = { clickListener.onMove(it) },
                    onClick = { clickListener.onClick(it) }
                )
            }
        }

        //This fixes a bug because compose views don't calculate their height immediately,
        // scrolling up through a recycler view causes jumpy behavior. See this issue:
        // https://issuetracker.google.com/issues/240449681
        composeView.getChildAt(0)?.requestLayout()
    }

    override fun elevateCard() {
        isElevated = true
    }

    override fun dropCard() {
        isElevated = false
    }

    companion object {
        fun from(parent: ViewGroup): GroupViewHolder {
            val composeView = ComposeView(parent.context)
            return GroupViewHolder(composeView)
        }
    }
}

class GroupClickListener(
    val onClick: (group: Group) -> Unit,
    val onEdit: (group: Group) -> Unit,
    val onDelete: (group: Group) -> Unit,
    val onMove: (group: Group) -> Unit
)
