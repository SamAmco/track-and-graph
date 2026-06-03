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
package com.samco.trackandgraph.applock

import android.os.SystemClock
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class AppLockSession @Inject constructor(
    private val repository: AppLockRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Job() + ioDispatcher

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    @Volatile
    private var currentConfig: AppLockConfig? = null

    @Volatile
    private var backgroundedAtMillis: Long? = null

    init {
        launch {
            repository.config.collect { config ->
                currentConfig = config
                if (!config.enabled) {
                    _unlocked.value = true
                    backgroundedAtMillis = null
                }
            }
        }
    }

    fun unlock() {
        _unlocked.value = true
        backgroundedAtMillis = null
    }

    fun lock() {
        if (currentConfig?.enabled == true) _unlocked.value = false
    }

    fun onAppBackgrounded() {
        if (currentConfig?.enabled != true) return
        backgroundedAtMillis = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded() {
        val config = currentConfig ?: return
        if (!config.enabled) {
            _unlocked.value = true
            backgroundedAtMillis = null
            return
        }

        val backgroundedAt = backgroundedAtMillis ?: return
        val lockAfterMillis = config.lockAfterMinutes * 60_000L
        if (SystemClock.elapsedRealtime() - backgroundedAt >= lockAfterMillis) {
            _unlocked.value = false
        }
        backgroundedAtMillis = null
    }
}
