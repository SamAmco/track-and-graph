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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor

internal object LuaCanvasInstructionParser {
    fun draw(
        drawScope: DrawScope,
        instructions: LuaValueAccessor,
        colors: Map<String, Color>,
        textMeasurer: TextMeasurer? = null,
    ) {
        drawScope.drawInstructions(instructions, colors, textMeasurer)
    }

    private fun DrawScope.drawInstructions(
        instructions: LuaValueAccessor,
        colors: Map<String, Color>,
        textMeasurer: TextMeasurer?,
    ) {
        for (index in 1..(instructions.arraySize ?: 0)) {
            val instruction = instructions[index] ?: continue
            if (instruction["type"]?.asString() == "GROUP") {
                drawGroup(instruction, colors, textMeasurer)
                continue
            }
            val paint = paint(instruction, colors) ?: continue
            val alpha = instruction.float("alpha", 1f)
            when (instruction["type"]?.asString()) {
                "LINE" -> drawLineInstruction(instruction, paint, alpha, colors)
                "RECT" -> drawRectInstruction(instruction, paint, alpha, colors)
                "ROUND_RECT" -> drawRoundRectInstruction(instruction, paint, alpha, colors)
                "CIRCLE" -> drawCircleInstruction(instruction, paint, alpha, colors)
                "OVAL" -> drawOvalInstruction(instruction, paint, alpha, colors)
                "ARC" -> drawArcInstruction(instruction, paint, alpha, colors)
                "PATH" -> drawPathInstruction(instruction, paint, alpha, colors)
                "POINTS" -> drawPointsInstruction(instruction, paint, alpha, colors)
                "TEXT" -> if (textMeasurer != null) {
                    drawTextInstruction(instruction, paint, alpha, textMeasurer)
                }
            }
        }
    }

    private sealed interface PaintSource {
        data class ColorValue(val value: Color) : PaintSource
        data class BrushValue(val value: Brush) : PaintSource
    }

