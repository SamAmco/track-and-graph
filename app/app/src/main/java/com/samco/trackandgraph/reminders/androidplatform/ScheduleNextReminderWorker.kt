/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders.androidplatform

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.reminders.ReminderInteractor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ScheduleNextReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor
) : CoroutineWorker(context, params) {

    companion object {
        const val REMINDER_ID_KEY = "REMINDER_ID_KEY"
    }

    override suspend fun doWork(): Result {
        try {
            val reminderId = inputData.getLong(REMINDER_ID_KEY, -1)
            if (reminderId == -1L) return Result.failure()

            val reminder = dataInteractor.getReminderById(reminderId)
                ?: return Result.failure()

            reminderInteractor.scheduleNext(reminder)
            return Result.success()
        } catch (t: Throwable) {
            Timber.e(t)
            return Result.failure()
        }
    }
}
