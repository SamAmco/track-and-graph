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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

class TrackerViewHolder private constructor(
    private val composeView: ComposeView,
) : GroupChildViewHolder(composeView) {
    private var clickListener: TrackerClickListener? = null
    private var tracker: DisplayTracker? = null
    private var isElevated by mutableStateOf(false)

    fun bind(tracker: DisplayTracker, clickListener: TrackerClickListener) {
        this.tracker = tracker
        this.clickListener = clickListener

        composeView.setContent {
            TnGComposeTheme {
                Tracker(
                    isElevated = isElevated,
                    tracker = tracker,
                    onEdit = { clickListener.onEdit(it) },
                    onDelete = { clickListener.onDelete(it) },
                    onMoveTo = { clickListener.onMoveTo(it) },
                    onDescription = { clickListener.onDescription(it) },
                    onAdd = { t, useDefault -> clickListener.onAdd(t, useDefault) },
                    onHistory = { clickListener.onHistory(it) },
                    onPlayTimer = { clickListener.onPlayTimer(it) },
                    onStopTimer = { clickListener.onStopTimer(it) }
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
        fun from(parent: ViewGroup): TrackerViewHolder {
            val composeView = ComposeView(parent.context)
            return TrackerViewHolder(composeView)
        }
    }
}

class TrackerClickListener(
    val onEdit: (tracker: DisplayTracker) -> Unit,
    val onDelete: (tracker: DisplayTracker) -> Unit,
    val onMoveTo: (tracker: DisplayTracker) -> Unit,
    val onDescription: (tracker: DisplayTracker) -> Unit,
    val onAdd: (tracker: DisplayTracker, useDefault: Boolean) -> Unit,
    val onHistory: (tracker: DisplayTracker) -> Unit,
    val onPlayTimer: (tracker: DisplayTracker) -> Unit,
    val onStopTimer: (tracker: DisplayTracker) -> Unit,
)
