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

package com.samco.trackandgraph.promo

import android.app.Activity
import android.app.UiModeManager
import android.content.res.Configuration
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import com.samco.trackandgraph.screenshots.ScreenshotUtils
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.samco.trackandgraph.TestDataInteractor
import com.samco.trackandgraph.createScreenshotsGroup
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.DataInteractorModule
import com.samco.trackandgraph.di.PrefHelperModule
import com.samco.trackandgraph.helpers.PrefHelper
import com.samco.trackandgraph.main.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@LargeTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(DataInteractorModule::class, PrefHelperModule::class)
@HiltAndroidTest
class PromoScreenshots {

    private val uiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun takeDeviceScreenshot(name: String) {
        ScreenshotUtils.takeDeviceScreenshot(uiDevice, "TrackAndGraphScreenshots", name)
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val dataInteractor: DataInteractor = TestDataInteractor.create()

    // Mock PrefHelper to skip tutorial by returning that first run is complete
    @BindValue
    @JvmField
    val mockPrefHelper: PrefHelper = mock<PrefHelper>().apply {
        whenever(isFirstRun()).thenReturn(false)  // Tutorial already completed
        whenever(getHideDataPointTutorial()).thenReturn(false)
        whenever(getDateFormatValue()).thenReturn(0)
        whenever(getThemeValue(any())).thenReturn(0)
    }

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capture_promo_screenshots() {
        // Create demo data AFTER activity has launched
        runBlocking { createScreenshotsGroup(dataInteractor) }

        // Wait for app to fully load and settle
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Screenshots"))
        composeRule.onNodeWithText("Screenshots").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Daily"))
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Sleep"))
        takeDeviceScreenshot("1")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        // Wait for group children to load by checking for Daily
        composeRule.waitUntilAtLeastOneExists(hasText("Daily"))
        composeRule.onNodeWithTag("groupGrid")
            .performScrollToNode(hasText("Exercise").and(hasTestTag("groupCard")))
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Exercise").and(hasTestTag("groupCard"))).performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Exercise weekly totals in the last 6 months"))
        takeDeviceScreenshot("2")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("trackAllFab"))
        composeRule.onNodeWithTag("trackAllFab", true).performClick()
        composeRule.waitForIdle()
        repeat(8) {
            composeRule.onNodeWithText("Skip").performClick()
            composeRule.waitForIdle()
        }
        takeDeviceScreenshot("3")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            whenever(mockPrefHelper.getThemeValue(any())).thenReturn(AppCompatDelegate.MODE_NIGHT_YES)
            activity.getSystemService(UiModeManager::class.java)
                .setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        }
        // Give time for activity recreation and WorkManager to settle after theme change
        Thread.sleep(1000)
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("darkTheme"), 3000)
        takeDeviceScreenshot("4")
        composeRule.activityRule.scenario.onActivity { activity ->
            whenever(mockPrefHelper.getThemeValue(any())).thenReturn(AppCompatDelegate.MODE_NIGHT_NO)
            activity.getSystemService(UiModeManager::class.java)
                .setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
        }
        // Give time for activity recreation after theme change
        Thread.sleep(500)
        composeRule.waitUntilAtLeastOneExists(hasTestTag("lightTheme"), 3000)
        composeRule.waitForIdle()
        // Wait for UI to fully stabilize after theme change - Sleep tracker is in Daily group
        composeRule.waitUntilAtLeastOneExists(hasText("Sleep"), 3000)
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        // Wait for Screenshots group to load after navigating back
        composeRule.waitUntilAtLeastOneExists(hasText("Rest day statistics"), 3000)
        composeRule.onNodeWithText("Rest day statistics").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Stress pie chart"))
        composeRule.waitForIdle()
        composeRule.waitUntilDoesNotExist(hasTestTag("loadingIndicator"))
        takeDeviceScreenshot("5")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("groupGrid")
            .performScrollToNode(hasText("Track & Graph                              Groups list"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track & Graph                              Groups list")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("groupGrid").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        takeDeviceScreenshot("6")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("burgerMenuButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("remindersMenuItem", true).performClick()
        composeRule.waitForIdle()
        takeDeviceScreenshot("7")
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Screenshots"))
        composeRule.onNodeWithText("Screenshots").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilDoesNotExist(hasTestTag("loadingIndicator"))
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Functions"))
        composeRule.onNodeWithText("Functions").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilDoesNotExist(hasTestTag("loadingIndicator"))
        composeRule.waitForIdle()

        val exercise = "Exercise"
        composeRule.onNodeWithTag("groupGrid").performScrollToNode(hasText(exercise))
        composeRule.waitForIdle()
        composeRule.onNode(hasTestTag("functionMenuButton")).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Edit"))
        composeRule.onNodeWithText("Edit").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Output"))
        composeRule.waitUntilAtLeastOneExists(hasText("Data Source"))
        composeRule.waitForIdle()
        takeDeviceScreenshot("8")
        //Wait a sec for any files to flush to disk
    }
}
