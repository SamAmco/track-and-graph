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

package com.samco.trackandgraph.viewgraphstat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.base.helpers.getDisplayValue
import com.samco.trackandgraph.databinding.ListItemNoteBinding
import com.samco.trackandgraph.util.FeatureDataProvider

// class NotesAdapter(
//     private val featureDataProvider: FeatureDataProvider,
//     private val weekDayNames: List<String>,
//     private val clickListener: NoteClickListener
// ) : ListAdapter<GraphNote, NotesAdapter.ViewHolder>(
//     NoteDiffCallback()
// ) {
//
//     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//         return ViewHolder.from(
//             parent,
//             weekDayNames,
//             clickListener
//         )
//     }
//
//     override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//         holder.bind(getItem(position), featureDataProvider)
//     }
//
//     class ViewHolder private constructor(
//         private val binding: ListItemNoteBinding,
//         private val weekDayNames: List<String>,
//         private val clickListener: NoteClickListener
//     ) : RecyclerView.ViewHolder(binding.root) {
//
//         var note: GraphNote? = null
//
//         fun bind(note: GraphNote, featureDataProvider: FeatureDataProvider) {
//             this.note = note
//             binding.timestampText.text =
//                 formatDayMonthYearHourMinuteWeekDayOneLine(
//                     binding.timestampText.context,
//                     weekDayNames,
//                     note.timestamp
//                 )
//             when {
//                 note.isDataPoint() -> initFromDataPointNote(featureDataProvider)
//                 else -> initFromGlobalNote()
//             }
//         }
//
//         private fun initFromGlobalNote() {
//             val globalNote = note!!.globalNote!!
//             binding.valueText.visibility = View.GONE
//             binding.trackerNameText.visibility = View.GONE
//             binding.cardView.setOnClickListener { clickListener.viewClicked(note!!) }
//             binding.noteText.visibility = View.VISIBLE
//             binding.noteText.text = globalNote.note
//         }
//
//         private fun initFromDataPointNote(featureDataProvider: FeatureDataProvider) {
//             val dataPoint = note!!.dataPoint!!
//             binding.valueText.visibility = View.VISIBLE
//             val isDuration = featureDataProvider.getDataSampleProperties(dataPoint.featureId)
//                 ?.isDuration ?: false
//             binding.valueText.text = note!!.dataPoint!!.getDisplayValue(isDuration)
//             binding.trackerNameText.visibility = View.VISIBLE
//             binding.trackerNameText.text =
//                 featureDataProvider.getPathForFeature(dataPoint.featureId)
//             binding.cardView.setOnClickListener { clickListener.viewClicked(note!!) }
//             binding.noteText.visibility = View.VISIBLE
//             binding.noteText.text = dataPoint.note
//         }
//
//         companion object {
//             fun from(
//                 parent: ViewGroup,
//                 weekDayNames: List<String>,
//                 clickListener: NoteClickListener
//             ): ViewHolder {
//                 val layoutInflater = LayoutInflater.from(parent.context)
//                 val binding = ListItemNoteBinding.inflate(layoutInflater, parent, false)
//                 return ViewHolder(
//                     binding,
//                     weekDayNames,
//                     clickListener
//                 )
//             }
//         }
//     }
// }
//
// class NoteDiffCallback : DiffUtil.ItemCallback<GraphNote>() {
//     override fun areItemsTheSame(oldItem: GraphNote, newItem: GraphNote) =
//         oldItem.timestamp == newItem.timestamp
//                 && oldItem.dataPoint?.featureId == newItem.dataPoint?.featureId
//                 && oldItem.globalNote?.timestamp == newItem.globalNote?.timestamp
//
//     override fun areContentsTheSame(oldItem: GraphNote, newItem: GraphNote) =
//         oldItem.dataPoint == newItem.dataPoint
//                 && oldItem.globalNote == newItem.globalNote
// }
//
// class NoteClickListener(private val onViewDataPoint: (GraphNote) -> Unit) {
//     fun viewClicked(note: GraphNote) = onViewDataPoint(note)
// }
