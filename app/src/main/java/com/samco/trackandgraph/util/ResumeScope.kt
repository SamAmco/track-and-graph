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
package com.samco.trackandgraph.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun Fragment.resumeScoped(
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

            override fun onResume(owner: LifecycleOwner) {
                scope.launch { block() }
            }

            override fun onPause(owner: LifecycleOwner) {
                scope.cancel()
                lifecycle.removeObserver(this)
            }
        }
    )
}