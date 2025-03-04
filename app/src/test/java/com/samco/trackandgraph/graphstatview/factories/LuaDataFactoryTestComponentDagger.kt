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
package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.LuaEngineSettingsProvider
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object LuaEngineModule {
    @Provides
    fun provideLuaSettingsProvider(): LuaEngineSettingsProvider = LuaEngineSettingsProvider()
}

@Component(modules = [LuaEngineModule::class])
interface LuaDataFactoryTestComponent {

    fun provideLuaDataFactory(): LuaGraphDataFactory

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun luaEngine(luaEngine: LuaEngine): Builder

        @BindsInstance
        fun dataInteractor(dataInteractor: DataInteractor): Builder

        @BindsInstance
        fun ioDispatcher(@IODispatcher ioDispatcher: CoroutineDispatcher): Builder

        fun build(): LuaDataFactoryTestComponent
    }
}
