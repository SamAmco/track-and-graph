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

import android.graphics.drawable.RippleDrawable
import android.icu.text.MessageFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DisplayTracker
import com.samco.trackandgraph.base.helpers.DISPLAY_DAYS_AGO_SETTING_PREF_KEY
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinute
import com.samco.trackandgraph.base.helpers.formatTimeDuration
import com.samco.trackandgraph.base.helpers.getPrefs
import com.samco.trackandgraph.databinding.ListItemTrackerBinding
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit

class TrackerViewHolder private constructor(
    private val binding: ListItemTrackerBinding,
) : GroupChildViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
    private var clickListener: TrackerClickListener? = null
    private var tracker: DisplayTracker? = null
    private var dropElevation = 0f

    fun bind(tracker: DisplayTracker, clickListener: TrackerClickListener) {
        this.tracker = tracker
        this.clickListener = clickListener
        this.dropElevation = binding.cardView.cardElevation
        setLastDateText()
        setNumEntriesText()
        binding.trackGroupNameText.text = tracker.name
        binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        binding.cardView.setOnClickListener { clickListener.onHistory(tracker) }
        initAddButton(tracker, clickListener)
        initTimerControls(tracker, clickListener)
    }

    private fun initTimerControls(tracker: DisplayTracker, clickListener: TrackerClickListener) {
        binding.playStopButtons.visibility =
            if (tracker.dataType == DataType.DURATION) View.VISIBLE
            else View.GONE
        binding.playTimerButton.setOnClickListener {
            clickListener.onPlayTimer(tracker)
        }
        binding.stopTimerButton.setOnClickListener {
            clickListener.onStopTimer(tracker)
        }
        binding.playTimerButton.visibility =
            if (tracker.timerStartInstant == null) View.VISIBLE else View.GONE
        binding.stopTimerButton.visibility =
            if (tracker.timerStartInstant == null) View.GONE else View.VISIBLE

        if (tracker.timerStartInstant != null) {
            updateTimerText()
            binding.timerText.visibility = View.VISIBLE
        } else {
            binding.timerText.visibility = View.GONE
            binding.timerText.text = formatTimeDuration(0)
        }
    }

    override fun update() {
        super.update()
        updateTimerText()
    }

    private fun updateTimerText() {
        tracker?.timerStartInstant?.let {
            val duration = Duration.between(it, Instant.now())
            binding.timerText.text = formatTimeDuration(duration.seconds)
        }
    }

    private fun initAddButton(tracker: DisplayTracker, clickListener: TrackerClickListener) {
        binding.addButton.setOnClickListener { clickListener.onAdd(tracker) }
        binding.quickAddButton.setOnClickListener { onQuickAddClicked() }
        binding.quickAddButton.setOnLongClickListener {
            clickListener.onAdd(tracker, false).let { true }
        }
        if (tracker.hasDefaultValue) {
            binding.addButton.visibility = View.INVISIBLE
            binding.quickAddButton.visibility = View.VISIBLE
        } else {
            binding.addButton.visibility = View.VISIBLE
            binding.quickAddButton.visibility = View.INVISIBLE
        }
    }

    private fun setLastDateText() {
        val timestamp = tracker?.timestamp
        val displayDaysAgo = getPrefs(binding.lastDateText.context).getBoolean(DISPLAY_DAYS_AGO_SETTING_PREF_KEY, false)
        binding.lastDateText.text = if (timestamp == null) {
            binding.lastDateText.context.getString(R.string.no_data)
        } else if (displayDaysAgo) {
            val message = binding.lastDateText.context.getString(R.string.days_ago)
            val midnight = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1)
            val daysSinceLastUpdate = Duration.between(timestamp, midnight).toDays()
            MessageFormat.format(message, daysSinceLastUpdate)
        } else {
            formatDayMonthYearHourMinute(binding.lastDateText.context, timestamp)
        }
    }

    private fun setNumEntriesText() {
        val numDataPoints = tracker?.numDataPoints
        binding.numEntriesText.text = if (numDataPoints != null) {
            binding.numEntriesText.context.getString(R.string.data_points, numDataPoints)
        } else {
            binding.numEntriesText.context.getString(R.string.no_data)
        }
    }

    private fun onQuickAddClicked() {
        val ripple = binding.cardView.foreground as RippleDrawable
        ripple.setHotspot(ripple.bounds.right.toFloat(), ripple.bounds.bottom.toFloat())
        ripple.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        ripple.state = intArrayOf()
        tracker?.let { clickListener?.onAdd(it) }
    }

    override fun elevateCard() {
        binding.cardView.postDelayed({
            binding.cardView.cardElevation = binding.cardView.cardElevation * 3f
        }, 10)
    }

    override fun dropCard() {
        binding.cardView.cardElevation = dropElevation
    }

    private fun createContextMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.edit_tracker_context_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        tracker?.let {
            when (item?.itemId) {
                R.id.edit -> clickListener?.onEdit(it)
                R.id.delete -> clickListener?.onDelete(it)
                R.id.moveTo -> clickListener?.onMoveTo(it)
                R.id.description -> clickListener?.onDescription(it)
                else -> {
                }
            }
        }
        return false
    }

    companion object {
        fun from(parent: ViewGroup): TrackerViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemTrackerBinding.inflate(layoutInflater, parent, false)
            return TrackerViewHolder(binding)
        }
    }
}

class TrackerClickListener(
    private val onEditListener: (tracker: DisplayTracker) -> Unit,
    private val onDeleteListener: (tracker: DisplayTracker) -> Unit,
    private val onMoveToListener: (tracker: DisplayTracker) -> Unit,
    private val onDescriptionListener: (tracker: DisplayTracker) -> Unit,
    private val onAddListener: (tracker: DisplayTracker, useDefault: Boolean) -> Unit,
    private val onHistoryListener: (tracker: DisplayTracker) -> Unit,
    private val onPlayTimerListener: (tracker: DisplayTracker) -> Unit,
    private val onStopTimerListener: (tracker: DisplayTracker) -> Unit,
) {
    fun onEdit(tracker: DisplayTracker) = onEditListener(tracker)
    fun onDelete(tracker: DisplayTracker) = onDeleteListener(tracker)
    fun onMoveTo(tracker: DisplayTracker) = onMoveToListener(tracker)
    fun onDescription(tracker: DisplayTracker) = onDescriptionListener(tracker)
    fun onAdd(tracker: DisplayTracker, useDefault: Boolean = true) =
        onAddListener(tracker, useDefault)

    fun onHistory(tracker: DisplayTracker) = onHistoryListener(tracker)
    fun onPlayTimer(tracker: DisplayTracker) = onPlayTimerListener(tracker)
    fun onStopTimer(tracker: DisplayTracker) = onStopTimerListener(tracker)
}
