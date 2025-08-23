package com.samco.trackandgraph.screenshots

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Set device to portrait orientation for consistent screenshots
        device.setOrientationNatural()
        
        // Wait for the device to settle
        device.waitForIdle()
    }

    @Test
    fun capture_home_playstore() {
        // Wait for app to fully load
        Thread.sleep(3000)
        
        activityRule.scenario.onActivity { activity ->
            // Name these exactly how you want them copied to Fastlane
            compareScreenshot(activity = activity, name = "01_home")
        }
    }

    @Test
    fun capture_drawer_playstore() {
        // Wait for app to fully load
        Thread.sleep(3000)
        
        // Open navigation drawer
        device.swipe(0, device.displayHeight / 2, device.displayWidth / 3, device.displayHeight / 2, 10)
        Thread.sleep(1000)
        
        activityRule.scenario.onActivity { activity ->
            compareScreenshot(activity = activity, name = "02_drawer")
        }
    }

    // Add more @Test methods here for additional Play Store screenshots
    // e.g. "03_details", "04_settings", etc.
}
