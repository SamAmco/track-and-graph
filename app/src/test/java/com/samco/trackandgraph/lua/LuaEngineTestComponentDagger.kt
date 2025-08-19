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
package com.samco.trackandgraph.lua

import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.lua.apiimpl.DataPointParser
import com.samco.trackandgraph.lua.apiimpl.LuaDataSourceProviderImpl
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@DisableInstallInCheck
interface LuaBindingModule {
    @Binds
    fun bindLuaEngine(impl: LuaEngineImpl): LuaEngine
}

@Singleton
@Component(
    modules = [LuaBindingModule::class]
)
interface LuaEngineTestComponent {

    fun provideLuaEngine(): LuaEngineImpl

    fun provideGlobalsProvider(): GlobalsProvider
    
    fun provideLuaScriptResolver(): LuaScriptResolver

    fun provideDataPointParser(): DataPointParser

    fun provideLuaDataSourceProvider(): LuaDataSourceProviderImpl

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun dataInteractor(dataInteractor: DataInteractor): Builder

        @BindsInstance
        fun assetReader(assetReader: AssetReader): Builder

        @BindsInstance
        fun ioDispatcher(@IODispatcher ioDispatcher: CoroutineDispatcher): Builder

        fun build(): LuaEngineTestComponent
    }
}
