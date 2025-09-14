package com.samco.trackandgraph

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.database.sampling.DataSamplerImpl
import com.samco.trackandgraph.data.model.CSVReadWriterImpl
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.DataInteractorImpl
import com.samco.trackandgraph.data.model.DataPointUpdateHelperImpl
import com.samco.trackandgraph.data.model.DatabaseTransactionHelperImpl
import com.samco.trackandgraph.data.model.FunctionHelperImpl
import com.samco.trackandgraph.data.model.TrackerHelperImpl
import kotlinx.coroutines.Dispatchers

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

        val functionHelper = FunctionHelperImpl(
            transactionHelper = transactionHelper,
            dao = database.trackAndGraphDatabaseDao,
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