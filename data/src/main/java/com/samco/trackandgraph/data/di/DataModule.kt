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

package com.samco.trackandgraph.data.di

import android.content.Context
import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.assetreader.AssetReaderImpl
import com.samco.trackandgraph.data.csvreadwriter.CSVReadWriter
import com.samco.trackandgraph.data.csvreadwriter.CSVReadWriterImpl
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.DatabaseTransactionHelperImpl
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.interactor.DataPointUpdateHelper
import com.samco.trackandgraph.data.interactor.DataPointUpdateHelperImpl
import com.samco.trackandgraph.data.interactor.FunctionHelper
import com.samco.trackandgraph.data.interactor.FunctionHelperImpl
import com.samco.trackandgraph.data.interactor.TrackerHelper
import com.samco.trackandgraph.data.interactor.TrackerHelperImpl
import com.samco.trackandgraph.data.lua.GuardedLuaEngineWrapper
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaEngineImpl
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.sampling.DataSamplerImpl
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.data.time.TimeProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @Singleton
    internal fun getDatabase(@ApplicationContext context: Context): TrackAndGraphDatabase =
        TrackAndGraphDatabase.getInstance(context)

    @Provides
    internal fun getDao(database: TrackAndGraphDatabase): TrackAndGraphDatabaseDao =
        database.trackAndGraphDatabaseDao

    @Provides
    internal fun getCSVReadWriter(impl: CSVReadWriterImpl): CSVReadWriter = impl

    @Provides
    internal fun getTrackerUpdater(impl: TrackerHelperImpl): TrackerHelper = impl

    @Provides
    internal fun getDataSampler(impl: DataSamplerImpl): DataSampler = impl

    @Provides
    internal fun getDataPointUpdateHelper(impl: DataPointUpdateHelperImpl): DataPointUpdateHelper =
        impl

    @Provides
    internal fun getDatabaseTransactionHelper(impl: DatabaseTransactionHelperImpl): DatabaseTransactionHelper =
        impl

    @Provides
    @Singleton
    internal fun getFunctionHelper(impl: FunctionHelperImpl): FunctionHelper = impl

    @Provides
    internal fun getLuaEngine(impl: GuardedLuaEngineWrapper): LuaEngine = impl

    @Provides
    fun provideTimeProvider(impl: TimeProviderImpl): TimeProvider = impl

    @Provides
    fun getAssetReader(impl: AssetReaderImpl): AssetReader = impl
}