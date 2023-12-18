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
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DemoDBGenerator {

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
        createOuterGroups()
        createDailyGroup()

        writeDBToFile()
    }

    private suspend fun createDailyGroup() {
        val dailyGroupId = dataInteractor.insertGroup(
            createGroup(
                name = "Daily",
                displayIndex = 0,
                colorIndex = 1
            )
        )

        val sleepTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Sleep",
                groupId = dailyGroupId,
                dataType = DataType.DURATION,
                displayIndex = 0
            )
        )

        val productivityTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Productivity",
                groupId = dailyGroupId,
                displayIndex = 1
            )
        )

        val alcoholTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Alcohol",
                groupId = dailyGroupId,
                displayIndex = 2
            )
        )

        val meditationTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Meditation",
                groupId = dailyGroupId,
                dataType = DataType.DURATION,
                displayIndex = 3
            )
        )

        val workTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Work",
                groupId = dailyGroupId,
                dataType = DataType.DURATION,
                displayIndex = 4
            )
        )

        val weightTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Weight",
                groupId = dailyGroupId,
                displayIndex = 5
            )
        )

        val exerciseTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Exercise",
                groupId = dailyGroupId,
                hasDefaultValue = true,
                defaultValue = 1.0,
                displayIndex = 6
            )
        )

        val studyingTracker = dataInteractor.insertTracker(
            createTracker(
                name = "Studying",
                groupId = dailyGroupId,
                dataType = DataType.DURATION,
                displayIndex = 7
            )
        )
    }

    private suspend fun createOuterGroups() {
        dataInteractor.insertGroup(
            createGroup(
                name = "Meal time tracking",
                displayIndex = 1,
                colorIndex = 11
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Morning tracking",
                displayIndex = 2,
                colorIndex = 6
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Daily tracking",
                displayIndex = 3,
                colorIndex = 0
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Weekly tracking",
                displayIndex = 4,
                colorIndex = 2
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Exercise routine tracking",
                displayIndex = 5,
                colorIndex = 8
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Weight loss graphs",
                displayIndex = 6,
                colorIndex = 7
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Mood quality",
                displayIndex = 7,
                colorIndex = 3
            )
        )
        dataInteractor.insertGroup(
            createGroup(
                name = "Stress and rest statistics",
                displayIndex = 8,
                colorIndex = 4
            )
        )
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


    private fun createGroup(
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

    private fun createTracker(
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
}
