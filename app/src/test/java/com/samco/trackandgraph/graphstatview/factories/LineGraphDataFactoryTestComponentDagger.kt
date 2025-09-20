package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
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