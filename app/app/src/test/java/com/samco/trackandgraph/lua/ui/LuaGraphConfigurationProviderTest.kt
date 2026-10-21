package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.data.database.dto.LuaGraphPackage
import com.samco.trackandgraph.data.database.dto.LuaGraphSource
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaGraphScriptAccessor
import com.samco.trackandgraph.data.lua.TestLuaVMFixtures
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
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
    private val descriptor: LuaValueAccessor = mock()

    @Test
    fun `creates the same retained configuration content for an inline script`() = runTest {
        val rendererAccessor: LuaValueAccessor = mock()
        val renderer: LuaValueReference = mock()
        val state: LuaValueReference = mock()
        whenever(luaEngine.acquireVM()).thenReturn(vmLock)
        whenever(luaEngine.runLuaScript(any(), any<LuaGraphScriptAccessor>())).thenReturn(descriptor)
        whenever(descriptor["render_configuration"]).thenReturn(rendererAccessor)
        whenever(rendererAccessor.asFunction()).thenReturn(renderer)
        whenever(luaEngine.decodeLuaState(vmLock, "{\"enabled\":true}"))
            .thenReturn(state)

        val content = LuaGraphConfigurationProvider(luaEngine)(
            LuaGraphSource.Inline("return {}"),
            "{\"enabled\":true}",
        )

        assertNotNull(content)
        verify(luaEngine).runLuaScript(
            eq(vmLock),
            argThat<LuaGraphScriptAccessor> { mainScript() == "return {}" },
        )
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `package and inline sources share descriptor capability detection`() = runTest {
        whenever(luaEngine.acquireVM()).thenReturn(vmLock)
        whenever(luaEngine.runLuaScript(any(), any<LuaGraphScriptAccessor>())).thenReturn(descriptor)
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
        )

        assertNull(content)
        verify(luaEngine).runLuaScript(
            eq(vmLock),
            argThat<LuaGraphScriptAccessor> { mainScript() == "return function() end" },
        )
        verify(luaEngine).releaseVM(vmLock)
    }
}
