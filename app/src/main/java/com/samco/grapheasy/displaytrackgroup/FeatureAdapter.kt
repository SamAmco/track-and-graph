package com.samco.grapheasy.displaytrackgroup

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.databinding.ListItemFeatureBinding

class FeatureAdapter(
    private val clickListener: FeatureClickListener,
    private val viewModel: DisplayTrackGroupViewModel
) : ListAdapter<Feature, FeatureAdapter.ViewHolder>(FeatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, viewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder private constructor(private val binding: ListItemFeatureBinding,
                                         private val viewModel: DisplayTrackGroupViewModel)
        : RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {

        private var clickListener: FeatureClickListener? = null
        private var feature: Feature? = null

        fun bind(feature: Feature, clickListener: FeatureClickListener) {
            this.feature = feature
            this.clickListener = clickListener
            binding.feature = feature
            binding.clickListener = clickListener
            binding.viewModel = viewModel
            binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
            binding.addButton.setOnClickListener { clickListener.onAdd(feature) }
            binding.historyButton.setOnClickListener { clickListener.onHistory(feature) }
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
                    R.id.rename -> clickListener?.onRename(it)
                    R.id.delete -> clickListener?.onDelete(it)
                    else -> {}
                }
            }
            return false
        }

        companion object {
            fun from(parent: ViewGroup, viewModel: DisplayTrackGroupViewModel): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemFeatureBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, viewModel)
            }
        }
    }
}

class FeatureDiffCallback : DiffUtil.ItemCallback<Feature>() {
    override fun areItemsTheSame(oldItem: Feature, newItem: Feature): Boolean {
        return oldItem.id == newItem.id
    }
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Feature, newItem: Feature): Boolean {
        return oldItem == newItem
    }
}

class FeatureClickListener(private val onRenameListener: (feature: Feature) -> Unit,
                           private val onDeleteListener: (feature: Feature) -> Unit,
                           private val onAddListener: (feature: Feature) -> Unit,
                           private val onHistoryListener: (feature: Feature) -> Unit) {
    fun onRename(feature: Feature) = onRenameListener(feature)
    fun onDelete(feature: Feature) = onDeleteListener(feature)
    fun onAdd(feature: Feature) = onAddListener(feature)
    fun onHistory(feature: Feature) = onHistoryListener(feature)
}
