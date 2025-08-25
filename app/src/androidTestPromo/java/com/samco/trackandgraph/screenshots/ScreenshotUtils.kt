package com.samco.trackandgraph.screenshots

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Shared utilities for taking and saving screenshots during instrumented tests
 */
object ScreenshotUtils {
    fun takeDeviceScreenshot(
        uiDevice: UiDevice,
        subdirectory: String,
        name: String,
    ) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ctx.contentResolver

        // UiDevice needs a File first (temp in app cache)
        val tmp = File(ctx.cacheDir, "$name.png")
        check(uiDevice.takeScreenshot(tmp) && tmp.length() > 0L) { "takeScreenshot failed: $name" }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$subdirectory"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")

        resolver.openFileDescriptor(uri, "w")!!.use { pfd ->
            FileInputStream(tmp).use { input ->
                FileOutputStream(pfd.fileDescriptor).use { output ->
                    input.copyTo(output)
                    input.fd.sync()
                    output.flush()
                    output.fd.sync()
                }
            }
        }
        tmp.delete()

        values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
