package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.DefaultDispatcher
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.functions.helpers.TimeHelper
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher

@Component
interface LineGraphDataFactoryTestComponent {

    fun provideTimeHelper(): TimeHelper

    fun provideLineGraphDataFactory(): LineGraphDataFactory

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun dataInteractor(dataInteractor: DataInteractor): Builder

        @BindsInstance
        fun ioDispatcher(@IODispatcher ioDispatcher: CoroutineDispatcher): Builder

        @BindsInstance
        fun defaultDispatcher(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): Builder

        @BindsInstance
        fun timeHelper(timeHelper: TimeHelper): Builder

        fun build(): LineGraphDataFactoryTestComponent
    }
}