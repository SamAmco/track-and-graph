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
package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.lua.LuaEngineImplTest
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

internal class RandomApiTests : LuaEngineImplTest() {

    @Test
    fun `Seeded random api gives reproducible results`() = testLuaFunction(
        // The seed here is of the form of an epoch milli, it's important we can handle
        // large numbers like this.
        script = """
            return {
                generator = function(source)
                    local random = require("tng.random").new_seeded_random(1761740202980, 1761740202981)
                    local remaining = 5
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        return {
                            timestamp = 0,
                            value = random:next(0, 1),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(5, resultList.size)
        assertEquals(0.5292621362673132, resultList[0].value)
        assertEquals(0.6310874068439589, resultList[1].value)
        assertEquals(0.09354638863957832, resultList[2].value)
        assertEquals(0.901839810325034, resultList[3].value)
        assertEquals(0.6505773193666343, resultList[4].value)
    }

    @Test
    fun `Single seed is still reproducible`() = testLuaFunction(
        script = """
            return {
                generator = function(source)
                    local random = require("tng.random").new_seeded_random(1234)
                    local remaining = 3
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        return {
                            timestamp = 0,
                            value = random:next(0, 1),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(0.1260932118007526, resultList[0].value)
        assertEquals(0.7618759742709109, resultList[1].value)
        assertEquals(0.7395846345175963, resultList[2].value)
    }

    @Test
    fun `The same seed used multiple times gives the same number`() = testLuaFunction(
        script = """
            return {
                generator = function(source)
                    local remaining = 3
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        local random = require("tng.random").new_seeded_random(1234)
                        return {
                            timestamp = 0,
                            value = random:next(0, 1),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(0.1260932118007526, resultList[0].value)
        assertEquals(0.1260932118007526, resultList[1].value)
        assertEquals(0.1260932118007526, resultList[2].value)
    }

    @Test
    fun `Unseeded random produces random results still`() = testLuaFunction(
        script = """
            return {
                generator = function(source)
                    local remaining = 1000
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        local random = require("tng.random").new_seeded_random()
                        return {
                            timestamp = 0,
                            value = random:next(0, 1),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(1000, resultList.size)
        val seen = mutableSetOf<String>()
        for (number in result.map { it.value }) {
            if (number.toString() in seen) fail("Unseeded random produced the same number twice")
            assertTrue(
                "Unseeded random number was not in its bounds",
                number >= 0.0 && number < 1.0
            )
        }
    }

    @Test
    fun `Test large bounds`() = testLuaFunction(
        script = """
            return {
                generator = function(source)
                    local random = require("tng.random").new_seeded_random(1234, 5678)
                    local remaining = 3
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        return {
                            timestamp = 0,
                            value = random:next(1234, 12351),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(11059.893675979674, resultList[0].value)
        assertEquals(4564.985862517691, resultList[1].value)
        assertEquals(4612.415851191359, resultList[2].value)
    }


    @Test
    fun `Test negative bounds`() = testLuaFunction(
        script = """
            return {
                generator = function(source)
                    local random = require("tng.random").new_seeded_random(-1234, 5678)
                    local remaining = 3
                    
                    return function()
                        if remaining == 0 then
                            return nil
                        end
                        
                        remaining = remaining - 1
                        return {
                            timestamp = 0,
                            value = random:next(-8888, -24),
                        }
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(-8230.532464333, resultList[0].value)
        assertEquals(-5775.09452447433, resultList[1].value)
        assertEquals(-6767.9092199905335, resultList[2].value)
    }
}