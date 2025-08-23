package com.samco.trackandgraph.screenshots

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class PlayStoreCaptureTest : ScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun capture_home_playstore() {
        // Wait for app to fully load and settle
        composeRule.waitForIdle()
        
        // Name these exactly how you want them copied to Fastlane
        compareScreenshot(composeRule, name = "01_home")
    }
}
