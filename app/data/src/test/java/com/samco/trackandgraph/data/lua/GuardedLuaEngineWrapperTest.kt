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
package com.samco.trackandgraph.data.lua

import kotlinx.coroutines.runBlocking
import org.junit.Test

internal class GuardedLuaEngineWrapperTest : LuaEngineImplTest() {

    @Test
    fun `releaseVM releases acquired lock even when lua is disabled`() = runBlocking {
        val luaEngineSwitch = LuaEngineSwitch()
        val wrapper = GuardedLuaEngineWrapper(uut(), luaEngineSwitch)

        val lock = wrapper.acquireVM()
        luaEngineSwitch.enabled = false

        wrapper.releaseVM(lock)

        luaEngineSwitch.enabled = true
        val reacquiredLock = wrapper.acquireVM()
        wrapper.releaseVM(reacquiredLock)
    }
}
