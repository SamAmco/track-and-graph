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
package com.samco.trackandgraph

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.samco.trackandgraph.base.helpers.*
import com.samco.trackandgraph.base.model.AlarmInteractor
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandler
import com.samco.trackandgraph.tutorial.TutorialPagerAdapter
import com.samco.trackandgraph.util.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NavButtonStyle { UP, MENU }

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    private lateinit var currentNavBarConfig: NavBarConfig

    @Inject
    lateinit var dataInteractor: DataInteractor

    @Inject
    lateinit var prefHelper: PrefHelper

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    private val viewModel by viewModels<MainActivityViewModel>()

    val toolbar: Toolbar by lazy { findViewById(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readThemeValue()
        setContentView(R.layout.activity_main)
        initializeNav()
        initializeAppBar()
        onDrawerHideKeyboard()
        initDrawerSpinners()
        viewModel.syncAlarms()
        if (prefHelper.isFirstRun()) showTutorial()
        else destroyTutorial()
        intent?.data?.let { handleDeepLink(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) = deepLinkHandler.handleUri(uri.toString())

    private fun initializeNav() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_fragment)!! as NavHostFragment
        navController = navHostFragment.navController
        navView = findViewById(R.id.nav_view)
        navView.setupWithNavController(navController)
    }

    private fun initializeAppBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            //The ActionBarDrawerToggle draws the navigation button/back button in the top left of
            //the action bar
            actionBarDrawerToggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open, R.string.close
            )
            //This click listener is called only when the button is "disabled" (i.e. the back button
            // is showing rather than the hamburger icon)
            actionBarDrawerToggle.setToolbarNavigationClickListener { onBackPressed() }
            //This notifies the button of when the drawer is open or closed
            drawerLayout.addDrawerListener(actionBarDrawerToggle)
            //This function should be called to synchronise the button with the drawers current state
            actionBarDrawerToggle.syncState()
        }
        //supportActionBar?.setHomeButtonEnabled(true)
        //supportActionBar?.setDisplayShowHomeEnabled(true)
        setActionBarConfig(NavBarConfig(NavButtonStyle.MENU))
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        //See the documentation of syncState for the eplanation of why this is done here
        actionBarDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //Doing this here was reccomended on stack overflow. See the documentation of syncState
        actionBarDrawerToggle.syncState()
    }

    private data class NavBarConfig(
        val buttonStyle: NavButtonStyle,
        val title: String? = null,
        val clearSubtitle: Boolean = false
    )

    fun setActionBarSubtitle(subtitle: String? = null) {
        supportActionBar?.subtitle = subtitle
    }

    /**
     * Set the title in the action bar and whether to show the menu button or the back button
     * in the top left. Every fragment should call this.
     */
    fun setActionBarConfig(buttonStyle: NavButtonStyle, title: String? = null, clearSubtitle: Boolean = false) {
        setActionBarConfig(NavBarConfig(buttonStyle, title, clearSubtitle))
    }

    private fun setActionBarConfig(config: NavBarConfig) {
        currentNavBarConfig = config
        val title = config.title ?: getString(R.string.app_name)
        supportActionBar?.title = title

        if (!config.clearSubtitle)
        {
            supportActionBar?.subtitle = null
        }

        when (config.buttonStyle) {
            NavButtonStyle.MENU -> {
                actionBarDrawerToggle.isDrawerIndicatorEnabled = true
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            NavButtonStyle.UP -> {
                actionBarDrawerToggle.isDrawerIndicatorEnabled = false
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }
        actionBarDrawerToggle.syncState()
    }

    private fun initDrawerSpinners() {
        setUpThemeSpinner()
        setUpDateFormatSpinner()
    }

    private fun setUpDateFormatSpinner() {
        val spinner = navView.menu.findItem(R.id.dateFormatSpinner).actionView as AppCompatSpinner
        val formatNames = resources.getStringArray(R.array.date_formats)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            formatNames
        )
        spinner.setSelection(prefHelper.getDateFormatValue())
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(av: AdapterView<*>?, v: View?, position: Int, id: Long) {
                prefHelper.setDateTimeFormatIndex(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun setUpThemeSpinner() {
        val spinner = navView.menu.findItem(R.id.themeSpinner).actionView as AppCompatSpinner
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getThemeNames()
        )
        val currentTheme = getThemeValue()
        updateStatusBarColor()
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> spinner.setSelection(1)
            AppCompatDelegate.MODE_NIGHT_YES -> spinner.setSelection(2)
            else -> spinner.setSelection(0)
        }
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(av: AdapterView<*>?, v: View?, position: Int, id: Long) {
                onThemeSelected(position)
                updateStatusBarColor()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateStatusBarColor() {
        window.statusBarColor = getColorFromAttr(R.attr.colorSecondaryVariant)
    }

    private fun onThemeSelected(position: Int) {
        when (position) {
            0 -> setThemeValue(getDefaultThemeValue())
            1 -> setThemeValue(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> setThemeValue(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun getDefaultThemeValue() =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY

    private fun getThemeValue() = prefHelper.getThemeValue(getDefaultThemeValue())

    private fun setThemeValue(themeValue: Int) {
        AppCompatDelegate.setDefaultNightMode(themeValue)
        prefHelper.setThemeValue(themeValue)
    }

    private fun readThemeValue() {
        val themeValue = getThemeValue()
        AppCompatDelegate.setDefaultNightMode(themeValue)
    }

    private fun getThemeNames() =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            resources.getStringArray(R.array.theme_names_Q)
        else resources.getStringArray(R.array.theme_names_pre_Q)

    private fun onDrawerHideKeyboard() {
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                window.hideKeyboard()
            }

            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerOpened(drawerView: View) {
                window.hideKeyboard()
            }
        })
    }

    private fun destroyTutorial() {
        val tutorialLayout = findViewById<ViewGroup>(R.id.tutorialOverlay)
        tutorialLayout.removeAllViews()
        prefHelper.setFirstRun(false)
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
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                refreshPips.invoke(position)
            }
        })
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(drawerLayout) || super.onNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val alarmInteractor: AlarmInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    fun syncAlarms() {
        viewModelScope.launch(io) {
            alarmInteractor.syncAlarms()
        }
    }
}
