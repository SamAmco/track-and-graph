#!/usr/bin/env lua
package.path = package.path .. ";src/?.lua;src/?/init.lua"
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local core = require("tng.core")
local prepare = dofile("src/examples/calendar-heatmap/data.lua")
local render = dofile("src/examples/calendar-heatmap/renderer.lua")

-- Deterministic civil-date fixture for exercising the package under ordinary Lua.
local today = os.time({ year = 2026, month = 11, day = 14, hour = 12 })
core.date = function(value)
	local date = os.date("*t", type(value) == "table" and value.timestamp or value or today)
	date.min, date.sec = 0, 0
	date.wday = (date.wday + 5) % 7 + 1
	return date
end
core.shift = function(value, _, amount)
	local date = type(value) == "table" and value.year and value or core.date(value)
	return os.time({ year = date.year, month = date.month, day = date.day + amount, hour = 12 })
end
core.format = function(date, pattern)
	local timestamp = type(date) == "number" and date
		or os.time({ year = date.year, month = date.month, day = date.day, hour = 12 })
	return os.date(pattern == "MMM" and "%b" or "%a", timestamp)
end

local function source(values)
	local points = {}
	for i, value in ipairs(values) do points[i] = { timestamp = today, value = value } end
	local index = 0
	return { name = "Source", index = 1, dp = function() index = index + 1; return points[index] end }
end

test("binary tests individual nonzero points and range scales signed daily sums", function()
	local function intensity(values, config)
		local data = prepare({ sources = { source_1 = source(values) }, state = { sources = { config } } })
		return data.sources[1].values["2026-11-14"]
	end
	assert(intensity({ 0, 0 }, { mode = "BINARY" }) == 0)
	assert(intensity({ -5, 5 }, { mode = "BINARY" }) == 1)
	local range = { mode = "RANGE", minimum = { text = "-3" }, maximum = { text = "5" } }
	assert(intensity({ -2, 3 }, range) == 0.5)
	assert(intensity({ -20 }, range) == 0)
	assert(intensity({ 20 }, range) == 1)
end)

test("month labels belong to the week containing the first, not its first Monday", function()
	local calendar = prepare({ sources = {}, state = {} }).calendar(53)
	local september_week
	for _, day in ipairs(calendar.days) do
		if day.key == "2026-09-01" then
			september_week = day.week
			assert(day.weekday == 2)
		end
		if day.key:sub(-2) == "01" then
			assert(calendar.month_labels[day.week] == day.month)
		end
	end
	assert(calendar.days[(september_week - 1) * 7 + 1].key == "2026-08-31")
	assert(calendar.month_labels[september_week] == "Sep")
	assert(calendar.month_labels[september_week + 1] == nil)
end)

test("calendar squares retain size and one-pixel gaps while revealing older weeks on wide screens", function()
	local data = prepare({ sources = {}, state = {} })
	local function rectangles(width)
		local result = render({ context = { density = 2, font_scale = 1 }, data = data })
		local recording = result.ui.children[1].draw({ bounds = { width = width, height = 240 }, density = 2 })
		local rects = {}
		for _, instruction in ipairs(recording.instructions) do
			if instruction.type == "RECT" then rects[#rects + 1] = instruction end
		end
		return rects
	end
	local narrow, wide = rectangles(400), rectangles(1000)
	assert(#wide > #narrow and #narrow % 7 == 0)
	assert(narrow[1].size.width == 28 and wide[1].size.width == 28)
	assert(narrow[2].top_left.y - narrow[1].top_left.y == 29)
	assert(narrow[8].top_left.x - narrow[1].top_left.x == 29)
	assert(narrow[#narrow].top_left.x + 28 == 400)
	assert(wide[#wide].top_left.x + 28 == 1000)
end)

test("overlapping colours average scaled results independently of source order", function()
	local data = prepare({ sources = {}, state = {} })
	local function composite(sources)
		data.sources = sources
		local result = render({ context = { density = 1 }, data = data })
		local instructions = result.ui.children[1].draw({ bounds = { width = 400, height = 140 }, density = 1 }).instructions
		local rgb = { 0, 0, 0 }
		local colors = { RED = { 1, 0, 0 }, BLUE = { 0, 0, 1 }, THEME_DATA_VIS_BLANK = { 0, 0, 0 } }
		-- Find today's tile, then replay ordinary alpha compositing for that tile.
		local target
		for _, item in ipairs(instructions) do
			if item.type == "RECT" and item.color == "RED" then target = item.top_left; break end
		end
		for _, item in ipairs(instructions) do
			if item.type == "RECT" and item.top_left.x == target.x and item.top_left.y == target.y then
				local color = colors[item.color]
				for c = 1, 3 do rgb[c] = rgb[c] * (1 - item.alpha) + color[c] * item.alpha end
			end
		end
		return rgb
	end
	local red = { name = "Red", color = "RED", values = { ["2026-11-14"] = 1 } }
	local blue = { name = "Blue", color = "BLUE", values = { ["2026-11-14"] = 0.5 } }
	local inactive = { name = "Inactive", color = "BLUE", values = {} }
	local a, b = composite({ red, blue, inactive }), composite({ blue, red })
	assert(math.abs(a[1] - 0.5) < 1e-6 and math.abs(a[3] - 0.25) < 1e-6)
	for c = 1, 3 do assert(math.abs(a[c] - b[c]) < 1e-6) end
end)

helpers.finish_tests()
