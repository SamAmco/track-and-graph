/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.community_test_runner

import com.samco.trackandgraph.data.lua.ZipLuaScriptAccessor
import com.samco.trackandgraph.data.time.TimeProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(Parameterized::class)
internal class GraphPackageTestRunner : CommunityTestRunner() {
    @Parameterized.Parameter(0) lateinit var testName: String
    @Parameterized.Parameter(1) lateinit var packageDirectory: File
    @Parameterized.Parameter(2) lateinit var testScript: String

    private val apiUsage by lazy { ApiUsageTracker { requireNotNull(readAssetToString(it)) } }
    override fun getModuleLoadInterceptor() = apiUsage
    override val timeProvider = object : TimeProvider {
        private val date = ZonedDateTime.parse("2026-11-14T12:00:00Z")
        override fun now() = date
        override fun epochMilli() = date.toInstant().toEpochMilli()
        override fun defaultZone() = ZoneOffset.UTC
    }

    @Test
    fun `run graph package tests through the real engine`() {
        val provider = daggerComponent.provideVMProvider()
        val lease = acquireTestVMLease(provider)
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.UK)
        try {
            val accessor = ZipLuaScriptAccessor(zipPackage(packageDirectory))
            val tests = lease.globals.load(testScript).call().checktable()!!
            check(tests.keys().isNotEmpty()) { "No tests in $testName" }
            for (key in tests.keys().sortedBy { it.tojstring() }) {
                try {
                    apiUsage.reset(lease.globals)
                    val harness = GraphTestHarness(
                        daggerComponent.provideLuaEngine(), lease, accessor, apiUsage, dataPointParser,
                    ) { requireNotNull(readAssetToString(it)) }
                    tests[key].checkfunction()!!.call(harness.api)
                    println("Test passed: $testName.$key")
                } catch (t: Throwable) {
                    throw AssertionError("Graph test failed: $testName.$key", t)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
            provider.release(lease)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<Array<Any>> {
            val root = File(requireNotNull(GraphPackageTestRunner::class.java.classLoader!!
                .getResource("generated/lua-graphs")) { "Missing graph test resources" }.toURI())
            val entries = root.walkTopDown().filter { it.name == "main.lua" && it.parentFile.name == "package" }.toList()
            check(entries.isNotEmpty()) { "No graph packages discovered" }
            return entries.flatMap { entry ->
                val graph = entry.parentFile.parentFile
                val tests = File(graph, "test").listFiles().orEmpty()
                    .filter { it.isFile && it.name.startsWith("test_") && it.extension == "lua" }
                check(tests.isNotEmpty()) { "No test/test_*.lua files for ${graph.name}" }
                tests.sortedBy { it.name }.map { arrayOf<Any>(
                    "${graph.relativeTo(root)}/test/${it.name}", entry.parentFile, it.readText(),
                ) }
            }
        }

        internal fun zipPackage(directory: File): ByteArray = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                directory.walkTopDown().filter { it.isFile }.sortedBy { it.path }.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.relativeTo(directory).invariantSeparatorsPath))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }
}
