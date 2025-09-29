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

package com.samco.trackandgraph.di

import android.content.ContentResolver
import android.content.Context
import com.samco.trackandgraph.BuildConfig
import com.samco.trackandgraph.adddatapoint.SuggestedValueHelper
import com.samco.trackandgraph.adddatapoint.SuggestedValueHelperImpl
import com.samco.trackandgraph.backupandrestore.BackupRestoreInteractor
import com.samco.trackandgraph.backupandrestore.BackupRestoreInteractorImpl
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandler
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandlerImpl
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.downloader.FileDownloaderImpl
import com.samco.trackandgraph.graphstatview.functions.aggregation.GlobalAggregationPreferences
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import com.samco.trackandgraph.navigation.PendingIntentProvider
import com.samco.trackandgraph.navigation.PendingIntentProviderImpl
import com.samco.trackandgraph.reminders.AlarmInteractor
import com.samco.trackandgraph.reminders.RemindersHelper
import com.samco.trackandgraph.reminders.RemindersHelperImpl
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.remoteconfig.RemoteConfigProviderImpl
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.remoteconfig.UrlNavigatorImpl
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.settings.TngSettingsImpl
import com.samco.trackandgraph.system.AlarmManagerWrapper
import com.samco.trackandgraph.system.AlarmManagerWrapperImpl
import com.samco.trackandgraph.system.ReminderPrefWrapper
import com.samco.trackandgraph.system.ReminderPrefWrapperImpl
import com.samco.trackandgraph.system.SystemInfoProvider
import com.samco.trackandgraph.system.SystemInfoProviderImpl
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.timers.TimerServiceInteractorImpl
import com.samco.trackandgraph.functions.repository.FunctionsRepository
import com.samco.trackandgraph.functions.repository.FunctionsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun getContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun getPendingIntentProvider(impl: PendingIntentProviderImpl): PendingIntentProvider = impl

    @Provides
    fun getSuggestedValueHelper(impl: SuggestedValueHelperImpl): SuggestedValueHelper = impl

    @Provides
    @Singleton
    fun getTngSettings(impl: TngSettingsImpl): TngSettings = impl

    @Provides
    fun getTimerHelper(): TimeHelper = TimeHelper(GlobalAggregationPreferences)

    @Provides
    @Singleton
    //Must be singleton because it is a dependency of work manager worker
    fun getBackupRestoreInteractor(impl: BackupRestoreInteractorImpl): BackupRestoreInteractor = impl

    @Provides
    @Singleton
    fun provideDeepLinkHandler(impl: DeepLinkHandlerImpl): DeepLinkHandler = impl

    @Provides
    fun provideFileDownloader(impl: FileDownloaderImpl): FileDownloader = impl

    @Provides
    @Singleton
    fun provideUrlNavigator(impl: UrlNavigatorImpl): UrlNavigator = impl

    @Provides
    @Singleton
    fun provideRemoteConfigProvider(impl: RemoteConfigProviderImpl): RemoteConfigProvider = impl

    @Provides
    @Singleton
    fun provideFunctionsRepository(impl: FunctionsRepositoryImpl): FunctionsRepository = impl

    @Provides
    internal fun getRemindersHelper(impl: RemindersHelperImpl): RemindersHelper = impl

    @Provides
    internal fun getAlarmInteractor(impl: RemindersHelper): AlarmInteractor = impl

    @Provides
    internal fun getServiceManager(impl: TimerServiceInteractorImpl): TimerServiceInteractor = impl

    @Singleton
    @Provides
    internal fun getReminderPref(impl: ReminderPrefWrapperImpl): ReminderPrefWrapper = impl

    @Provides
    @Singleton
    internal fun alarmManager(impl: AlarmManagerWrapperImpl): AlarmManagerWrapper = impl

    @Provides
    fun systemInfoProvider(impl: SystemInfoProviderImpl): SystemInfoProvider = impl

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = !BuildConfig.DEBUG
        isLenient = !BuildConfig.DEBUG
    }
}
