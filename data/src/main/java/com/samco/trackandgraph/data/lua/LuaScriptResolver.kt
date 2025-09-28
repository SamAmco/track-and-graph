package com.samco.trackandgraph.data.lua

import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaScriptResolver @Inject constructor() {
    fun resolveLuaScript(script: String, vmLease: VMLease): LuaValue {
        val cleanedScript = script.cleanLuaScript()
        return synchronized(vmLease.lock) {
            vmLease.globals.load(cleanedScript).call()
        }
    }

    private fun String.cleanLuaScript(): String {
        return this
            // Replace NBSP with space
            .replace(Regex("\\u00A0"), " ")
            // Remove zero-width space and BOM
            .replace(Regex("[\\u200B\\uFEFF]"), "")
            // Replace all new lines with the same newline character
            .replace(Regex("[\\r\\n]+"), "\n")
            .trim()
    }
}