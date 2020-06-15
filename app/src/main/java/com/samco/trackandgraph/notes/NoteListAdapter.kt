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

package com.samco.trackandgraph.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.database.DisplayNote
import com.samco.trackandgraph.databinding.ListItemNoteBinding
import com.samco.trackandgraph.ui.OrderedListAdapter

private val getIdForNote = { n: DisplayNote -> n.timestamp.hashCode().toLong() }

internal class NoteListAdapter(
    private val clickListener: NoteClickListener
) : OrderedListAdapter<DisplayNote, NoteViewHolder>(getIdForNote, NoteDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }
}

internal class NoteDiffCallback : DiffUtil.ItemCallback<DisplayNote>() {
    override fun areItemsTheSame(oldItem: DisplayNote, newItem: DisplayNote): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: DisplayNote, newItem: DisplayNote): Boolean {
        return oldItem == newItem
    }
}

internal class NoteViewHolder private constructor(private val binding: ListItemNoteBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private var clickListener: NoteClickListener? = null
    private var note: DisplayNote? = null

    fun bind(note: DisplayNote, clickListener: NoteClickListener) {
        this.note = note
        this.clickListener = clickListener
        //TODO initialise the view from the note
    }

    companion object {
        fun from(parent: ViewGroup): NoteViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemNoteBinding.inflate(layoutInflater, parent, false)
            return NoteViewHolder(binding)
        }
    }
}

internal class NoteClickListener(
    private val onEdit: (DisplayNote) -> Unit,
    private val onDelete: (DisplayNote) -> Unit
) {
    fun edit(note: DisplayNote) = onEdit(note)
    fun delete(note: DisplayNote) = onDelete(note)
}
