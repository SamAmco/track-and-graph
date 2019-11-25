package com.samco.trackandgraph.tutorial

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.viewpager.widget.PagerAdapter
import com.samco.trackandgraph.R


class TutorialPagerAdapter(
    private val context: Context,
    private val closeTutorialCallback: () -> Unit)
: PagerAdapter() {

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val resId = when (position) {
            0 -> R.layout.tutorial_page_1
            1 -> R.layout.tutorial_page_2
            else -> R.layout.tutorial_page_3
        }
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(resId, collection, false) as ViewGroup
        val closeButton = layout.findViewById<Button?>(R.id.closeButton)
        closeButton?.setOnClickListener { closeTutorialCallback() }
        collection.addView(layout)
        return layout
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View?)
    }

    override fun getCount() = 3

    override fun isViewFromObject(view: View, `object`: Any) = view === `object`
}