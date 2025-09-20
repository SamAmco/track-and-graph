package com.samco.trackandgraph

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.data.csvreadwriter.CSVReadWriterImpl
import com.samco.trackandgraph.data.database.DatabaseTransactionHelperImpl
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataInteractorImpl
import com.samco.trackandgraph.data.interactor.DataPointUpdateHelperImpl
import com.samco.trackandgraph.data.interactor.FunctionHelperImpl
import com.samco.trackandgraph.data.interactor.TrackerHelperImpl
import com.samco.trackandgraph.data.sampling.DataSamplerImpl
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
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

        val functionHelper = FunctionHelperImpl(
            transactionHelper = transactionHelper,
            dao = database.trackAndGraphDatabaseDao,
            functionGraphSerializer = functionGraphSerializer,
            io = Dispatchers.IO
        )

        return DataInteractorImpl(
            database = database,
            dao = database.trackAndGraphDatabaseDao,
            io = Dispatchers.IO,
            trackerHelper = trackerHelper,
            functionHelper = functionHelper,
            csvReadWriter = CSVReadWriterImpl(
                dao = database.trackAndGraphDatabaseDao,
                trackerHelper = trackerHelper,
                io = Dispatchers.IO
            ),
            dataSampler = DataSamplerImpl(dao = database.trackAndGraphDatabaseDao)
        )
    }
}