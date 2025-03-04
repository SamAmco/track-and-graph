package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TextAlignment
import com.samco.trackandgraph.lua.dto.TextSize
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TextLuaGraphTests : LuaEngineImplTest() {
    @Test
    fun `Text type can return NIL`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = NIL
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(
            LuaGraphResultData.TextData(
                text = null,
                size = TextSize.LARGE,
                alignment = TextAlignment.CENTER
            ), result.data
        )
        assertEquals(null, result.error)
    }

    @Test
    fun `Text type with just text`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text" 
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with text and size small`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                size = 1
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.SMALL, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with text and size medium`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                size = 2
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with text and size large`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                size = 3
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with nested text and no size`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with alignment start`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                align = "start"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.START, textData.alignment)
    }

    @Test
    fun `Text type with text and alignment center`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                align = "center"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with text and alignment centre`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                align = "centre"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `Text type with text and alignment end`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.TEXT,
                text = "text",
                align = "end"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.LARGE, textData.size)
        assertEquals(TextAlignment.END, textData.alignment)
    }
}