package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.apiimpl.RequireApiImpl
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import javax.inject.Inject
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import javax.inject.Singleton

@Singleton
class GlobalsProvider @Inject constructor(
    private val requireApi: RequireApiImpl,
) {
    val globals: Lazy<Globals> = lazy {
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
        // Print isn't dangerous but it won't work so I would rather throw than
        // fail silently as it may be confusing
        globals["print"] = LuaValue.NIL
        LoadState.install(globals)
        LuaC.install(globals)
        requireApi.installIn(globals)
        globals
    }
}