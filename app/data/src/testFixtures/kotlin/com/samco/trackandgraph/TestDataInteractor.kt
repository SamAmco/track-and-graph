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

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.data.database.DatabaseTransactionHelperImpl
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyserProvider
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataInteractorImpl
import com.samco.trackandgraph.data.interactor.DataPointUpdateHelperImpl
import com.samco.trackandgraph.data.interactor.FunctionHelperImpl
import com.samco.trackandgraph.data.interactor.GraphHelperImpl
import com.samco.trackandgraph.data.interactor.GroupHelperImpl
import com.samco.trackandgraph.data.interactor.ReminderHelperImpl
import com.samco.trackandgraph.data.interactor.TrackerHelperImpl
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.serialization.ReminderSerializer
import com.samco.trackandgraph.data.validation.FunctionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

object TestDataInteractor {

    fun create(
        database: TrackAndGraphDatabase =
            Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                TrackAndGraphDatabase::class.java,
                "trackandgraph_database"
            ).build()
    ): DataInteractor {
        database.openHelper.writableDatabase.execSQL(
            """
                INSERT OR REPLACE INTO 
                groups_table(id, name, display_index, parent_group_id, color_index) 
                VALUES(0, '', 0, NULL, 0)
            """.trimMargin()
        )
        val transactionHelper = DatabaseTransactionHelperImpl(
            database = database
        )
        val trackerHelper = TrackerHelperImpl(
            transactionHelper = DatabaseTransactionHelperImpl(
                database = database
            ),
            dao = database.trackAndGraphDatabaseDao,
            dataPointUpdateHelper = DataPointUpdateHelperImpl(),
            io = Dispatchers.IO
        )

        val testJson = Json {
            ignoreUnknownKeys = false
            isLenient = false
        }
        val functionGraphSerializer = FunctionGraphSerializer(testJson)

        val dependencyAnalyserProvier = DependencyAnalyserProvider(
            dao = database.trackAndGraphDatabaseDao,
        )

        val functionValidator = FunctionValidator(
            dependencyAnalyserProvider = dependencyAnalyserProvier,
            dao = database.trackAndGraphDatabaseDao,
        )

        val functionHelper = FunctionHelperImpl(
            transactionHelper = transactionHelper,
            dao = database.trackAndGraphDatabaseDao,
            functionGraphSerializer = functionGraphSerializer,
            functionValidator = functionValidator,
            io = Dispatchers.IO
        )

        val reminderHelper = ReminderHelperImpl(
            reminderDao = database.trackAndGraphDatabaseDao,
            reminderSerializer = ReminderSerializer(testJson),
            io = Dispatchers.IO
        )

        val groupHelper = GroupHelperImpl(
            dao = database.trackAndGraphDatabaseDao,
            io = Dispatchers.IO
        )

        val graphHelper = GraphHelperImpl(
            transactionHelper = transactionHelper,
            graphDao = database.trackAndGraphDatabaseDao,
            io = Dispatchers.IO
        )

        val dataInteractor = DataInteractorImpl(
            transactionHelper = transactionHelper,
            dao = database.trackAndGraphDatabaseDao,
            io = Dispatchers.IO,
            trackerHelper = trackerHelper,
            functionHelper = functionHelper,
            reminderHelper = reminderHelper,
            groupHelper = groupHelper,
            graphHelper = graphHelper,
            dependencyAnalyserProvider = dependencyAnalyserProvier,
        )

        return dataInteractor
    }
}