/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Constraints
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.ui.dataVisColorList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LuaCanvasInstructionParserTest {
    @Test
    fun `uses requested text width as alignment bounds without forcing height`() {
        val constraints = textConstraints(Size(40f, 20f))

        assertEquals(40, constraints.minWidth)
        assertEquals(40, constraints.maxWidth)
        assertEquals(0, constraints.minHeight)
        assertEquals(20, constraints.maxHeight)
        assertEquals(Constraints(), textConstraints(null))
    }

    @Test
    fun `vertically aligns measured text within its requested bounds`() {
        assertEquals(0f, verticalTextOffset(30f, 10, "top"), 0f)
        assertEquals(10f, verticalTextOffset(30f, 10, "center"), 0f)
        assertEquals(20f, verticalTextOffset(30f, 10, "bottom"), 0f)
        assertEquals(0f, verticalTextOffset(null, 10, "center"), 0f)
    }

    @Test
    fun `draws instructions in Lua array order`() {
        val scope: DrawScope = mock()
        val instructions = accessor(listOf(
            mapOf(
                "type" to "LINE", "color" to "THEME_PRIMARY",
                "start" to mapOf("x" to 1.0, "y" to 2.0),
                "finish" to mapOf("x" to 3.0, "y" to 4.0),
                "stroke_width" to 5.0, "cap" to "round", "alpha" to 0.5,
            ),
            mapOf(
                "type" to "CIRCLE", "color" to 7.0,
                "center" to mapOf("x" to 6.0, "y" to 7.0), "radius" to 8.0,
            ),
        ))

        LuaCanvasInstructionParser.draw(scope, instructions, mapOf("THEME_PRIMARY" to Color.Blue))

        verify(scope).drawLine(
            eq(Color.Blue), eq(Offset(1f, 2f)), eq(Offset(3f, 4f)), eq(5f),
            eq(StrokeCap.Round), isNull(), eq(0.5f), isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawCircle(
            eq(dataVisColorList[7]), eq(8f), eq(Offset(6f, 7f)), eq(1f), eq(Fill),
            isNull(), eq(BlendMode.SrcOver),
        )
    }

    @Test
    fun `draws the expanded shape and path instructions`() {
        val scope: DrawScope = mock()
        val instructions = accessor(listOf(
            mapOf(
                "type" to "RECT", "color" to "#112233",
                "top_left" to mapOf("x" to 1.0, "y" to 2.0),
                "size" to mapOf("width" to 3.0, "height" to 4.0),
            ),
            mapOf(
                "type" to "ROUND_RECT", "color" to "#112233",
                "top_left" to mapOf("x" to 5.0, "y" to 6.0),
                "size" to mapOf("width" to 7.0, "height" to 8.0),
                "corner_radius" to mapOf("x" to 2.0, "y" to 3.0),
                "style" to "stroke", "stroke_width" to 2.0,
            ),
            mapOf(
                "type" to "OVAL", "color" to "#112233",
                "top_left" to mapOf("x" to 9.0, "y" to 10.0),
                "size" to mapOf("width" to 11.0, "height" to 12.0),
            ),
            mapOf(
                "type" to "ARC", "color" to "#112233",
                "start_angle" to 13.0, "sweep_angle" to 14.0, "use_center" to true,
                "top_left" to mapOf("x" to 15.0, "y" to 16.0),
                "size" to mapOf("width" to 17.0, "height" to 18.0),
            ),
            mapOf(
                "type" to "PATH", "color" to "#112233",
                "commands" to emptyList<Any>(),
            ),
            mapOf(
                "type" to "POINTS", "color" to "#112233", "mode" to "polygon",
                "points" to listOf(
                    mapOf("x" to 1.0, "y" to 2.0),
                    mapOf("x" to 3.0, "y" to 4.0),
                ),
                "stroke_width" to 5.0, "cap" to "square",
            ),
            mapOf(
                "type" to "RECT",
                "brush" to mapOf(
                    "type" to "LINEAR_GRADIENT",
                    "colors" to listOf("#112233", "#FFFFFF"),
                ),
                "top_left" to mapOf("x" to 20.0, "y" to 21.0),
                "size" to mapOf("width" to 22.0, "height" to 23.0),
                "blend_mode" to "multiply",
                "color_filter" to mapOf("type" to "TINT", "color" to "#FFFFFF"),
            ),
        ))
        val color = Color(0xFF112233)

        LuaCanvasInstructionParser.draw(scope, instructions, emptyMap<String, Color>())

        verify(scope).drawRect(
            eq(color), eq(Offset(1f, 2f)), eq(Size(3f, 4f)), eq(1f), eq(Fill),
            isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawRoundRect(
            eq(color), eq(Offset(5f, 6f)), eq(Size(7f, 8f)), eq(CornerRadius(2f, 3f)),
            eq(Stroke(2f)), eq(1f), isNull<ColorFilter>(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawOval(
            eq(color), eq(Offset(9f, 10f)), eq(Size(11f, 12f)), eq(1f), eq(Fill),
            isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawArc(
            eq(color), eq(13f), eq(14f), eq(true), eq(Offset(15f, 16f)),
            eq(Size(17f, 18f)), eq(1f), eq(Fill), isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawPath(
            any<Path>(), eq(color), eq(1f), eq(Fill), isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawPoints(
            eq(listOf(Offset(1f, 2f), Offset(3f, 4f))), eq(PointMode.Polygon), eq(color),
            eq(5f), eq(StrokeCap.Square), isNull(), eq(1f), isNull(), eq(BlendMode.SrcOver),
        )
        verify(scope).drawRect(
            any<Brush>(), eq(Offset(20f, 21f)), eq(Size(22f, 23f)), eq(1f), eq(Fill),
            any<ColorFilter>(), eq(BlendMode.Multiply),
        )
    }

    @Test
    fun `applies grouped transforms around their children`() {
        val scope: DrawScope = mock()
        val context: DrawContext = mock()
        val transform: DrawTransform = mock()
        val canvas: Canvas = mock()
        whenever(scope.drawContext).thenReturn(context)
        whenever(context.transform).thenReturn(transform)
        whenever(context.canvas).thenReturn(canvas)
        whenever(context.size).thenReturn(Size(100f, 80f))
        val instructions = accessor(listOf(
            mapOf(
                "type" to "GROUP",
                "operation" to mapOf(
                    "type" to "WITH_TRANSFORM",
                    "operations" to listOf(
                        mapOf("type" to "TRANSLATE", "left" to 2.0, "top" to 3.0),
                        mapOf(
                            "type" to "ROTATE", "degrees" to 45.0,
                            "pivot" to mapOf("x" to 10.0, "y" to 20.0),
                        ),
                        mapOf(
                            "type" to "SCALE", "x" to 2.0, "y" to 3.0,
                            "pivot" to mapOf("x" to 5.0, "y" to 6.0),
                        ),
                        mapOf(
                            "type" to "CLIP_RECT", "left" to 1.0, "top" to 2.0,
                            "right" to 30.0, "bottom" to 40.0, "clip_op" to "difference",
                        ),
                    ),
                ),
                "instructions" to listOf(
                    mapOf("type" to "CIRCLE", "color" to "#FFFFFF", "radius" to 4.0),
                ),
            ),
        ))

        LuaCanvasInstructionParser.draw(scope, instructions, emptyMap<String, Color>())

        inOrder(canvas, transform, scope) {
            verify(canvas).save()
            verify(transform).translate(2f, 3f)
            verify(transform).rotate(45f, Offset(10f, 20f))
            verify(transform).scale(2f, 3f, Offset(5f, 6f))
            verify(transform).clipRect(1f, 2f, 30f, 40f, androidx.compose.ui.graphics.ClipOp.Difference)
            verify(scope).drawCircle(
                eq(Color.White), eq(4f), eq(Offset.Zero), eq(1f), eq(Fill),
                isNull(), eq(BlendMode.SrcOver),
            )
            verify(canvas).restore()
        }
    }

    @Test
    fun `maps every path command`() {
        val outer: Path = mock()
        val nested: Path = mock()
        val paths = ArrayDeque(listOf(outer, nested))
        val commands = accessor(
            listOf(
                mapOf("type" to "MOVE_TO", "x" to 1.0, "y" to 2.0),
                mapOf("type" to "RELATIVE_MOVE_TO", "x" to 3.0, "y" to 4.0),
                mapOf("type" to "LINE_TO", "x" to 5.0, "y" to 6.0),
                mapOf("type" to "RELATIVE_LINE_TO", "x" to 7.0, "y" to 8.0),
                mapOf(
                    "type" to "QUADRATIC_TO", "x1" to 1.0, "y1" to 2.0,
                    "x2" to 3.0, "y2" to 4.0,
                ),
                mapOf(
                    "type" to "RELATIVE_QUADRATIC_TO", "x1" to 5.0, "y1" to 6.0,
                    "x2" to 7.0, "y2" to 8.0,
                ),
                mapOf(
                    "type" to "CUBIC_TO", "x1" to 1.0, "y1" to 2.0,
                    "x2" to 3.0, "y2" to 4.0, "x3" to 5.0, "y3" to 6.0,
                ),
                mapOf(
                    "type" to "RELATIVE_CUBIC_TO", "x1" to 7.0, "y1" to 8.0,
                    "x2" to 9.0, "y2" to 10.0, "x3" to 11.0, "y3" to 12.0,
                ),
                pathRectCommand("ARC_TO") + mapOf(
                    "start_angle" to 10.0, "sweep_angle" to 20.0, "force_move_to" to true,
                ),
                pathRectCommand("ARC_TO_RAD") + mapOf(
                    "start_angle" to 0.5, "sweep_angle" to 1.0,
                ),
                pathRectCommand("ADD_RECT") + mapOf("direction" to "clockwise"),
                pathRectCommand("ADD_OVAL"),
                pathRectCommand("ADD_ROUND_RECT") + mapOf(
                    "corner_radius" to mapOf("x" to 2.0, "y" to 3.0),
                ),
                pathRectCommand("ADD_ARC") + mapOf(
                    "start_angle" to 30.0, "sweep_angle" to 40.0,
                ),
                pathRectCommand("ADD_ARC_RAD") + mapOf(
                    "start_angle" to 1.5, "sweep_angle" to 2.0,
                ),
                mapOf(
                    "type" to "ADD_PATH",
                    "commands" to listOf(mapOf("type" to "MOVE_TO", "x" to 9.0, "y" to 10.0)),
                    "offset" to mapOf("x" to 20.0, "y" to 30.0),
                ),
                mapOf("type" to "TRANSLATE", "offset" to mapOf("x" to 4.0, "y" to 5.0)),
                mapOf("type" to "TRANSFORM", "matrix" to identityMatrix()),
                mapOf("type" to "CLOSE"),
            ),
        )

        val result = LuaCanvasInstructionParser.path(commands) { paths.removeFirst() }

        assertSame(outer, result)
        verify(outer).moveTo(1f, 2f)
        verify(outer).relativeMoveTo(3f, 4f)
        verify(outer).lineTo(5f, 6f)
        verify(outer).relativeLineTo(7f, 8f)
        verify(outer).quadraticTo(1f, 2f, 3f, 4f)
        verify(outer).relativeQuadraticTo(5f, 6f, 7f, 8f)
        verify(outer).cubicTo(1f, 2f, 3f, 4f, 5f, 6f)
        verify(outer).relativeCubicTo(7f, 8f, 9f, 10f, 11f, 12f)
        verify(outer).arcTo(Rect(1f, 2f, 4f, 6f), 10f, 20f, true)
        verify(outer).arcToRad(Rect(1f, 2f, 4f, 6f), 0.5f, 1f, false)
        verify(outer).addRect(Rect(1f, 2f, 4f, 6f), Path.Direction.Clockwise)
        verify(outer).addOval(Rect(1f, 2f, 4f, 6f), Path.Direction.CounterClockwise)
        verify(outer).addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(1f, 2f, 4f, 6f), CornerRadius(2f, 3f),
            ),
            Path.Direction.CounterClockwise,
        )
        verify(outer).addArc(Rect(1f, 2f, 4f, 6f), 30f, 40f)
        verify(outer).addArcRad(Rect(1f, 2f, 4f, 6f), 1.5f, 2f)
        verify(nested).moveTo(9f, 10f)
        verify(outer).addPath(nested, Offset(20f, 30f))
        verify(outer).translate(Offset(4f, 5f))
        verify(outer).transform(any())
        verify(outer).close()
    }

    private fun pathRectCommand(type: String) =
        mapOf(
            "type" to type,
            "top_left" to mapOf("x" to 1.0, "y" to 2.0),
            "size" to mapOf("width" to 3.0, "height" to 4.0),
        )

    private fun identityMatrix() = listOf(
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0,
    )

    @Test
    fun `parses empty paths without Android path operations`() {
        val scope: DrawScope = mock()
        val instructions = accessor(listOf(
            mapOf(
                "type" to "PATH", "color" to "#FFFFFF",
                "commands" to emptyList<Any>(),
            ),
        ))

        LuaCanvasInstructionParser.draw(scope, instructions, emptyMap<String, Color>())

        verify(scope).drawPath(
            any<Path>(), eq(Color.White), eq(1f), eq(Fill), isNull(), eq(BlendMode.SrcOver),
        )
    }

    @Test
    fun `maps gradient stroke filter and path effect options`() {
        val scope: DrawScope = mock()
        val instructions = accessor(listOf(
            mapOf(
                "type" to "RECT",
                "brush" to mapOf(
                    "type" to "RADIAL_GRADIENT",
                    "color_stops" to listOf(
                        mapOf("offset" to 0.0, "color" to "THEME_PRIMARY"),
                        mapOf("offset" to 1.0, "color" to "#FFFFFF"),
                    ),
                    "center" to mapOf("x" to 10.0, "y" to 20.0),
                    "radius" to 30.0,
                    "tile_mode" to "mirror",
                ),
                "top_left" to mapOf("x" to 1.0, "y" to 2.0),
                "size" to mapOf("width" to 3.0, "height" to 4.0),
                "style" to "stroke",
                "stroke_width" to 5.0,
                "miter" to 6.0,
                "cap" to "round",
                "join" to "bevel",
                "path_effect" to mapOf(
                    "type" to "DASH", "intervals" to listOf(2.0, 3.0), "phase" to 1.0,
                ),
                "color_filter" to mapOf(
                    "type" to "LIGHTING", "multiply" to "#FFFFFF", "add" to "#010203",
                ),
                "blend_mode" to "screen",
            ),
        ))
        val style = argumentCaptor<DrawStyle>()

        LuaCanvasInstructionParser.draw(
            scope, instructions, mapOf("THEME_PRIMARY" to Color.Blue),
        )

        verify(scope).drawRect(
            any<Brush>(), eq(Offset(1f, 2f)), eq(Size(3f, 4f)), eq(1f),
            style.capture(), any<ColorFilter>(), eq(BlendMode.Screen),
        )
        val stroke = style.firstValue as Stroke
        assertEquals(5f, stroke.width)
        assertEquals(6f, stroke.miter)
        assertEquals(StrokeCap.Round, stroke.cap)
        assertEquals(StrokeJoin.Bevel, stroke.join)
        assertTrue(stroke.pathEffect is PathEffect)
    }

    @Test
    fun `maps every gradient brush type`() {
        val scope: DrawScope = mock()
        val colors = listOf("#000000", "#FFFFFF")
        val brushes = listOf(
            mapOf(
                "type" to "LINEAR_GRADIENT", "colors" to colors,
                "start" to mapOf("x" to 1.0, "y" to 2.0),
                "finish" to mapOf("x" to 3.0, "y" to 4.0), "tile_mode" to "repeated",
            ),
            mapOf(
                "type" to "HORIZONTAL_GRADIENT", "colors" to colors,
                "start_x" to 1.0, "end_x" to 3.0, "tile_mode" to "mirror",
            ),
            mapOf(
                "type" to "VERTICAL_GRADIENT", "colors" to colors,
                "start_y" to 2.0, "end_y" to 4.0, "tile_mode" to "decal",
            ),
            mapOf(
                "type" to "RADIAL_GRADIENT", "colors" to colors,
                "center" to mapOf("x" to 2.0, "y" to 3.0), "radius" to 4.0,
            ),
            mapOf(
                "type" to "SWEEP_GRADIENT", "colors" to colors,
                "center" to mapOf("x" to 2.0, "y" to 3.0),
            ),
        )
        val instructions = accessor(brushes.mapIndexed { index, brush ->
            mapOf(
                "type" to "RECT", "brush" to brush,
                "top_left" to mapOf("x" to index.toDouble(), "y" to 0.0),
                "size" to mapOf("width" to 1.0, "height" to 1.0),
            )
        })

        LuaCanvasInstructionParser.draw(scope, instructions, emptyMap<String, Color>())

        brushes.indices.forEach { index ->
            verify(scope).drawRect(
                any<Brush>(), eq(Offset(index.toFloat(), 0f)), eq(Size(1f, 1f)),
                eq(1f), eq(Fill), isNull(), eq(BlendMode.SrcOver),
            )
        }
    }

    @Test
    fun `maps every supported blend mode`() {
        val scope: DrawScope = mock()
        val modes = mapOf(
            "clear" to BlendMode.Clear, "src" to BlendMode.Src, "dst" to BlendMode.Dst,
            "src_over" to BlendMode.SrcOver, "dst_over" to BlendMode.DstOver,
            "src_in" to BlendMode.SrcIn, "dst_in" to BlendMode.DstIn,
            "src_out" to BlendMode.SrcOut, "dst_out" to BlendMode.DstOut,
            "src_atop" to BlendMode.SrcAtop, "dst_atop" to BlendMode.DstAtop,
            "xor" to BlendMode.Xor, "plus" to BlendMode.Plus,
            "modulate" to BlendMode.Modulate, "screen" to BlendMode.Screen,
            "overlay" to BlendMode.Overlay, "darken" to BlendMode.Darken,
            "lighten" to BlendMode.Lighten, "color_dodge" to BlendMode.ColorDodge,
            "color_burn" to BlendMode.ColorBurn, "hard_light" to BlendMode.Hardlight,
            "soft_light" to BlendMode.Softlight, "difference" to BlendMode.Difference,
            "exclusion" to BlendMode.Exclusion, "multiply" to BlendMode.Multiply,
            "hue" to BlendMode.Hue, "saturation" to BlendMode.Saturation,
            "color" to BlendMode.Color, "luminosity" to BlendMode.Luminosity,
        )
        val instructions = accessor(modes.keys.mapIndexed { index, mode ->
            mapOf(
                "type" to "RECT", "color" to "#FFFFFF", "blend_mode" to mode,
                "top_left" to mapOf("x" to index.toDouble(), "y" to 0.0),
                "size" to mapOf("width" to 1.0, "height" to 1.0),
            )
        })

        LuaCanvasInstructionParser.draw(scope, instructions, emptyMap<String, Color>())

        modes.entries.forEachIndexed { index, (_, expected) ->
            verify(scope).drawRect(
                eq(Color.White), eq(Offset(index.toFloat(), 0f)), eq(Size(1f, 1f)),
                eq(1f), eq(Fill), isNull(), eq(expected),
            )
        }
    }

    private fun accessor(value: Any): LuaValueAccessor = TestAccessor(value)

    private class TestAccessor(private val value: Any) : LuaValueAccessor {
        override fun get(key: String) = (value as? Map<*, *>)?.get(key)?.let(::TestAccessor)
        override fun get(index: Int) = (value as? List<*>)?.getOrNull(index - 1)?.let(::TestAccessor)
        override val arraySize get() = (value as? List<*>)?.size
        override fun asDouble() = (value as? Number)?.toDouble()
        override fun asString() = value as? String
        override fun asBoolean() = value as? Boolean
        override fun asFunction(): LuaValueReference? = null
        override fun snapshot() = error("Canvas parser test values are never snapshotted")
    }
}
