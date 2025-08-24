package com.samco.trackandgraph.promo

import com.samco.trackandgraph.screenshots.ScreenshotUtils
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.DataInteractorModule
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

    private val screenshotDir = "TrackAndGraphScreenshots"

    private val uiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun takeDeviceScreenshot(name: String) {
        ScreenshotUtils.takeDeviceScreenshot(composeRule, uiDevice, name)
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
        runBlocking { createScreenshotsGroup(dataInteractor) }
    }

    @Test
    fun capture_promo_screenshots() {
        // Wait for app to fully load and settle
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Screenshots").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        takeDeviceScreenshot("1")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Exercise")[0].performClick()
        composeRule.waitForIdle()
        //Wait a sec for the graphs to load
        //TODO we could probably do this in a more stable way by searching for loading indicators
        Thread.sleep(2000)
        takeDeviceScreenshot("2")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("trackAllFab", true).performClick()
        composeRule.waitForIdle()
        repeat(8) {
            composeRule.onNodeWithText("Skip").performClick()
            composeRule.waitForIdle()
        }
        takeDeviceScreenshot("3")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("trackAllFab", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hoursInput", true).performTextInput("8")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("addNoteChip", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notesInput", true).performTextInput("Get plenty of rest")
        composeRule.waitForIdle()
        takeDeviceScreenshot("4")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Rest day statistics").performClick()
        composeRule.waitForIdle()
        //Wait a sec for the graphs to load
        Thread.sleep(2000)
        takeDeviceScreenshot("5")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("groupGrid")
            .performScrollToNode(hasText("Track & Graph                              Groups list"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Track & Graph                              Groups list").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("groupGrid").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        takeDeviceScreenshot("6")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("burgerMenuButton", true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("remindersMenuItem", true).performClick()
        takeDeviceScreenshot("7")
        //Wait a sec for any files to flush to disk
        Thread.sleep(100)
    }
}
