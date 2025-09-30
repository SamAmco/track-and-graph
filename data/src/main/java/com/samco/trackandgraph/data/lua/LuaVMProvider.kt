package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.apiimpl.RequireApiImpl
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

data class VMLease(
    val globals: Globals,
    val lock: ReentrantLock,
    val name: String,
)

@Singleton
internal class LuaVMProvider @Inject constructor(
    private val requireApi: RequireApiImpl,
) {

    private val poolSize = 8
    private val next = AtomicInteger(0)

    // Lazy initialization of VM pool
    private val vmPool: Lazy<Array<VMLease>> = lazy {
        createVMPool()
    }

    /**
     * Acquire a VM lease for concurrent execution.
     * Returns a VMLease containing a Globals instance and its associated lock.
     * VMs are distributed round-robin to avoid always using the same one.
     */
    fun acquire(): VMLease {
        val pool = vmPool.value
        val index = Math.floorMod(next.getAndIncrement(), poolSize)
        return pool[index]
    }

    private fun createVMPool(): Array<VMLease> {
        // Create array of VM leases with globals and locks
        // Note: Memory measurements showed each VM typically uses 30-40KB of memory
        return Array(poolSize) { idx ->
            VMLease(
                globals = buildGlobals(),
                lock = ReentrantLock(),
                name = "VM-$idx",
            )
        }
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
        globals["pcall"] = LuaValue.NIL
        globals["xpcall"] = LuaValue.NIL
        globals["package"] = LuaValue.NIL
        LoadState.install(globals)
        LuaC.install(globals)
        requireApi.installIn(globals)
        return globals
    }
}