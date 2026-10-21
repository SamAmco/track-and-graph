#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local canvas = require("tng.canvas")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("canvas units convert to physical pixels", function()
	local render = canvas.create(function(scope)
		scope:drawCircle({
			color = "#ffffff",
			radius = scope:dp(3),
			center = { x = scope:sp(9), y = scope:sp(-120) },
		})
	end)

	local result = render({
		bounds = { width = 100, height = 50 },
		density = 2,
		font_scale = 1.5,
		sp_to_px = {
			{ sp = 8, px = 20 },
			{ sp = 10, px = 24 },
			{ sp = 100, px = 160 },
		},
	})
	local instruction = result.instructions[1]
	assert(instruction.radius == 6)
	assert(instruction.center.x == 22)
	assert(instruction.center.y == -192)
end)

test("canvas sp conversion falls back to linear scaling", function()
	local render = canvas.create(function(scope)
		scope:drawCircle({ color = "#ffffff", radius = scope:sp(4) })
	end)
	local result = render({
		bounds = { width = 100, height = 50 },
		density = 2,
		font_scale = 1.5,
	})
	assert(result.instructions[1].radius == 12)
end)

test("recorded paths do not change when their source path changes", function()
	local path = canvas.path():moveTo(1, 2):lineTo(3, 4)
	local render = canvas.create(function(scope)
		scope:drawPath({ color = "#ffffff", path = path })
		path.commands[1].x = 99
		path:lineTo(5, 6)
	end)

	local result = render({ bounds = { width = 100, height = 50 } })
	local commands = result.instructions[1].commands
	assert(#commands == 2)
	assert(commands[1].x == 1)
end)

test("path builder records every supported command", function()
	local size = { width = 10, height = 20 }
	local nested = canvas.path():addRect({ size = size })
	local matrix = {
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1,
	}
	local path = canvas.path()
		:moveTo(1, 2)
		:relativeMoveTo(3, 4)
		:lineTo(5, 6)
		:relativeLineTo(7, 8)
		:quadraticTo(1, 2, 3, 4)
		:relativeQuadraticTo(1, 2, 3, 4)
		:cubicTo(1, 2, 3, 4, 5, 6)
		:relativeCubicTo(1, 2, 3, 4, 5, 6)
		:arcTo({ size = size, start_angle = 0, sweep_angle = 90 })
		:arcToRad({ size = size, start_angle = 0, sweep_angle = 1 })
		:addRect({ size = size })
		:addOval({ size = size })
		:addRoundRect({ size = size, corner_radius = { x = 2, y = 3 } })
		:addArc({ size = size, start_angle = 0, sweep_angle = 90 })
		:addArcRad({ size = size, start_angle = 0, sweep_angle = 1 })
		:addPath(nested, { x = 2, y = 3 })
		:translate({ x = 4, y = 5 })
		:transform(matrix)
		:close()

	local expected = {
		"MOVE_TO", "RELATIVE_MOVE_TO", "LINE_TO", "RELATIVE_LINE_TO",
		"QUADRATIC_TO", "RELATIVE_QUADRATIC_TO", "CUBIC_TO", "RELATIVE_CUBIC_TO",
		"ARC_TO", "ARC_TO_RAD", "ADD_RECT", "ADD_OVAL", "ADD_ROUND_RECT",
		"ADD_ARC", "ADD_ARC_RAD", "ADD_PATH", "TRANSLATE", "TRANSFORM", "CLOSE",
	}
	assert(#path.commands == #expected)
	for index, command_type in ipairs(expected) do
		assert(path.commands[index].type == command_type)
	end
end)

test("nested canvas operations retain their order and children", function()
	local render = canvas.create(function(scope)
		scope:withTransform({
			canvas.Transform.translate({ left = 2, top = 3 }),
			canvas.Transform.rotate({ degrees = 45, pivot = { x = 10, y = 20 } }),
		}, function(transformed)
			transformed:clipRect({
				left = 1,
				top = 2,
				right = 30,
				bottom = 40,
				clip_op = canvas.CLIP_OP.DIFFERENCE,
			}, function(clipped)
				clipped:drawCircle({ color = "#ffffff", radius = 5 })
			end)
		end)
	end)

	local result = render({ bounds = { width = 100, height = 50 } })
	local transformed = result.instructions[1]
	assert(transformed.type == canvas.INSTRUCTION_TYPE.GROUP)
	assert(transformed.operation.type == "WITH_TRANSFORM")
	assert(transformed.operation.operations[1].type == "TRANSLATE")
	assert(transformed.operation.operations[2].type == "ROTATE")
	local clipped = transformed.instructions[1]
	assert(clipped.operation.type == "CLIP_RECT")
	assert(clipped.operation.clip_op == canvas.CLIP_OP.DIFFERENCE)
	assert(clipped.instructions[1].type == canvas.INSTRUCTION_TYPE.CIRCLE)
end)

test("text options are recorded without losing layout metadata", function()
	local render = canvas.create(function(scope)
		scope:drawText({
			text = "Canvas text",
			color = "#ffffff",
			top_left = { x = 2, y = 3 },
			size = { width = 80, height = 20 },
			font_size = scope:sp(12),
			font_weight = canvas.FONT_WEIGHT.BOLD,
			text_align = canvas.TEXT_ALIGN.CENTER,
			overflow = canvas.TEXT_OVERFLOW.ELLIPSIS,
			max_lines = 2,
			soft_wrap = false,
		})
	end)

	local result = render({
		bounds = { width = 100, height = 50 },
		sp_to_px = { { sp = 12, px = 30 } },
	})
	local instruction = result.instructions[1]
	assert(instruction.text == "Canvas text")
	assert(instruction.top_left.x == 2 and instruction.top_left.y == 3)
	assert(instruction.size.width == 80 and instruction.size.height == 20)
	assert(instruction.font_size == 30)
	assert(instruction.font_weight == canvas.FONT_WEIGHT.BOLD)
	assert(instruction.text_align == canvas.TEXT_ALIGN.CENTER)
	assert(instruction.overflow == canvas.TEXT_OVERFLOW.ELLIPSIS)
	assert(instruction.max_lines == 2)
	assert(instruction.soft_wrap == false)
end)

test("text vertical alignment is recorded", function()
	local render = canvas.create(function(scope)
		scope:drawText({
			text = "Centered",
			color = "#ffffff",
			size = { width = 100, height = 40 },
			vertical_align = canvas.TEXT_VERTICAL_ALIGN.CENTER,
		})
	end)

	local result = render({ bounds = { width = 100, height = 40 } })
	assert(result.instructions[1].vertical_align == "center")
end)

test("text vertical alignment defaults to top", function()
	local render = canvas.create(function(scope)
		scope:drawText({ text = "Top", color = "#ffffff" })
	end)

	local result = render({ bounds = { width = 100, height = 40 } })
	assert(result.instructions[1].vertical_align == "top")
end)

helpers.finish_tests()
