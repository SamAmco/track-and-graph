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

import kotlinx.coroutines.sync.Mutex
import org.luaj.vm2.Globals

/**
 * Test fixture factory functions for creating LuaVMLock instances in tests.
 * Since LuaVMLock is a sealed interface, we need to create instances from within
 * the data module where the implementations are defined.
 */
object TestLuaVMFixtures {
    
    /**
     * Creates a test LuaVMLock instance for use in unit tests.
     * This creates a minimal VMLease with empty globals and an unlocked mutex.
     */
    fun createTestLuaVMLock(): LuaVMLock {
        return VMLease(
            globals = Globals(),
            lock = Mutex(),
            name = "TestVM"
        )
    }
}