    private fun DrawScope.drawLineInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val start = point(instruction["start"])
        val end = point(instruction["finish"])
        val width = instruction.float("stroke_width")
        val cap = strokeCap(instruction["cap"]?.asString())
        val effect = pathEffect(instruction["path_effect"], colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawLine(
                color = paint.value, start = start, end = end, strokeWidth = width, cap = cap,
                pathEffect = effect, alpha = alpha, colorFilter = filter, blendMode = blend,
            )
            is PaintSource.BrushValue -> drawLine(
                brush = paint.value, start = start, end = end, strokeWidth = width, cap = cap,
                pathEffect = effect, alpha = alpha, colorFilter = filter, blendMode = blend,
            )
        }
    }

    private fun DrawScope.drawRectInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val topLeft = point(instruction["top_left"])
        val size = size(instruction["size"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawRect(paint.value, topLeft, size, alpha, style, filter, blend)
            is PaintSource.BrushValue -> drawRect(paint.value, topLeft, size, alpha, style, filter, blend)
        }
    }

    private fun DrawScope.drawRoundRectInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val topLeft = point(instruction["top_left"])
        val size = size(instruction["size"])
        val radius = cornerRadius(instruction["corner_radius"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawRoundRect(
                color = paint.value, topLeft = topLeft, size = size, cornerRadius = radius,
                style = style, alpha = alpha, colorFilter = filter, blendMode = blend,
            )
            is PaintSource.BrushValue -> drawRoundRect(
                brush = paint.value, topLeft = topLeft, size = size, cornerRadius = radius,
                alpha = alpha, style = style, colorFilter = filter, blendMode = blend,
            )
        }
    }

    private fun DrawScope.drawCircleInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val radius = instruction.float("radius")
        val center = point(instruction["center"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawCircle(paint.value, radius, center, alpha, style, filter, blend)
            is PaintSource.BrushValue -> drawCircle(paint.value, radius, center, alpha, style, filter, blend)
        }
    }

    private fun DrawScope.drawOvalInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val topLeft = point(instruction["top_left"])
        val size = size(instruction["size"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawOval(paint.value, topLeft, size, alpha, style, filter, blend)
            is PaintSource.BrushValue -> drawOval(paint.value, topLeft, size, alpha, style, filter, blend)
        }
    }

    private fun DrawScope.drawArcInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val start = instruction.float("start_angle")
        val sweep = instruction.float("sweep_angle")
        val useCenter = instruction["use_center"]?.asBoolean() ?: false
        val topLeft = point(instruction["top_left"])
        val size = size(instruction["size"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawArc(
                paint.value, start, sweep, useCenter, topLeft, size, alpha, style, filter, blend,
            )
            is PaintSource.BrushValue -> drawArc(
                paint.value, start, sweep, useCenter, topLeft, size, alpha, style, filter, blend,
            )
        }
    }

    private fun DrawScope.drawPathInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val path = path(instruction["commands"])
        val style = drawStyle(instruction, colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawPath(path, paint.value, alpha, style, filter, blend)
            is PaintSource.BrushValue -> drawPath(path, paint.value, alpha, style, filter, blend)
        }
    }

    private fun DrawScope.drawPointsInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        colors: Map<String, Color>,
    ) {
        val points = points(instruction["points"])
        val mode = pointMode(instruction["mode"]?.asString())
        val width = instruction.float("stroke_width")
        val cap = strokeCap(instruction["cap"]?.asString())
        val effect = pathEffect(instruction["path_effect"], colors)
        val filter = colorFilter(instruction["color_filter"], colors)
        val blend = blendMode(instruction["blend_mode"]?.asString())
        when (paint) {
            is PaintSource.ColorValue -> drawPoints(
                points, mode, paint.value, width, cap, effect, alpha, filter, blend,
            )
            is PaintSource.BrushValue -> drawPoints(
                points, mode, paint.value, width, cap, effect, alpha, filter, blend,
            )
        }
    }

    private fun DrawScope.drawGroup(
        instruction: LuaValueAccessor,
        colors: Map<String, Color>,
        textMeasurer: TextMeasurer?,
    ) {
        val operation = instruction["operation"] ?: return
        val children = instruction["instructions"] ?: return
        val drawChildren: DrawScope.() -> Unit = { drawInstructions(children, colors, textMeasurer) }
        when (operation["type"]?.asString()) {
            "TRANSLATE" -> translate(
                left = operation.float("left"),
                top = operation.float("top"),
                block = drawChildren,
            )
            "ROTATE" -> rotate(
                degrees = operation.float("degrees"),
                pivot = point(operation["pivot"]),
                block = drawChildren,
            )
            "ROTATE_RAD" -> rotateRad(
                radians = operation.float("radians"),
                pivot = point(operation["pivot"]),
                block = drawChildren,
            )
            "SCALE" -> scale(
                scaleX = operation.float("x", 1f),
                scaleY = operation.float("y", 1f),
                pivot = point(operation["pivot"]),
                block = drawChildren,
            )
            "INSET" -> inset(
                left = operation.float("left"),
                top = operation.float("top"),
                right = operation.float("right"),
                bottom = operation.float("bottom"),
                block = drawChildren,
            )
            "CLIP_RECT" -> clipRect(
                left = operation.float("left"),
                top = operation.float("top"),
                right = operation.float("right"),
                bottom = operation.float("bottom"),
                clipOp = clipOp(operation["clip_op"]?.asString()),
                block = drawChildren,
            )
            "CLIP_PATH" -> clipPath(
                path = path(operation["commands"]),
                clipOp = clipOp(operation["clip_op"]?.asString()),
                block = drawChildren,
            )
            "MATRIX" -> withTransform(
                transformBlock = { transform(matrix(operation["matrix"])) },
                drawBlock = drawChildren,
            )
            "WITH_TRANSFORM" -> withTransform(
                transformBlock = {
                    val operations = operation["operations"]
                    for (index in 1..(operations?.arraySize ?: 0)) {
                        operations?.get(index)?.let { applyOperation(it) }
                    }
                },
                drawBlock = drawChildren,
            )
        }
    }

    private fun DrawTransform.applyOperation(operation: LuaValueAccessor) {
        when (operation["type"]?.asString()) {
            "TRANSLATE" -> translate(operation.float("left"), operation.float("top"))
            "ROTATE" -> rotate(
                operation.float("degrees"),
                operation["pivot"]?.let(::point) ?: center,
            )
            "ROTATE_RAD" -> rotate(
                Math.toDegrees(operation.float("radians").toDouble()).toFloat(),
                operation["pivot"]?.let(::point) ?: center,
            )
            "SCALE" -> scale(
                operation.float("x", operation.float("scale", 1f)),
                operation.float("y", operation.float("scale", 1f)),
                operation["pivot"]?.let(::point) ?: center,
            )
            "INSET" -> inset(
                operation.float("left"), operation.float("top"),
                operation.float("right"), operation.float("bottom"),
            )
            "CLIP_RECT" -> clipRect(
                operation.float("left"), operation.float("top"),
                operation.float("right", size.width), operation.float("bottom", size.height),
                clipOp(operation["clip_op"]?.asString()),
            )
            "CLIP_PATH" -> clipPath(
                path(operation["commands"]),
                clipOp(operation["clip_op"]?.asString()),
            )
            "MATRIX" -> transform(matrix(operation["matrix"]))
        }
    }

    private fun DrawScope.drawTextInstruction(
        instruction: LuaValueAccessor,
        paint: PaintSource,
        alpha: Float,
        textMeasurer: TextMeasurer,
    ) {
        val requestedSize = instruction["size"]?.let(::size)
        val constraints = textConstraints(requestedSize)
        val baseStyle = TextStyle(
            fontSize = instruction["font_size"]?.asDouble()?.toFloat()?.toSp()
                ?: TextStyle.Default.fontSize,
            fontWeight = when (instruction["font_weight"]?.asString()) {
                "medium" -> FontWeight.Medium
                "bold" -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            textAlign = when (instruction["text_align"]?.asString()) {
                "center" -> TextAlign.Center
                "end" -> TextAlign.End
                else -> TextAlign.Start
            },
        )
        val style = when (paint) {
            is PaintSource.ColorValue -> baseStyle.copy(color = paint.value)
            is PaintSource.BrushValue -> baseStyle.copy(brush = paint.value, alpha = alpha)
        }
        val layout = textMeasurer.measure(
            text = instruction["text"]?.asString().orEmpty(),
            style = style,
            overflow = when (instruction["overflow"]?.asString()) {
                "ellipsis" -> TextOverflow.Ellipsis
                "visible" -> TextOverflow.Visible
                else -> TextOverflow.Clip
            },
            softWrap = instruction["soft_wrap"]?.asBoolean() ?: true,
            maxLines = instruction["max_lines"]?.asDouble()?.toInt() ?: Int.MAX_VALUE,
            constraints = constraints,
        )
        drawText(
            textLayoutResult = layout,
            topLeft = point(instruction["top_left"]).let { topLeft ->
                topLeft.copy(
                    y = topLeft.y + verticalTextOffset(
                        containerHeight = requestedSize?.height,
                        textHeight = layout.size.height,
                        alignment = instruction["vertical_align"]?.asString(),
                    ),
                )
            },
            alpha = if (paint is PaintSource.BrushValue) Float.NaN else alpha,
            blendMode = blendMode(instruction["blend_mode"]?.asString()),
        )
    }

    private fun drawStyle(
        instruction: LuaValueAccessor,
        colors: Map<String, Color>,
    ): DrawStyle =
        if (instruction["style"]?.asString() == "stroke") {
            Stroke(
                width = instruction.float("stroke_width"),
                miter = instruction.float("miter", 4f),
                cap = strokeCap(instruction["cap"]?.asString()),
                join = when (instruction["join"]?.asString()) {
                    "round" -> StrokeJoin.Round
                    "bevel" -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                },
                pathEffect = pathEffect(instruction["path_effect"], colors),
            )
        } else Fill

    internal fun path(
        commands: LuaValueAccessor?,
        pathFactory: () -> Path = { Path() },
    ): Path = pathFactory().apply {
        if (commands == null) return@apply
        for (index in 1..(commands.arraySize ?: 0)) {
            val command = commands[index] ?: continue
            when (command["type"]?.asString()) {
                "MOVE_TO" -> moveTo(command.float("x"), command.float("y"))
                "RELATIVE_MOVE_TO" -> relativeMoveTo(command.float("x"), command.float("y"))
                "LINE_TO" -> lineTo(command.float("x"), command.float("y"))
                "RELATIVE_LINE_TO" -> relativeLineTo(command.float("x"), command.float("y"))
                "QUADRATIC_TO" -> quadraticTo(
                    command.float("x1"), command.float("y1"),
                    command.float("x2"), command.float("y2"),
                )
                "RELATIVE_QUADRATIC_TO" -> relativeQuadraticTo(
                    command.float("x1"), command.float("y1"),
                    command.float("x2"), command.float("y2"),
                )
                "CUBIC_TO" -> cubicTo(
                    command.float("x1"), command.float("y1"),
                    command.float("x2"), command.float("y2"),
                    command.float("x3"), command.float("y3"),
                )
                "RELATIVE_CUBIC_TO" -> relativeCubicTo(
                    command.float("x1"), command.float("y1"),
                    command.float("x2"), command.float("y2"),
                    command.float("x3"), command.float("y3"),
                )
                "ARC_TO" -> arcTo(
                    rect(command), command.float("start_angle"), command.float("sweep_angle"),
                    command["force_move_to"]?.asBoolean() ?: false,
                )
                "ARC_TO_RAD" -> arcToRad(
                    rect(command), command.float("start_angle"), command.float("sweep_angle"),
                    command["force_move_to"]?.asBoolean() ?: false,
                )
                "ADD_RECT" -> addRect(
                    rect(command), pathDirection(command["direction"]?.asString()),
                )
                "ADD_OVAL" -> addOval(
                    rect(command), pathDirection(command["direction"]?.asString()),
                )
                "ADD_ROUND_RECT" -> addRoundRect(
                    RoundRect(rect(command), cornerRadius(command["corner_radius"])),
                    pathDirection(command["direction"]?.asString()),
                )
                "ADD_ARC" -> addArc(
                    rect(command), command.float("start_angle"), command.float("sweep_angle"),
                )
                "ADD_ARC_RAD" -> addArcRad(
                    rect(command), command.float("start_angle"), command.float("sweep_angle"),
                )
                "ADD_PATH" -> addPath(
                    path(command["commands"], pathFactory), point(command["offset"]),
                )
                "TRANSLATE" -> translate(point(command["offset"]))
                "TRANSFORM" -> transform(matrix(command["matrix"]))
                "CLOSE" -> close()
            }
        }
    }

    private fun rect(value: LuaValueAccessor): Rect {
        val topLeft = point(value["top_left"])
        val size = size(value["size"])
        return Rect(topLeft, size)
    }

    private fun pathDirection(value: String?) = if (value == "clockwise") {
        Path.Direction.Clockwise
    } else Path.Direction.CounterClockwise

    private fun points(value: LuaValueAccessor?): List<Offset> {
        if (value == null) return emptyList()
        return (1..(value.arraySize ?: 0)).mapNotNull { value[it]?.let(::point) }
    }

    private fun point(value: LuaValueAccessor?) = Offset(
        value?.get("x")?.asDouble()?.toFloat() ?: 0f,
        value?.get("y")?.asDouble()?.toFloat() ?: 0f,
    )

    private fun size(value: LuaValueAccessor?) = Size(
        value?.get("width")?.asDouble()?.toFloat() ?: 0f,
        value?.get("height")?.asDouble()?.toFloat() ?: 0f,
    )

    private fun cornerRadius(value: LuaValueAccessor?) = CornerRadius(
        value?.get("x")?.asDouble()?.toFloat() ?: 0f,
        value?.get("y")?.asDouble()?.toFloat() ?: 0f,
    )

    private fun strokeCap(value: String?) = when (value) {
        "round" -> StrokeCap.Round
        "square" -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

    private fun pointMode(value: String?) = when (value) {
        "lines" -> PointMode.Lines
        "polygon" -> PointMode.Polygon
        else -> PointMode.Points
    }

    private fun clipOp(value: String?) =
        if (value == "difference") ClipOp.Difference else ClipOp.Intersect

    private fun DrawScope.paint(
        instruction: LuaValueAccessor,
        colors: Map<String, Color>,
    ): PaintSource? = instruction["brush"]?.let { brush(it, colors) }
        ?.let(PaintSource::BrushValue)
        ?: resolveLuaColor(instruction["color"], colors)?.let(PaintSource::ColorValue)

    private fun DrawScope.brush(value: LuaValueAccessor, colors: Map<String, Color>): Brush? {
        val colorStops = gradientStops(value["color_stops"] ?: value["stops"], colors)
        val gradientColors = colors(value["colors"], colors)
        val tileMode = tileMode(value["tile_mode"]?.asString())
        return when (value["type"]?.asString()) {
            "LINEAR_GRADIENT" -> if (colorStops != null) {
                Brush.linearGradient(
                    *colorStops, start = value["start"]?.let(::point) ?: Offset.Zero,
                    end = value["finish"]?.let(::point) ?: Offset.Infinite, tileMode = tileMode,
                )
            } else Brush.linearGradient(
                gradientColors, value["start"]?.let(::point) ?: Offset.Zero,
                value["finish"]?.let(::point) ?: Offset.Infinite, tileMode,
            )
            "HORIZONTAL_GRADIENT" -> if (colorStops != null) {
                Brush.horizontalGradient(
                    *colorStops, startX = value.float("start_x"),
                    endX = value.float("end_x", Float.POSITIVE_INFINITY), tileMode = tileMode,
                )
            } else Brush.horizontalGradient(
                gradientColors, value.float("start_x"),
                value.float("end_x", Float.POSITIVE_INFINITY), tileMode,
            )
            "VERTICAL_GRADIENT" -> if (colorStops != null) {
                Brush.verticalGradient(
                    *colorStops, startY = value.float("start_y"),
                    endY = value.float("end_y", Float.POSITIVE_INFINITY), tileMode = tileMode,
                )
            } else Brush.verticalGradient(
                gradientColors, value.float("start_y"),
                value.float("end_y", Float.POSITIVE_INFINITY), tileMode,
            )
            "RADIAL_GRADIENT" -> if (colorStops != null) {
                Brush.radialGradient(
                    *colorStops, center = value["center"]?.let(::point) ?: Offset.Unspecified,
                    radius = value.float("radius", Float.POSITIVE_INFINITY), tileMode = tileMode,
                )
            } else Brush.radialGradient(
                gradientColors, value["center"]?.let(::point) ?: Offset.Unspecified,
                value.float("radius", Float.POSITIVE_INFINITY), tileMode,
            )
            "SWEEP_GRADIENT" -> if (colorStops != null) {
                Brush.sweepGradient(
                    *colorStops, center = value["center"]?.let(::point) ?: Offset.Unspecified,
                )
            } else Brush.sweepGradient(
                gradientColors, value["center"]?.let(::point) ?: Offset.Unspecified,
            )
            else -> null
        }
    }

    private fun gradientStops(
        value: LuaValueAccessor?,
        colors: Map<String, Color>,
    ): Array<out Pair<Float, Color>>? {
        if (value == null) return null
        return (1..(value.arraySize ?: 0)).mapNotNull { index ->
            val stop = value[index] ?: return@mapNotNull null
            val color = resolveLuaColor(stop["color"], colors) ?: return@mapNotNull null
            stop.float("offset") to color
        }.toTypedArray()
    }

    private fun colors(value: LuaValueAccessor?, colors: Map<String, Color>): List<Color> {
        if (value == null) return emptyList()
        return (1..(value.arraySize ?: 0)).mapNotNull { resolveLuaColor(value[it], colors) }
    }

    private fun tileMode(value: String?) = when (value) {
        "repeated" -> TileMode.Repeated
        "mirror" -> TileMode.Mirror
        "decal" -> TileMode.Decal
        else -> TileMode.Clamp
    }

    private fun colorFilter(
        value: LuaValueAccessor?,
        colors: Map<String, Color>,
    ): ColorFilter? = when (value?.get("type")?.asString()) {
        "TINT" -> resolveLuaColor(value["color"], colors)?.let {
            ColorFilter.tint(it, blendMode(value["blend_mode"]?.asString(), BlendMode.SrcIn))
        }
        "COLOR_MATRIX" -> floatArray(value["values"], 20)?.let {
            ColorFilter.colorMatrix(ColorMatrix(it))
        }
        "LIGHTING" -> {
            val multiply = resolveLuaColor(value["multiply"], colors)
            val add = resolveLuaColor(value["add"], colors)
            if (multiply != null && add != null) ColorFilter.lighting(multiply, add) else null
        }
        else -> null
    }

    private fun pathEffect(
        value: LuaValueAccessor?,
        colors: Map<String, Color>,
    ): PathEffect? = when (value?.get("type")?.asString()) {
        "CORNER" -> PathEffect.cornerPathEffect(value.float("radius"))
        "DASH" -> floatArray(value["intervals"])?.let {
            PathEffect.dashPathEffect(it, value.float("phase"))
        }
        "CHAIN" -> {
            val outer = pathEffect(value["outer"], colors)
            val inner = pathEffect(value["inner"], colors)
            if (outer != null && inner != null) PathEffect.chainPathEffect(outer, inner) else null
        }
        "STAMPED" -> PathEffect.stampedPathEffect(
            shape = path(value["commands"]),
            advance = value.float("advance"),
            phase = value.float("phase"),
            style = when (value["style"]?.asString()) {
                "rotate" -> StampedPathEffectStyle.Rotate
                "morph" -> StampedPathEffectStyle.Morph
                else -> StampedPathEffectStyle.Translate
            },
        )
        else -> null
    }

    private fun matrix(value: LuaValueAccessor?): Matrix =
        floatArray(value, 16)?.let(::Matrix) ?: Matrix()

    private fun floatArray(value: LuaValueAccessor?, requiredSize: Int? = null): FloatArray? {
        val size = value?.arraySize ?: return null
        if (requiredSize != null && size != requiredSize) return null
        return FloatArray(size) { index -> value[index + 1]?.asDouble()?.toFloat() ?: 0f }
    }

    private fun blendMode(value: String?, default: BlendMode = BlendMode.SrcOver) = when (value) {
        "clear" -> BlendMode.Clear
        "src" -> BlendMode.Src
        "dst" -> BlendMode.Dst
        "dst_over" -> BlendMode.DstOver
        "src_in" -> BlendMode.SrcIn
        "dst_in" -> BlendMode.DstIn
        "src_out" -> BlendMode.SrcOut
        "dst_out" -> BlendMode.DstOut
        "src_atop" -> BlendMode.SrcAtop
        "dst_atop" -> BlendMode.DstAtop
        "xor" -> BlendMode.Xor
        "plus" -> BlendMode.Plus
        "modulate" -> BlendMode.Modulate
        "screen" -> BlendMode.Screen
        "overlay" -> BlendMode.Overlay
        "darken" -> BlendMode.Darken
        "lighten" -> BlendMode.Lighten
        "color_dodge" -> BlendMode.ColorDodge
        "color_burn" -> BlendMode.ColorBurn
        "hard_light" -> BlendMode.Hardlight
        "soft_light" -> BlendMode.Softlight
        "difference" -> BlendMode.Difference
        "exclusion" -> BlendMode.Exclusion
        "multiply" -> BlendMode.Multiply
        "hue" -> BlendMode.Hue
        "saturation" -> BlendMode.Saturation
        "color" -> BlendMode.Color
        "luminosity" -> BlendMode.Luminosity
        "src_over" -> BlendMode.SrcOver
        else -> default
    }

    private fun LuaValueAccessor.float(key: String, default: Float = 0f) =
        this[key]?.asDouble()?.toFloat() ?: default

}

internal fun textConstraints(requestedSize: Size?): Constraints {
    if (requestedSize == null) return Constraints()
    val width = requestedSize.width.toInt().coerceAtLeast(0)
    return Constraints(
        minWidth = width,
        maxWidth = width,
        maxHeight = requestedSize.height.toInt().coerceAtLeast(0),
    )
}

internal fun verticalTextOffset(
    containerHeight: Float?,
    textHeight: Int,
    alignment: String?,
): Float {
    if (containerHeight == null) return 0f
    val remainingHeight = containerHeight - textHeight
    return when (alignment) {
        "center" -> remainingHeight / 2f
        "bottom" -> remainingHeight
        else -> 0f
    }
}
