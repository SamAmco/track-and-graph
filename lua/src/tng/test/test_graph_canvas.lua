#!/usr/bin/env lua

local canvas = require("src.tng.canvas")
local ui = require("src.tng.ui")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("canvas records Compose-like draw calls in paint order", function()
	local start = { x = 10, y = 20 }
	local render = canvas.create(function(drawScope)
		assert(drawScope.size.width == 200)
		assert(drawScope.size.height == 100)
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
	assert(type(render) == "function")
	local result = render({ bounds = { width = 200, height = 100 } })

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

test("custom graph example renders a canvas component", function()
	package.path = package.path .. ";src/?.lua;src/?/init.lua"
	local script = dofile("examples/canvas-basic-graph.lua")
	local data_points = {
		{ value = 30 },
		{ value = 20 },
		{ value = 10 },
	}
	local index = 0
	assert(script.version == "2.0.0")
	assert(script.type == ui.TYPE)

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
		constraints = {
			min_width = 0,
			max_width = 1000,
			min_height = 0,
			max_height = 600,
		},
		density = 3,
		font_scale = 1,
		layout_direction = "LTR",
		theme = "LIGHT",
	})

	-- Rendering uses the gathered graph data and does not touch the source again.
	assert(index == 4)
	assert(result.type == ui.COMPONENT_TYPE.SURFACE)
	assert(result.key == "graph_surface")
	assert(result.shape == "SHAPE_MEDIUM")
	assert(result.shadow_elevation.unit == ui.UNIT.DP)
	assert(result.shadow_elevation.value == 4)

	local column = result.child
	assert(column.type == ui.COMPONENT_TYPE.COLUMN)
	assert(column.spacing == "SPACING_LARGE")
	assert(#column.children == 3)
	assert(column.children[1].type == ui.COMPONENT_TYPE.TEXT)
	assert(column.children[1].text == "Basic graph")
	assert(column.children[1].font_size.unit == ui.UNIT.SP)
	assert(column.children[2].type == ui.COMPONENT_TYPE.ROW)
	assert(#column.children[2].children == 2)

	local canvas_component = column.children[3]
	assert(canvas_component.type == canvas.TYPE)
	assert(canvas_component.key == "graph_canvas")
	assert(canvas_component.modifier[2].type == ui.MODIFIER_TYPE.HEIGHT)
	assert(canvas_component.modifier[2].value.value == 360)
	assert(type(canvas_component.draw) == "function")
	local canvas_result = canvas_component.draw({
		bounds = { width = 1000, height = 360 },
	})
	-- Two axes + two connecting lines + three point markers.
	assert(#canvas_result.instructions == 7)
	assert(canvas_result.instructions[3].type == canvas.INSTRUCTION_TYPE.LINE)
	assert(canvas_result.instructions[5].type == canvas.INSTRUCTION_TYPE.CIRCLE)
	-- Source values arrive newest-first; the example reverses them for left-to-right time order.
	assert(canvas_result.instructions[5].center.y > canvas_result.instructions[7].center.y)
end)

helpers.finish_tests()
