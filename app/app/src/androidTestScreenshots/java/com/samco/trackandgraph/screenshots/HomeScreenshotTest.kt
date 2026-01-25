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

package com.samco.trackandgraph.screenshots

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.karumi.shot.ScreenshotTest
import com.samco.trackandgraph.di.PrefHelperModule
import com.samco.trackandgraph.helpers.PrefHelper
import com.samco.trackandgraph.main.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(PrefHelperModule::class)
class HomeScreenshotTest : ScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    // Mock PrefHelper to skip tutorial by returning that first run is complete
    @BindValue
    @JvmField
    val mockPrefHelper: PrefHelper = mock<PrefHelper>().apply {
        whenever(isFirstRun()).thenReturn(false)  // Tutorial already completed
        whenever(getHideDataPointTutorial()).thenReturn(false)
        whenever(getDateFormatValue()).thenReturn(0)
        whenever(getThemeValue(any())).thenReturn(0)
    }

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun home_screen_empty() {
        // Wait for app to fully load and settle
        composeRule.waitForIdle()
        
        // Since tutorial is marked as complete via mocked PrefHelper,
        // the app should go directly to the empty home screen
        compareScreenshot(composeRule, name = "home_screen_empty")
    }
}
