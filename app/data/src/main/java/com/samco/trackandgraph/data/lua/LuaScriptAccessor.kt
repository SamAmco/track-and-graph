/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

/** Supplies a main chunk and lazily loaded modules without exposing storage to the Lua engine. */
interface LuaScriptAccessor {
    fun mainScript(): String
    fun moduleScript(moduleName: String): String?
}

class InlineLuaScriptAccessor(private val script: String) : LuaScriptAccessor {
    override fun mainScript(): String = script
    override fun moduleScript(moduleName: String): String? = null
}

/**
 * Reads an ordinary ZIP file whose entry point is `main.lua`. `require("chart.renderer")`
 * resolves `chart/renderer.lua`, then `chart/renderer/init.lua`. Entries are inflated on demand.
 */
class ZipLuaScriptAccessor(private val archive: ByteArray) : LuaScriptAccessor {
    init {
        require(archive.size <= MAX_ARCHIVE_BYTES) { "Lua graph package is too large" }
        require(MAIN_SCRIPT in entryNames()) { "Lua graph package must contain $MAIN_SCRIPT" }
    }

    override fun mainScript(): String = requireNotNull(readEntry(MAIN_SCRIPT))

    override fun moduleScript(moduleName: String): String? {
        require(MODULE_NAME.matches(moduleName)) { "Invalid Lua module name: $moduleName" }
        val path = moduleName.replace('.', '/')
        return readEntry("$path.lua") ?: readEntry("$path/init.lua")
    }

    private fun entryNames(): Set<String> {
        val result = mutableSetOf<String>()
        var count = 0
        var totalBytes = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        zipInput().use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                validateEntryName(entry.name)
                if (!entry.isDirectory && !result.add(entry.name)) {
                    throw IllegalArgumentException("Duplicate ZIP entry: ${entry.name}")
                }
                count++
                require(count <= MAX_ENTRIES) { "Lua graph package contains too many entries" }
                if (!entry.isDirectory) {
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        totalBytes += read
                        require(totalBytes <= MAX_UNCOMPRESSED_BYTES) {
                            "Lua graph package expands to too much data"
                        }
                    }
                }
            }
        }
        return result
    }

    private fun readEntry(name: String): String? {
        zipInput().use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: return null
                validateEntryName(entry.name)
                if (!entry.isDirectory && entry.name == name) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    return decodeUtf8(output.toByteArray(), name)
                }
            }
        }
    }

    private fun zipInput() = ZipInputStream(ByteArrayInputStream(archive))

    private fun validateEntryName(name: String) {
        require(name.isNotBlank() && !name.startsWith('/') && '\\' !in name) {
            "Invalid ZIP entry name: $name"
        }
        require(name.split('/').none { it == "." || it == ".." || it.isEmpty() }) {
            "Invalid ZIP entry path: $name"
        }
    }

    private fun decodeUtf8(bytes: ByteArray, name: String): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: Exception) {
        throw IllegalArgumentException("Lua package entry is not valid UTF-8: $name", e)
    }

    companion object {
        const val MAX_ARCHIVE_BYTES = 5 * 1024 * 1024
        const val MAX_UNCOMPRESSED_BYTES = 8 * 1024 * 1024
        const val MAX_ENTRIES = 256
        private const val MAIN_SCRIPT = "main.lua"
        private val MODULE_NAME = Regex("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*")
    }
}
