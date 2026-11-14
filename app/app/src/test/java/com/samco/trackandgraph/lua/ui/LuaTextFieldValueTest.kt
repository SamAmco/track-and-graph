package com.samco.trackandgraph.lua.ui

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LuaTextFieldValueTest {
    @Test
    fun `typing advances immediately and delayed echoes and replacements cannot roll it back`() {
        val initial = field("", 12)
        val first = initial.edit(TextFieldValue("a", TextRange(1)))
        val second = first.edit(TextFieldValue("ab", TextRange(2)))

        assertEquals("ab", second.value.text)
        assertEquals(14L, second.revision)
        assertNull(second.replaceWith(first))
        assertNull(second.replaceWith(field("", revision = 14, base = 13)))
        assertTrue(second.needsAcknowledgement(first))
    }

    @Test
    fun `Lua reset applies to its base revision once and acknowledgement clears the request`() {
        val current = field("abc", 7)
        val request = field("", revision = 9, base = 7) // two Lua mutations before rendering
        val applied = requireNotNull(current.replaceWith(request))

        assertEquals("", applied.value.text)
        assertEquals(9L, applied.revision)
        assertNull(applied.baseRevision)
        assertNull(applied.replaceWith(request))
        assertTrue(applied.needsAcknowledgement(request))
        val acknowledged = LuaTextFieldValue.fromLua(applied.snapshot())
        assertFalse(applied.needsAcknowledgement(acknowledged))
        assertNotNull(applied.replaceWith(field("again", revision = 10, base = 9)))
    }

    @Test
    fun `selection clamps to UTF-16 text length and retains reversed ranges`() {
        val incoming = LuaValueSnapshot.TableValue(mapOf(
            "text" to LuaValueSnapshot.StringValue("A😀"),
            "selection" to LuaValueSnapshot.TableValue(mapOf(
                "start" to LuaValueSnapshot.NumberValue(100.0),
                "end" to LuaValueSnapshot.NumberValue(-5.0),
            )),
            "revision" to LuaValueSnapshot.NumberValue(51.0),
        ))
        val restored = LuaTextFieldValue.fromLua(incoming)

        assertEquals(TextRange(3, 0), restored.value.selection)
        assertEquals(51L, restored.revision)
        assertEquals(52L, restored.edit(TextFieldValue("next")).revision)
    }

    @Test
    fun `ordinary echoes preserve IME composition while explicit text replacement commits it`() {
        val current = LuaTextFieldValue(
            TextFieldValue("ab", TextRange(2), composition = TextRange(0, 2)), 4,
        )
        val echo = LuaTextFieldValue.fromLua(current.snapshot())

        assertNull(current.replaceWith(echo))
        assertFalse(current.needsAcknowledgement(echo))
        val moved = requireNotNull(current.replaceWith(field("ab", 5, base = 4)))
        assertEquals(TextRange(0, 2), moved.value.composition)
        val replaced = requireNotNull(current.replaceWith(field("replacement", 5, base = 4)))
        assertNull(replaced.value.composition)
    }

    @Test
    fun `rotation restores native text selection and revision even when Lua is behind`() {
        val native = LuaTextFieldValue(
            TextFieldValue("newer", TextRange(4, 1), composition = TextRange(0, 5)), 23,
        )
        val saved = with(LuaTextFieldValue.Saver) { SaverScope { true }.save(native) }
        val restored = requireNotNull(LuaTextFieldValue.Saver.restore(requireNotNull(saved)))

        assertEquals(native.value.text, restored.value.text)
        assertEquals(native.value.selection, restored.value.selection)
        assertEquals(23L, restored.revision)
        assertNull(restored.value.composition)
        assertTrue(restored.needsAcknowledgement(field("older", 22)))
        assertNull(restored.replaceWith(field("stale reset", 23, base = 22)))
    }

    private fun field(text: String, revision: Long, base: Long? = null) =
        LuaTextFieldValue(TextFieldValue(text, TextRange(text.length)), revision, base)
}
