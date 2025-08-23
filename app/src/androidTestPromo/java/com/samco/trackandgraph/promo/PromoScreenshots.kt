package com.samco.trackandgraph.promo

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
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
import java.io.File
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
import java.io.FileInputStream
import java.io.FileOutputStream

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
        composeRule.waitForIdle()

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ctx.contentResolver

        // UiDevice needs a File first
        val tmp = File(ctx.cacheDir, "$name.png")
        check(uiDevice.takeScreenshot(tmp) && tmp.length() > 0L) { "takeScreenshot failed: $name" }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TrackAndGraphScreenshots")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!

        // Write + hard flush
        resolver.openFileDescriptor(uri, "w")!!.use { pfd ->
            FileInputStream(tmp).use { input ->
                FileOutputStream(pfd.fileDescriptor).use { output ->
                    input.copyTo(output)
                    output.fd.sync() // <-- ensure bytes are on disk
                }
            }
        }
        tmp.delete()

        // Publish
        values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        // Optional: sanity check size > 0
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && c.getLong(0) <= 0L) {
                throw AssertionError("Saved screenshot has zero size: $uri")
            }
        }
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
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ctx.contentResolver
        resolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.Downloads.RELATIVE_PATH}=?",
            arrayOf("${Environment.DIRECTORY_DOWNLOADS}/$screenshotDir/")
        )
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
