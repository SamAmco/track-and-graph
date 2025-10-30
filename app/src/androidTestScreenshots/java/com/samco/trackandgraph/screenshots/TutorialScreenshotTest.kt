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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.karumi.shot.ScreenshotTest
import com.samco.trackandgraph.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TutorialScreenshotTest : ScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun tutorial_complete_flow() {
        // Wait for app to fully load and settle
        composeRule.waitForIdle()
        
        // Screenshot 1: First tutorial slide
        compareScreenshot(composeRule, name = "tutorial_slide_1")
        
        // Swipe left to go to slide 2
        composeRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()
        
        // Screenshot 2: Second tutorial slide
        compareScreenshot(composeRule, name = "tutorial_slide_2")
        
        // Swipe left to go to slide 3
        composeRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()
        
        // Screenshot 3: Third tutorial slide (with "Got it" button)
        compareScreenshot(composeRule, name = "tutorial_slide_3")
        
        // Find and click the "Got it" button
        composeRule.onNodeWithText("Got it!").performClick()
        composeRule.waitForIdle()
        
        // Screenshot 4: Empty home screen after tutorial completion
        compareScreenshot(composeRule, name = "tutorial_complete_home_screen_empty")
    }
}
