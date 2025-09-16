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

package com.samco.trackandgraph.helpers

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import androidx.core.content.edit
import androidx.core.net.toUri

interface PrefHelper {
    fun getHideDataPointTutorial(): Boolean

    fun isFirstRun(): Boolean

    fun setFirstRun(firstRun: Boolean)

    fun setDateTimeFormatIndex(formatIndex: Int)

    fun getDateFormatValue(): Int

    fun getThemeValue(defaultThemeValue: Int): Int

    fun setThemeValue(themeValue: Int)

    fun setHideDataPointTutorial(hide: Boolean)

    @Serializable
    enum class BackupConfigUnit {
        HOURS,
        DAYS,
        WEEKS
    }

    @Serializable
    data class BackupConfigData(
        @Serializable(with = UriSerializer::class)
        val uri: Uri,
        @Serializable(with = OffsetDateTimeSerializer::class)
        val firstDate: OffsetDateTime,
        val interval: Int,
        val units: BackupConfigUnit
    )

    fun setAutoBackupConfig(backupConfig: BackupConfigData?)

    fun getAutoBackupConfig(): BackupConfigData?

    fun setLastAutoBackupTime(time: OffsetDateTime)

    fun getLastAutoBackupTime(): OffsetDateTime?
}

const val THEME_SETTING_PREF_KEY = "theme_setting"
const val DATE_FORMAT_SETTING_PREF_KEY = "date_format_setting"
const val FIRST_RUN_PREF_KEY = "firstrun2"
const val HIDE_DATA_POINT_TUTORIAL_PREF_KEY = "HIDE_DATA_POINT_TUTORIAL_PREF_KEY"
const val AUTO_BACKUP_CONFIG_PREF_KEY = "auto_backup_config"
const val LAST_AUTO_BACKUP_TIME_PREF_KEY = "last_auto_backup_time"

fun getPrefs(context: Context, mode: Int = AppCompatActivity.MODE_PRIVATE): SharedPreferences {
    return context.getSharedPreferences("com.samco.trackandgraph", mode)
}

private object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: OffsetDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): OffsetDateTime = OffsetDateTime.parse(decoder.decodeString())
}

private object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Uri) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Uri = decoder.decodeString().toUri()
}

class PrefHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : PrefHelper {

    private val prefs get() = getPrefs(context)

    override fun getHideDataPointTutorial(): Boolean =
        prefs.getBoolean(HIDE_DATA_POINT_TUTORIAL_PREF_KEY, false)

    override fun isFirstRun(): Boolean = prefs.getBoolean(FIRST_RUN_PREF_KEY, true)

    override fun setFirstRun(firstRun: Boolean) {
        prefs.edit { putBoolean(FIRST_RUN_PREF_KEY, firstRun) }
    }

    override fun setDateTimeFormatIndex(formatIndex: Int) {
        prefs.edit { putInt(DATE_FORMAT_SETTING_PREF_KEY, formatIndex) }
    }

    override fun getDateFormatValue(): Int =
        prefs.getInt(DATE_FORMAT_SETTING_PREF_KEY, DateFormatSetting.DMY.ordinal)

    override fun getThemeValue(defaultThemeValue: Int): Int =
        prefs.getInt(THEME_SETTING_PREF_KEY, defaultThemeValue)

    override fun setThemeValue(themeValue: Int) {
        prefs.edit { putInt(THEME_SETTING_PREF_KEY, themeValue) }
    }

    override fun setHideDataPointTutorial(hide: Boolean) {
        prefs.edit { putBoolean(HIDE_DATA_POINT_TUTORIAL_PREF_KEY, hide) }
    }

    override fun setAutoBackupConfig(backupConfig: PrefHelper.BackupConfigData?) {
        if (backupConfig == null) {
            prefs.edit { remove(AUTO_BACKUP_CONFIG_PREF_KEY) }
        } else {
            prefs.edit {
                putString(
                    AUTO_BACKUP_CONFIG_PREF_KEY,
                    json.encodeToString(backupConfig)
                )
            }
        }
    }

    override fun getAutoBackupConfig(): PrefHelper.BackupConfigData? {
        return prefs.getString(AUTO_BACKUP_CONFIG_PREF_KEY, null)?.let {
            json.decodeFromString<PrefHelper.BackupConfigData>(it)
        }
    }

    override fun setLastAutoBackupTime(time: OffsetDateTime) {
        prefs.edit { putString(LAST_AUTO_BACKUP_TIME_PREF_KEY, time.toString()) }
    }

    override fun getLastAutoBackupTime(): OffsetDateTime? {
        return prefs.getString(LAST_AUTO_BACKUP_TIME_PREF_KEY, null)?.let {
            OffsetDateTime.parse(it)
        }
    }
}