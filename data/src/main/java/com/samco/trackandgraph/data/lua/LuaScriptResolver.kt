package com.samco.trackandgraph.data.lua

import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaScriptResolver @Inject constructor(
    private val globalsProvider: GlobalsProvider,
) {

    private val globals get() = globalsProvider.globals.value

    fun resolveLuaGraphScriptResult(
        script: String,
        dataSources: LuaValue,
    ): LuaValue {
        val cleanedScript = script.cleanLuaScript()
        val scriptResult = globals.load(cleanedScript).call()
        return when {
            scriptResult.isfunction() -> scriptResult.checkfunction()!!.call(dataSources)

            scriptResult.istable() -> scriptResult
            else -> throw IllegalArgumentException("Invalid lua graph script result. Must be a function or table")
        }
    }

    fun resolveLuaScript(script: String): LuaValue {
        val cleanedScript = script.cleanLuaScript()
        return globals.load(cleanedScript).call()
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