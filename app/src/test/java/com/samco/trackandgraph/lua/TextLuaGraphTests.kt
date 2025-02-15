package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TextSize
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TextLuaGraphTests : LuaEngineImplTest() {
    @Test
    fun `Text type returns NIL`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = NIL
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals(null, textData.text)
        assertEquals(TextSize.LARGE, textData.size)
    }


    @Test
    fun `Text type with just text`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = "text" 
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
    }

    @Test
    fun `Text type with text and size small`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = {
                    text = "text",
                    size = 1
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.SMALL, textData.size)
    }

    @Test
    fun `Text type with text and size medium`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = {
                    text = "text",
                    size = 2
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
    }

    @Test
    fun `Text type with text and size large`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = {
                    text = "text",
                    size = 3
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
    }

    @Test
    fun `Text type with nested text and no size`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.TEXT,
                data = {
                    text = "text",
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
    }
}