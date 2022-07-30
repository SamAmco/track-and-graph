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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DisplayFeature
import com.samco.trackandgraph.databinding.ListItemFeatureBinding
import com.samco.trackandgraph.ui.formatDayMonthYearHourMinute

class FeatureViewHolder private constructor(private val binding: ListItemFeatureBinding) :
    GroupChildViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {

    private var clickListener: FeatureClickListener? = null
    private var feature: DisplayFeature? = null
    private var dropElevation = 0f

    fun bind(feature: DisplayFeature, clickListener: FeatureClickListener) {
        this.feature = feature
        this.clickListener = clickListener
        this.dropElevation = binding.cardView.cardElevation
        setLastDateText()
        setNumEntriesText()
        binding.trackGroupNameText.text = feature.name
        binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        binding.addButton.setOnClickListener { clickListener.onAdd(feature) }
        binding.quickAddButton.setOnClickListener { onQuickAddClicked() }
        binding.quickAddButton.setOnLongClickListener {
            clickListener.onAdd(feature, false).let { true }
        }
        if (feature.hasDefaultValue) {
            binding.addButton.visibility = View.INVISIBLE
            binding.quickAddButton.visibility = View.VISIBLE
        } else {
            binding.addButton.visibility = View.VISIBLE
            binding.quickAddButton.visibility = View.INVISIBLE
        }
        binding.cardView.setOnClickListener { clickListener.onHistory(feature) }
    }

    private fun setLastDateText() {
        val timestamp = feature?.timestamp
        binding.lastDateText.text = if (timestamp == null) {
            binding.lastDateText.context.getString(R.string.no_data)
        } else {
            formatDayMonthYearHourMinute(binding.lastDateText.context, timestamp)
        }
    }

    private fun setNumEntriesText() {
        val numDataPoints = feature?.numDataPoints
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
        feature?.let { clickListener?.onAdd(it) }
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
        popup.menuInflater.inflate(R.menu.edit_feature_context_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        feature?.let {
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
        fun from(parent: ViewGroup): FeatureViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemFeatureBinding.inflate(layoutInflater, parent, false)
            return FeatureViewHolder(binding)
        }
    }
}

class FeatureClickListener(
    private val onEditListener: (feature: DisplayFeature) -> Unit,
    private val onDeleteListener: (feature: DisplayFeature) -> Unit,
    private val onMoveToListener: (feature: DisplayFeature) -> Unit,
    private val onDescriptionListener: (feature: DisplayFeature) -> Unit,
    private val onAddListener: (feature: DisplayFeature, useDefault: Boolean) -> Unit,
    private val onHistoryListener: (feature: DisplayFeature) -> Unit
) {
    fun onEdit(feature: DisplayFeature) = onEditListener(feature)
    fun onDelete(feature: DisplayFeature) = onDeleteListener(feature)
    fun onMoveTo(feature: DisplayFeature) = onMoveToListener(feature)
    fun onDescription(feature: DisplayFeature) = onDescriptionListener(feature)
    fun onAdd(feature: DisplayFeature, useDefault: Boolean = true) =
        onAddListener(feature, useDefault)

    fun onHistory(feature: DisplayFeature) = onHistoryListener(feature)
}
