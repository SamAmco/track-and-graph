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

import com.samco.trackandgraph.data.lua.apiimpl.RequireApiImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class LuaVMProviderTest {

    private fun createVMProvider(): LuaVMProvider {
        val requireApi = mock<RequireApiImpl>()
        return LuaVMProvider(requireApi)
    }

    @Test
    fun `acquire creates first VM when pool is empty`() = runTest {
        val provider = createVMProvider()
        
        val vm = provider.acquire()
        
        assertNotNull(vm)
        assertEquals("VM-0", vm.name)
        
        provider.release(vm)
    }

    @Test
    fun `acquire reuses available VM instead of creating new one`() = runTest {
        val provider = createVMProvider()
        
        // Acquire and release first VM
        val vm1 = provider.acquire()
        provider.release(vm1)
        
        // Acquire again - should reuse the same VM
        val vm2 = provider.acquire()
        
        assertEquals(vm1.name, vm2.name)
        assertEquals("VM-0", vm2.name)
        
        provider.release(vm2)
    }

    @Test
    fun `acquire creates new VM when existing ones are busy`() = runTest {
        val provider = createVMProvider()
        
        // Acquire first VM and keep it locked
        val vm1 = provider.acquire()
        
        // Acquire second VM - should create new one since first is busy
        val vm2 = provider.acquire()
        
        assertNotEquals(vm1.name, vm2.name)
        assertEquals("VM-0", vm1.name)
        assertEquals("VM-1", vm2.name)
        
        provider.release(vm1)
        provider.release(vm2)
    }

    @Test
    fun `acquire does not exceed maximum VM limit of 8`() = runTest {
        val provider = createVMProvider()
        val maxVMs = LuaVMProvider.MAX_VMS
        
        // Acquire maximum number of VMs
        val vms = mutableListOf<VMLease>()
        repeat(maxVMs) {
            vms.add(provider.acquire())
        }
        
        // Verify we have exactly 8 different VMs
        val vmNames = vms.map { it.name }.toSet()
        assertEquals(maxVMs, vmNames.size)

        // Try to acquire one more - should reuse existing VM (round-robin)
        val extraVM = async { provider.acquire() }

        runCurrent()

        // Release one VM to allow the extra acquire to complete
        provider.release(vms[0])
        val acquiredExtraVM = extraVM.await()
        
        // Should reuse one of the existing VMs
        assertTrue(vmNames.contains(acquiredExtraVM.name))
        
        // Clean up
        vms.drop(1).forEach { provider.release(it) }
        provider.release(acquiredExtraVM)
    }

    @Test
    fun `concurrent acquire creates optimal number of VMs`() = runTest {
        val provider = createVMProvider()
        val concurrentRequests = 5
        
        // Launch concurrent acquire operations
        val vms = (1..concurrentRequests).map {
            async { provider.acquire() }
        }.awaitAll()
        
        // Should create exactly 5 VMs for 5 concurrent requests
        val vmNames = vms.map { it.name }.toSet()
        assertEquals(concurrentRequests, vmNames.size)
        
        // Verify VM names are sequential
        val expectedNames = (0 until concurrentRequests).map { "VM-$it" }.toSet()
        assertEquals(expectedNames, vmNames)
        
        // Clean up
        vms.forEach { provider.release(it) }
    }

    @Test
    fun `round-robin selection when at capacity`() = runTest {
        val provider = createVMProvider()
        val maxVMs = LuaVMProvider.MAX_VMS

        // Fill the pool to capacity
        val vms = (1..maxVMs).map { provider.acquire() }

        // Create tasks that will wait for VMs to become available
        val waitingTasks = (1..3).map { taskIndex ->
            async {
                val vm = provider.acquire()
                vm.name to taskIndex
            }
        }

        runCurrent()

        // Release VMs one by one and collect which VM each waiting task gets
        repeat(3) { index ->
            provider.release(vms[index])
        }

        runCurrent()

        val completedResults = waitingTasks.awaitAll()

        // Verify that different VMs were assigned (round-robin behavior)
        val assignedVMs = completedResults.map { it.first }.toSet()
        assertTrue("Should assign different VMs via round-robin", assignedVMs.size > 1)

        // Clean up all VMs
        vms.forEach { provider.release(it) }
    }

    @Test
    fun `VM pool grows incrementally only when needed`() = runTest {
        val provider = createVMProvider()
        
        // Test incremental growth
        val vm1 = provider.acquire()
        assertEquals("VM-0", vm1.name)
        
        val vm2 = provider.acquire() 
        assertEquals("VM-1", vm2.name)
        
        // Release first VM
        provider.release(vm1)
        
        // Next acquire should reuse VM-0 instead of creating VM-2
        val vm3 = provider.acquire()
        assertEquals("VM-0", vm3.name)
        
        provider.release(vm2)
        provider.release(vm3)
    }

    @Test
    fun `stress test - many concurrent operations stay within limits`() = runTest {
        val provider = createVMProvider()
        val operationCount = 50
        val maxVMs = LuaVMProvider.MAX_VMS
        
        // Track all VM names we see
        val observedVMs = mutableSetOf<String>()
        
        // Launch many concurrent operations
        val operations = (1..operationCount).map { operationId ->
            launch {
                val vm = provider.acquire()
                synchronized(observedVMs) {
                    observedVMs.add(vm.name)
                }
                // Simulate some work
                delay(1)
                provider.release(vm)
            }
        }
        
        // Wait for all operations to complete
        operations.forEach { it.join() }
        
        // Verify we never exceeded the maximum VM limit
        assertTrue(
            "Should not create more than $maxVMs VMs, but saw: $observedVMs",
            observedVMs.size <= maxVMs
        )
    }
}
