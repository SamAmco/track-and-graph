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
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = NIL
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(
            LuaGraphResultData.TextData(
                text = null,
                size = TextSize.MEDIUM,
                alignment = TextAlignment.CENTER
            ), result.data
        )
        assertEquals(null, result.error)
    }

    @Test
    fun `Text type with just text`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text" 
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
    fun `Text type with text and size small`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
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
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
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
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
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
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text",
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
    fun `Text type with alignment start`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text",
                align = "start"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.START, textData.alignment)
    }

    @Test
    fun `Text type with text and alignment center`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text",
                align = "center"
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
    fun `Text type with text and alignment centre`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text",
                align = "centre"
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
    fun `Text type with text and alignment end`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = "text",
                align = "end"
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("text", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.END, textData.alignment)
    }

    @Test
    fun `Text type with numeric text value`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = 42
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("42", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `text function with string parameter`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return graph.text("hello world")
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("hello world", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `text function with number parameter`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return graph.text(123.45)
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("123.45", textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }

    @Test
    fun `text function with table parameter`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return graph.text({
                text = "custom text",
                size = 1,
                align = "start"
            })
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals("custom text", textData.text)
        assertEquals(TextSize.SMALL, textData.size)
        assertEquals(TextAlignment.START, textData.alignment)
    }
    
    @Test
    fun `text function with nil parameter should handle gracefully`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return graph.text(NIL)
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val textData = result.data as LuaGraphResultData.TextData
        assertEquals(null, textData.text)
        assertEquals(TextSize.MEDIUM, textData.size)
        assertEquals(TextAlignment.CENTER, textData.alignment)
    }
}
