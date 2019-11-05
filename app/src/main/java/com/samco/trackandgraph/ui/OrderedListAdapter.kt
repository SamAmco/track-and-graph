package com.samco.trackandgraph.ui

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class OrderedListAdapter<T, G : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, G>(diffCallback) {

    private var list: MutableList<T> = mutableListOf()

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = list.removeAt(fromPosition)
        list.add(toPosition, item)
        super.notifyItemMoved(fromPosition, toPosition)
    }

    override fun submitList(list: MutableList<T>?) {
        list?.let { this.list = list }
        super.submitList(list)
    }

    fun getItems() = list
}
