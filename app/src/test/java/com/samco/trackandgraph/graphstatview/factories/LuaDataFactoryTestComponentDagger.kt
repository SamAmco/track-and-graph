package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.lua.LuaEngine
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher

@Component
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