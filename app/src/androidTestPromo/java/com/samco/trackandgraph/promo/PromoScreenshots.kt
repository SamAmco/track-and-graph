package com.samco.trackandgraph.promo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.karumi.shot.ScreenshotTest
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
class PromoScreenshots : ScreenshotTest {

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
        Thread.sleep(1000)
        composeRule.onNodeWithText("Screenshots").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        compareScreenshot(composeRule, name = "1")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(5000)
        composeRule.onAllNodesWithText("Exercise")[0].performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        compareScreenshot(composeRule, name = "2")
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("trackAllFab", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        repeat(8) {
            composeRule.onNodeWithText("Skip").performClick()
            composeRule.waitForIdle()
        }
        Thread.sleep(1000)
        compareScreenshot(composeRule, name = "3")
        composeRule.onNodeWithText("cancel").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("trackAllFab", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("addNoteChip", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("notesInput", true).performTextInput("Get plenty of rest")
        composeRule.waitForIdle()
        Thread.sleep(1000)
        compareScreenshot(composeRule, name = "4")
        composeRule.onNodeWithText("cancel").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithText("Rest day statistics").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        compareScreenshot(composeRule, name = "5")
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("backButton", true).performClick()
        compareScreenshot(composeRule, name = "6")
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("backButton", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("burgerMenuButton", true).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
        composeRule.onNodeWithTag("remindersMenuItem", true).performClick()
        compareScreenshot(composeRule, name = "7")
    }
}
