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
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.threetenabp.AndroidThreeTen
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.sampling.DataSampler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DemoDBGenerator {

    private val database: TrackAndGraphDatabase = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        TrackAndGraphDatabase::class.java,
        "trackandgraph_database"
    ).build()

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var luaEngine: LuaEngine

    private val dataInteractor = TestDataInteractor.create(database)

    private lateinit var dataSampler: DataSampler

    @Before
    fun setup() {
        hiltRule.inject()
        dataSampler = TestDataSampler.create(
            luaEngine = luaEngine,
            dataInteractor = dataInteractor,
            database = database,
        )
        AndroidThreeTen.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun generateDemoDB(): Unit = runBlocking {
        println("creating screenshot data")
        createScreenshotsGroup(dataInteractor)
        println("creating faq1 data")
        createFaq1Group(dataInteractor)
        println("creating first open tutorial data")
        createFirstOpenTutorialGroup(dataInteractor, dataSampler)
        println("writing to a db file")
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