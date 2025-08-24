package com.samco.trackandgraph.screenshots

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.samco.trackandgraph.main.MainActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Shared utilities for taking and saving screenshots during instrumented tests
 */
object ScreenshotUtils {
    
    fun takeDeviceScreenshot(
        composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
        uiDevice: UiDevice,
        name: String,
        subdirectory: String = "TrackAndGraphScreenshots"
    ) {
        composeRule.waitForIdle()

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ctx.contentResolver

        // UiDevice needs a File first
        val tmp = File(ctx.cacheDir, "$name.png")
        check(uiDevice.takeScreenshot(tmp) && tmp.length() > 0L) { "takeScreenshot failed: $name" }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$subdirectory")
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
}
