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

package com.samco.trackandgraph

import android.os.Environment
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.threetenabp.AndroidThreeTen
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.base.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.base.database.sampling.DataSamplerImpl
import com.samco.trackandgraph.base.model.CSVReadWriterImpl
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.DataInteractorImpl
import com.samco.trackandgraph.base.model.DataPointUpdateHelperImpl
import com.samco.trackandgraph.base.model.DatabaseTransactionHelperImpl
import com.samco.trackandgraph.base.model.RemindersHelperImpl
import com.samco.trackandgraph.base.model.TrackerHelperImpl
import com.samco.trackandgraph.base.service.ServiceManager
import com.samco.trackandgraph.base.system.AlarmManagerWrapperImpl
import com.samco.trackandgraph.base.system.ReminderPrefWrapperImpl
import com.samco.trackandgraph.base.system.SystemInfoProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DemoDBGenerator {

    companion object {
    }

    private val mockServiceManager = object : ServiceManager {
        override fun startTimerNotificationService() {
            //Do nothing
        }

        override fun requestWidgetUpdatesForFeatureId(featureId: Long) {
            //Do nothing
        }

        override fun requestWidgetsDisabledForFeatureId(featureId: Long) {
            //Do nothing
        }
    }

    private val database = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        TrackAndGraphDatabase::class.java,
        "trackandgraph_database"
    ).build()

    private val trackerHelper = TrackerHelperImpl(
        transactionHelper = DatabaseTransactionHelperImpl(
            database = database
        ),
        dao = database.trackAndGraphDatabaseDao,
        dataPointUpdateHelper = DataPointUpdateHelperImpl(),
        io = Dispatchers.IO
    )

    private val dataInteractor: DataInteractor = DataInteractorImpl(
        database = database,
        dao = database.trackAndGraphDatabaseDao,
        io = Dispatchers.IO,
        trackerHelper = trackerHelper,
        csvReadWriter = CSVReadWriterImpl(
            dao = database.trackAndGraphDatabaseDao,
            trackerHelper = trackerHelper,
            io = Dispatchers.IO
        ),
        alarmInteractor = RemindersHelperImpl(
            reminderPref = ReminderPrefWrapperImpl(
                context = InstrumentationRegistry.getInstrumentation().targetContext
            ),
            alarmManager = AlarmManagerWrapperImpl(
                context = InstrumentationRegistry.getInstrumentation().targetContext
            ),
            systemInfoProvider = SystemInfoProviderImpl(),
            dao = database.trackAndGraphDatabaseDao,
            io = Dispatchers.IO
        ),
        serviceManager = mockServiceManager,
        dataSampler = DataSamplerImpl(dao = database.trackAndGraphDatabaseDao)
    )

    @Before
    fun setup() {
        AndroidThreeTen.init(InstrumentationRegistry.getInstrumentation().targetContext)
        database.openHelper.writableDatabase.execSQL(
            """
                INSERT OR REPLACE INTO 
                groups_table(id, name, display_index, parent_group_id, color_index) 
                VALUES(0, '', 0, NULL, 0)
            """.trimMargin()
        )
    }

    @Test
    fun generateDemoDB(): Unit = runBlocking {
        createScreenshotsGroup(dataInteractor)
        createFaq1Group(dataInteractor)
        writeDBToFile()
    }


    private fun writeDBToFile() {
        database.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(full)"))
            .apply {
                moveToFirst()
                if (getInt(0) != 0) throw Exception("WAL checkpoint failed")
                close()
            }

        val dbFile = database.openHelper.writableDatabase.path?.let { File(it) } ?: throw Exception(
            "Could not get DB file"
        )
        val outFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "tng_demo_database.db"
        )

        println("DB file path: ${outFile.absolutePath}")


        dbFile.copyTo(outFile, overwrite = true)
    }

}

const val SEC_PER_HOUR = 60.0 * 60.0

class SinTransform(
    val amplitude: Double,
    val wavelength: Double,
    val yOffset: Double = 0.0,
    val xOffset: Double = 0.0
) {
    fun transform(index: Int): Double {
        val xPos = index.toDouble() + xOffset
        val sinTransform = sin((xPos / wavelength) * Math.PI * 2.0)
        return (((sinTransform + 1.0) / 2.0) * amplitude) + yOffset
    }
}

suspend fun createWaveData(
    dataInteractor: DataInteractor,
    trackerId: Long,
    sinTransform: SinTransform = SinTransform(10.0, 3.0),
    randomSeed: Int = 0,
    randomOffsetScalar: Double = 5.0,
    numDataPoints: Int = 1000,
    spacing: Duration = Duration.ofDays(1),
    spacingRandomisationHours: Int = 6,
    endPoint: OffsetDateTime = OffsetDateTime.now(),
    roundToInt: Boolean = false,
    clampMin: Double? = null,
    clampMax: Double? = null,
    labels: List<String> = emptyList(),
) {
    val tracker =
        dataInteractor.getTrackerById(trackerId) ?: throw Exception("Tracker not found")
    val featureId = tracker.featureId

    val random = Random(randomSeed)

    for (i in 0 until numDataPoints) {
        val sin = sinTransform.transform(i)
        val randAdjusted = sin + (random.nextDouble() * randomOffsetScalar)
        val rounded = if (roundToInt) randAdjusted.roundToInt().toDouble() else randAdjusted
        val clamped = clamp(
            value = rounded,
            min = clampMin ?: Double.MIN_VALUE,
            max = clampMax ?: Double.MAX_VALUE
        )

        val randDuration = Duration.ofHours(
            random.nextLong(spacingRandomisationHours * 2L) - spacingRandomisationHours
        )

        val time = endPoint - spacing.multipliedBy(i.toLong()) - randDuration

        val labelIndex = labels.getOrNull(clamped.roundToInt()) ?: ""

        val dataPoint = createDataPoint(
            timestamp = time,
            featureId = featureId,
            value = clamped,
            label = labelIndex
        )
        dataInteractor.insertDataPoint(dataPoint)
    }
}

fun clamp(value: Double, min: Double, max: Double): Double {
    if (value < min) {
        return min
    } else if (value > max) {
        return max
    }
    return value
}

fun createGroup(
    name: String = "",
    displayIndex: Int = 0,
    parentGroupId: Long? = null,
    colorIndex: Int = 0,
) = Group(
    id = 0L,
    name = name,
    displayIndex = displayIndex,
    parentGroupId = parentGroupId ?: 0L,
    colorIndex = colorIndex
)

fun createTracker(
    name: String,
    groupId: Long,
    displayIndex: Int = 0,
    description: String = "",
    dataType: DataType = DataType.CONTINUOUS,
    hasDefaultValue: Boolean = false,
    defaultValue: Double = 0.0,
    defaultLabel: String = "",
    suggestionType: TrackerSuggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
    suggestionOrder: TrackerSuggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
) = Tracker(
    id = 0L,
    name = name,
    groupId = groupId,
    featureId = 0L,
    displayIndex = displayIndex,
    description = description,
    dataType = dataType,
    hasDefaultValue = hasDefaultValue,
    defaultValue = defaultValue,
    defaultLabel = defaultLabel,
    suggestionType = suggestionType,
    suggestionOrder = suggestionOrder
)

fun createDataPoint(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    featureId: Long,
    value: Double,
    label: String = "",
    note: String = ""
) = DataPoint(
    timestamp = timestamp,
    featureId = featureId,
    value = value,
    label = label,
    note = note
)
