package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.apiimpl.RequireApiImpl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LuaVMLock

internal data class VMLease(
    val globals: Globals,
    val lock: Mutex,
    val name: String,
) : LuaVMLock

internal fun LuaVMLock.asLease(): VMLease = when (this) {
    is VMLease -> this
}

/**
 * This approach solves several problems:
 *
 * 1. Most users (for now) will not use Lua at all, we don't want to create un-necessary VMs and memory
 * pressure, so we have a growing pool of VMs and distribute work between them to benefit from parallelism.
 *
 * 2. You can not use the same Lua engine from two different threads at once. Every call to a given
 * Lua engine must be serialized.
 *
 * 3. Calls to the lua engine interface can be recursive. A lua function may draw from a data source
 * or node, that its self requires the lua engine in order to run. However also keep in mind:
 *
 * 4. Coroutines in LuaJ are implemented using threads. Meaning when a lua function running in a LuaThread
 * calls a function like dp() it is dispatched in kotlin on a different thread. If that dp() function then
 * recursively calls back into the lua engine and you constrain using a lock on each api call (even a re-entrant lock)
 * you can get deadlocks. The LuaJ devs were aware of this issue. See shouldSynchronize here:
 * https://gudzpoz.github.io/luajava/javadoc/party/iroiro/luajava/luaj/LuaJ.html
 *
 * For this reason access to each VM is protected by a mutex. A recursive chain of calls may be made
 * to the same lua engine from different threads, provided they all use the same VM and they make
 * each call to the lua engine serially.
 */
@Singleton
internal class LuaVMProvider @Inject constructor(
    private val requireApi: RequireApiImpl,
) {
    private val poolWriteMutex = Mutex()
    private val vmPool = CopyOnWriteArrayList<VMLease>()
    private val nextVMIndex = AtomicInteger(0)

    suspend fun acquire(): VMLease {
        // 1) Try to grab any free VM without blocking writers
        vmPool.firstOrNull { it.lock.tryLock() }?.let { return it }

        // 2) Grow the pool if we still have capacity
        if (vmPool.size < MAX_VMS) {
            return poolWriteMutex.withLock {
                if (vmPool.size < MAX_VMS) {
                    val newVM = VMLease(
                        globals = buildGlobals(),
                        lock = Mutex(),
                        name = "VM-${vmPool.size}",
                    )
                    vmPool += newVM
                    newVM.lock.lock()
                    newVM
                } else {
                    // capacity filled while we waited → fall through to round-robin
                    val idx = nextVMIndex.getAndIncrement()
                    val vm = vmPool[idx % vmPool.size]
                    vm.lock.lock()
                    vm
                }
            }
        }

        // 3) Nothing free and at capacity → pick next round-robin and suspend on its lock
        val idx = nextVMIndex.getAndIncrement()
        val vm = vmPool[idx % vmPool.size]
        vm.lock.lock()
        return vm
    }

    fun release(vmLease: VMLease) {
        vmLease.lock.unlock()
    }

    private fun buildGlobals(): Globals {
        val globals = Globals()
        // Only install libraries that are useful and not dangerous
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        // Remove potentially dangerous functions from BaseLib
        globals["dofile"] = LuaValue.NIL
        globals["loadfile"] = LuaValue.NIL
        globals["package"] = LuaValue.NIL
        LoadState.install(globals)
        LuaC.install(globals)
        requireApi.installIn(globals)
        return globals
    }

    internal companion object {
        const val MAX_VMS = 8
    }
}