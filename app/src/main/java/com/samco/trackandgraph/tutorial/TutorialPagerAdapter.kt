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
package com.samco.trackandgraph.tutorial

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val closeButton = layout.findViewById<View>(R.id.closeButton)
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