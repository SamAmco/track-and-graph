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
* along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.samco.trackandgraph.tutorial.TutorialPagerAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    private val navFragments = setOf(
        R.id.selectGroupFragment,
        R.id.FAQFragment,
        R.id.aboutPageFragment,
        R.id.remindersFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navController = findNavController(R.id.nav_fragment)
        appBarConfiguration = AppBarConfiguration(navFragments, drawerLayout)
        navView = findViewById(R.id.nav_view)
        navView.itemIconTintList = null

        setSupportActionBar(findViewById(R.id.toolbar))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (isFirstRun()) showTutorial()
        else destroyTutorial()
    }

    private fun destroyTutorial() {
        val tutorialLayout = findViewById<ViewGroup>(R.id.tutorialOverlay)
        tutorialLayout.removeAllViews()
        getPrefs().edit().putBoolean("firstrun", false).apply()
    }

    private fun showTutorial() {
        val pips = listOf(
            findViewById<ImageView>(R.id.pip1),
            findViewById(R.id.pip2),
            findViewById(R.id.pip3)
        )
        val viewPager = findViewById<ViewPager>(R.id.tutorialViewPager)
        val refreshPips = { position: Int ->
            pips.forEachIndexed { i, p -> p.alpha = if (i == position) 1f else 0.5f }
        }
        refreshPips.invoke(0)
        viewPager.visibility = View.VISIBLE
        viewPager.adapter = TutorialPagerAdapter(applicationContext, this::destroyTutorial)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
            override fun onPageSelected(position: Int) { refreshPips.invoke(position) }
        })
    }

    private fun getPrefs() = getSharedPreferences("com.samco.trackandgraph", MODE_PRIVATE)

    private fun isFirstRun() = getPrefs().getBoolean("firstrun", true)

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
