#!/usr/bin/env lua

local canvas = require("src.tng.canvas")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("canvas records Compose-like draw calls in paint order", function()
	local start = { x = 10, y = 20 }
	local result = canvas.create(function(drawScope)
		drawScope:drawLine({
			color = "#112233",
			start = start,
			finish = { x = 30, y = 40 },
			stroke_width = 5,
			cap = canvas.STROKE_CAP.ROUND,
		})
		drawScope:drawCircle({
			color = "#445566",
			center = { x = 100, y = 50 },
			radius = 25,
		})
	end)

	start.x = 999
	assert(result.type == canvas.TYPE)
	assert(#result.instructions == 2)

	local line = result.instructions[1]
	assert(line.type == canvas.INSTRUCTION_TYPE.LINE)
	assert(line.start.x == 10)
	assert(line.finish.y == 40)
	assert(line.stroke_width == 5)
	assert(line.cap == canvas.STROKE_CAP.ROUND)
	assert(line.alpha == 1)

	local circle = result.instructions[2]
	assert(circle.type == canvas.INSTRUCTION_TYPE.CIRCLE)
	assert(circle.center.x == 100)
	assert(circle.center.y == 50)
	assert(circle.radius == 25)
	assert(circle.style == canvas.DRAW_STYLE.FILL)
end)

test("canvas graph example converts source values into axes, lines, and points", function()
	package.path = package.path .. ";src/?.lua;src/?/init.lua"
	local script = dofile("examples/canvas-basic-graph.lua")
	local data_points = {
		{ value = 30 },
		{ value = 20 },
		{ value = 10 },
	}
	local index = 0
	assert(script.version == "2.0.0")
	assert(script.type == canvas.TYPE)

	local graph_data = script.data({
		values = {
			dp = function()
				index = index + 1
				return data_points[index]
			end,
		},
	})
	assert(index == 4)
	assert(#graph_data.values == 3)

	local result = script.render(graph_data, {
		bounds = { width = 1000, height = 600 },
	})

	-- Rendering uses the gathered graph data and does not touch the source again.
	assert(index == 4)
	assert(result.type == canvas.TYPE)
	-- Two axes + two connecting lines + three point markers.
	assert(#result.instructions == 7)
	assert(result.instructions[3].type == canvas.INSTRUCTION_TYPE.LINE)
	assert(result.instructions[5].type == canvas.INSTRUCTION_TYPE.CIRCLE)
	-- Source values arrive newest-first; the example reverses them for left-to-right time order.
	assert(result.instructions[5].center.y > result.instructions[7].center.y)
end)

helpers.finish_tests()
