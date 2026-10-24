package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.data.database.dto.LuaGraphPackage
import com.samco.trackandgraph.data.database.dto.LuaGraphSource
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaScriptAccessor
import com.samco.trackandgraph.data.lua.TestLuaVMFixtures
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LuaGraphConfigurationProviderTest {
    private val luaEngine: LuaEngine = mock()
    private val vmLock = TestLuaVMFixtures.createTestLuaVMLock()
    private val descriptor: LuaValueHandle = mock()
    private val featureMap = LuaValueSnapshot.TableValue(emptyMap())

    @Test
    fun `creates the same retained configuration content for an inline script`() = runTest {
        val renderer: LuaValueHandle = mock()
        val state = LuaValueSnapshot.TableValue(
            mapOf("value" to LuaValueSnapshot.NumberValue(1.0))
        )
        whenever(luaEngine.acquireVM()).thenReturn(vmLock)
        whenever(luaEngine.runLuaScript(any(), any<LuaScriptAccessor>())).thenReturn(descriptor)
        whenever(descriptor["render_configuration"]).thenReturn(renderer)
        whenever(renderer.asFunction()).thenReturn(renderer)
        val content = LuaGraphConfigurationProvider(luaEngine)(
            LuaGraphSource.Inline("return {}"),
            lazyOf(state),
            featureMap,
        )

        assertNotNull(content)
        verify(luaEngine).runLuaScript(
            eq(vmLock),
            argThat<LuaScriptAccessor> { mainScript() == "return {}" },
        )
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `package and inline sources share descriptor capability detection`() = runTest {
        whenever(luaEngine.acquireVM()).thenReturn(vmLock)
        whenever(luaEngine.runLuaScript(any(), any<LuaScriptAccessor>())).thenReturn(descriptor)
        whenever(descriptor["render_configuration"]).thenReturn(null)
        val archive = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("main.lua"))
                zip.write("return function() end".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val content = LuaGraphConfigurationProvider(luaEngine)(
            LuaGraphSource.Package(LuaGraphPackage("example", "1.0.0", archive)),
            null,
            featureMap,
        )

        assertNull(content)
        verify(luaEngine).runLuaScript(
            eq(vmLock),
            argThat<LuaScriptAccessor> { mainScript() == "return function() end" },
        )
        verify(luaEngine).releaseVM(vmLock)
    }
}
