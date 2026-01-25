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

package com.samco.trackandgraph.tutorial

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.samco.trackandgraph.TestDataInteractor
import com.samco.trackandgraph.TestDataSampler
import com.samco.trackandgraph.TestDatabase
import com.samco.trackandgraph.createFirstOpenTutorialGroup
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.DataInteractorModule
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.di.PrefHelperModule
import com.samco.trackandgraph.helpers.PrefHelper
import com.samco.trackandgraph.main.MainActivity
import com.samco.trackandgraph.screenshots.ScreenshotUtils
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
import javax.inject.Inject

@LargeTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(DataInteractorModule::class, PrefHelperModule::class)
@HiltAndroidTest
class TutorialScreenshots {

    private val uiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun takeDeviceScreenshot(name: String) {
        ScreenshotUtils.takeDeviceScreenshot(uiDevice, "TutorialScreenshots", name)
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val database: TrackAndGraphDatabase = TestDatabase.create()

    @BindValue
    @JvmField
    val dataInteractor: DataInteractor = TestDataInteractor.create(database)

    @Inject
    lateinit var luaEngine: LuaEngine

    lateinit var dataSampler: DataSampler

    // Mock PrefHelper to enable tutorial by returning that first run is NOT complete
    @BindValue
    @JvmField
    val mockPrefHelper: PrefHelper = mock<PrefHelper>().apply {
        whenever(isFirstRun()).thenReturn(false)
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

        dataSampler = TestDataSampler.create(
            luaEngine = luaEngine,
            database = database,
            dataInteractor = dataInteractor,
        )
        runBlocking { createFirstOpenTutorialGroup(dataInteractor, dataSampler) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capture_tutorial_screenshots() {
        // Wait for app to fully load and tutorial to appear
        composeRule.waitForIdle()

        composeRule.onNodeWithText("First open tutorial").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("groupCard"))
        composeRule.onAllNodes(hasTestTag("groupCard"))[0].performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("trackerCard"))
        composeRule.waitForIdle()
        takeDeviceScreenshot("tutorial_1")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasTestTag("groupCard"))[1].performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("graphStatCard"))
        composeRule.waitForIdle()
        composeRule.waitUntilDoesNotExist(hasTestTag("loadingIndicator"))
        composeRule.waitForIdle()
        takeDeviceScreenshot("tutorial_2")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasTestTag("groupCard"))[2].performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("graphStatCard"))
        composeRule.waitForIdle()
        composeRule.waitUntilDoesNotExist(hasTestTag("loadingIndicator"))
        composeRule.waitForIdle()
        takeDeviceScreenshot("tutorial_3")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()

        //Wait a sec for any files to flush to disk
        Thread.sleep(100)
    }
}
