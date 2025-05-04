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
package com.samco.trackandgraph.main

import android.app.UiModeManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.IntentActions
import com.samco.trackandgraph.base.helpers.*
import com.samco.trackandgraph.base.model.AlarmInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.service.TimerServiceInteractor
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandler
import com.samco.trackandgraph.lua.LuaEngineSettingsProvider
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.tutorial.TutorialScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeSelection(
    val appCompatMode: Int,
    val uiManagerMode: Int,
) {
    LIGHT(
        appCompatMode = AppCompatDelegate.MODE_NIGHT_NO,
        uiManagerMode = UiModeManager.MODE_NIGHT_NO
    ),
    DARK(
        appCompatMode = AppCompatDelegate.MODE_NIGHT_YES,
        uiManagerMode = UiModeManager.MODE_NIGHT_YES
    ),
    SYSTEM(
        appCompatMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
        uiManagerMode = UiModeManager.MODE_NIGHT_AUTO
    );
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val themeMap = ThemeSelection.entries.associateBy { it.appCompatMode }

    private val uiModeManager by lazy { getSystemService(UI_MODE_SERVICE) as UiModeManager }

    @Inject
    lateinit var prefHelper: PrefHelper

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    @Inject
    lateinit var luaEngineSettingsProvider: LuaEngineSettingsProvider

    @Inject
    lateinit var urlNavigator: UrlNavigator

    private val viewModel by viewModels<MainActivityViewModel>()

    private val currentTheme: MutableState<ThemeSelection> by lazy { mutableStateOf(getThemeValue()) }
    private val currentDateFormat: MutableState<Int> by lazy { mutableStateOf(prefHelper.getDateFormatValue()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        checkDisableLuaEngine()
        viewModel.init()
        intent?.data?.let { handleDeepLink(it) }
        onThemeSelected(currentTheme.value)
        setContent {
            var showTutorial by remember { mutableStateOf(prefHelper.isFirstRun()) }
            AnimatedContent(showTutorial) { show ->
                if (show) {
                    TutorialScreen {
                        showTutorial = false
                        prefHelper.setFirstRun(false)
                    }
                } else {
                    MainScreen(
                        activity = this@MainActivity,
                        onNavigateToBrowser = ::onNavigateToBrowser,
                        currentTheme = currentTheme,
                        onThemeSelected = ::onThemeSelected,
                        currentDateFormat = currentDateFormat,
                        onDateFormatSelected = ::onDateFormatSelected,
                    )
                }
            }
        }
    }

    private fun onNavigateToBrowser(location: DrawerMenuBrowserLocation) {
        when (location) {
            DrawerMenuBrowserLocation.FAQ -> urlNavigator
                .triggerNavigation(this, UrlNavigator.Location.TUTORIAL_ROOT)

            DrawerMenuBrowserLocation.RATE_APP -> urlNavigator
                .triggerNavigation(this, UrlNavigator.Location.PLAY_STORE_PAGE)
        }
    }

    private fun getThemeFromPrefs() =
        themeMap[prefHelper.getThemeValue(ThemeSelection.SYSTEM.appCompatMode)]

    private fun getThemeValue() = getThemeFromPrefs() ?: ThemeSelection.SYSTEM
    private fun onThemeSelected(theme: ThemeSelection) {
        currentTheme.value = theme
        // https://developer.android.com/develop/ui/views/theming/darktheme#change-themes
        if (Build.VERSION.SDK_INT >= 31) uiModeManager.setApplicationNightMode(theme.uiManagerMode)
        else AppCompatDelegate.setDefaultNightMode(theme.appCompatMode)
        prefHelper.setThemeValue(theme.appCompatMode)
    }

    private fun onDateFormatSelected(dateFormatIndex: Int) {
        currentDateFormat.value = dateFormatIndex
        prefHelper.setDateTimeFormatIndex(dateFormatIndex)
    }

    private fun checkDisableLuaEngine() {
        // Check if the activity was launched with the custom action
        if (intent != null && intent.action == IntentActions.DISABLE_LUA_ENGINE) {
            // Disable the Lua engine
            luaEngineSettingsProvider.settings = luaEngineSettingsProvider.settings.copy(
                enabled = false
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) = deepLinkHandler.handleUri(uri.toString())
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val alarmInteractor: AlarmInteractor,
    private val timerServiceInteractor: TimerServiceInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    private var hasInitialized = false

    fun init() {
        if (hasInitialized) return
        hasInitialized = true

        syncAlarms()
        recoverTimerServiceIfNecessary()
    }

    private fun syncAlarms() {
        viewModelScope.launch(io) {
            alarmInteractor.syncAlarms()
        }
    }

    private fun recoverTimerServiceIfNecessary() {
        timerServiceInteractor.startTimerNotificationService()
    }
}
