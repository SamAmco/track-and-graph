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

import com.samco.trackandgraph.graphstatview.factories.LuaGraphDataFactory
import com.samco.trackandgraph.base.model.DataInteractor
import dagger.Component
import dagger.Module
import dagger.Binds
import dagger.BindsInstance
import kotlinx.coroutines.CoroutineDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.assetreader.AssetReader
import dagger.hilt.migration.DisableInstallInCheck

@Module
@DisableInstallInCheck
interface LuaBindingModule {
    @Binds
    fun bindLuaEngine(impl: LuaEngineImpl): LuaEngine
}

@Component(
    modules = [LuaBindingModule::class]
)
interface LuaDataFactoryTestComponent {

    fun provideLuaDataFactory(): LuaGraphDataFactory

    fun provideLuaEngine(): LuaEngineImpl

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun dataInteractor(dataInteractor: DataInteractor): Builder

        @BindsInstance
        fun assetReader(assetReader: AssetReader): Builder

        @BindsInstance
        fun ioDispatcher(@IODispatcher ioDispatcher: CoroutineDispatcher): Builder

        fun build(): LuaDataFactoryTestComponent
    }
}
