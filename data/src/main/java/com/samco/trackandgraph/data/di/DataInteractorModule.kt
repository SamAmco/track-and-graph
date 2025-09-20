package com.samco.trackandgraph.data.di

import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataInteractorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataInteractorModule {
    @Provides
    @Singleton
    internal fun getDataInteractor(impl: DataInteractorImpl): DataInteractor = impl
}