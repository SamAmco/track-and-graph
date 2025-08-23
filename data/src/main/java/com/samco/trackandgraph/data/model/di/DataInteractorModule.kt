package com.samco.trackandgraph.data.model.di

import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.DataInteractorImpl
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