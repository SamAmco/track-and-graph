#!/usr/bin/env lua
package.path = "src/examples/calendar-heatmap/?.lua;" .. package.path .. ";src/?.lua;src/?/init.lua"
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local core = require("tng.core")
local prepare = dofile("src/examples/calendar-heatmap/data.lua")
local render = dofile("src/examples/calendar-heatmap/renderer.lua")
local month_layout = require("month_layout")

-- Deterministic civil-date fixture for exercising the package under ordinary Lua.
local today = os.time({ year = 2026, month = 11, day = 14, hour = 12 })
core.time = function(date)
	return { timestamp = date and os.time({ year = date.year, month = date.month, day = date.day,
		hour = date.hour or 0, min = date.min or 0, sec = date.sec or 0 }) or today }
end
core.date = function(value)
	local timestamp = type(value) == "table" and (value.timestamp or core.time(value).timestamp) or value or today
	local date = os.date("*t", timestamp)
	date.min, date.sec = 0, 0
	date.wday = (date.wday + 5) % 7 + 1
	return date
end
core.shift = function(value, _, amount)
	local date = type(value) == "table" and value.year and value or core.date(value)
	return core.time({ year = date.year, month = date.month, day = date.day + amount,
		hour = date.hour or 0, min = date.min or 0, sec = date.sec or 0 })
end

local function source_points(points)
	local index = 0
	return { name = "Source", index = 1, dp = function() index = index + 1; return points[index] end }
end
core.format = function(date, pattern)
	local timestamp = type(date) == "number" and date
		or os.time({ year = date.year, month = date.month, day = date.day, hour = 12 })
	return os.date(pattern == "MMMM" and "%B" or pattern == "MMM" and "%b" or "%a", timestamp)
end

local function source(values)
	local points = {}
	for i, value in ipairs(values) do points[i] = { timestamp = today, value = value } end
	local index = 0
	return { name = "Source", index = 1, dp = function() index = index + 1; return points[index] end }
end

local function sections(result)
	return result.ui.children[1].children[1].children
end

local function rows(section)
	return section.children[#section.children].children
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
			if day.month_number == 1 then assert(calendar.year_labels[day.week] == tostring(day.year)) end
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
	assert(narrow[1].top_left.x >= 80 and wide[1].top_left.x >= 80) -- weekday gutter on the left
	assert(400 - narrow[#narrow].top_left.x - 28 < 29)
	assert(1000 - wide[#wide].top_left.x - 28 < 29)
end)

test("full screen quantizes rows to whole chronological months within newest-first years", function()
	local data = prepare({ sources = { source_1 = source_points({
		{ timestamp = today, value = 1 },
		{ timestamp = core.time({ year = 2024, month = 1, day = 1 }).timestamp, value = 1 },
	}) }, state = {} })
	local result = render({ context = { density = 2, constraints = { max_width = 764 } },
		view_mode = "FULL_SCREEN", data = data })
	local years = sections(result)
	assert(#years == 3)
	assert(result.ui.modifier[2].type == "FILL_HEIGHT")
	assert(result.ui.children[1].modifier[2].type == "WEIGHT")
	for index, year in ipairs({ "2026", "2025", "2024" }) do
		assert(years[index].children[1].text == year)
		assert(years[index].children[1].font_weight == "BOLD")
		local geometry = month_layout(data.years[index].months, 700, 2, 1, index == 1)
		assert(#rows(years[index]) == #geometry.rows)
		local next_month = 1
		for _, row in ipairs(geometry.rows) do
			assert(row.width <= 700)
			for _, entry in ipairs(row.months) do
				assert(entry.month.month == next_month)
				assert(entry.month == data.years[index].months[next_month])
				next_month = next_month + 1
			end
		end
		assert(next_month == (index == 1 and 12 or 13))
	end
	local single = render({ context = { density = 2 }, view_mode = "FULL_SCREEN",
		data = prepare({ sources = { source_1 = source({ 1 }) }, state = {} }) })
	assert(#sections(single) == 1 and #sections(single)[1].children == 1)
	assert(#rows(sections(single)[1]) == 1)
end)

test("month blocks contain each date once, including leap days and year boundaries", function()
	local data = prepare({ sources = { source_1 = source_points({
		{ timestamp = today, value = 1 },
		{ timestamp = core.time({ year = 2023, month = 1, day = 1 }).timestamp, value = 1 },
	}) }, state = {} })
	local seen = {}
	for _, section in ipairs(data.years) do
		for _, month in ipairs(section.months) do
			for _, day in ipairs(month.days) do
				assert(not seen[day.key])
				assert(tonumber(day.key:sub(1, 4)) == section.year)
				assert(tonumber(day.key:sub(6, 7)) == month.month)
				assert(day.weekday >= 1 and day.weekday <= 7 and day.week <= 6)
				if not day.future then seen[day.key] = true end
			end
		end
	end
	assert(seen["2023-01-01"] and seen["2024-02-29"] and seen["2025-01-01"])
	assert(seen["2026-11-14"] and not seen["2026-11-15"])
end)

test("tiles have one-pixel gaps within months and five-pixel gaps at all month boundaries", function()
	local data = prepare({ sources = { source_1 = source_points({
		{ timestamp = today, value = 1 },
		{ timestamp = core.time({ year = 2024, month = 1, day = 1 }).timestamp, value = 1 },
	}) }, state = {} })
	local boundaries = {}
	for _, year in ipairs(data.years) do
		local geometry = month_layout(year.months, 2000, 2, 1, false)
		assert(geometry.tile == 28)
		for _, row in ipairs(geometry.rows) do
			local weekdays = { {}, {}, {}, {}, {}, {}, {} }
			for _, entry in ipairs(row.months) do
				boundaries[entry.month.days[1].weekday] = true
				for _, day in ipairs(entry.month.days) do
					table.insert(weekdays[day.weekday], {
						x = entry.x + (day.week - 1) * geometry.step, month = entry.month.month,
					})
				end
			end
			for _, tiles in ipairs(weekdays) do
				for index = 2, #tiles do
					local previous, current = tiles[index - 1], tiles[index]
					local gap = current.x - previous.x - geometry.tile
					assert(gap == (current.month == previous.month and 1 or 5))
				end
			end
		end
	end
	for weekday = 1, 7 do assert(boundaries[weekday]) end -- includes exact Monday boundaries
end)

test("continuous month rows have abbreviated headings, no digits and weekdays only once", function()
	local data = prepare({ sources = { source_1 = source_points({
		{ timestamp = today, value = 1 },
		{ timestamp = core.time({ year = 2025, month = 1, day = 1 }).timestamp, value = 1 },
	}) }, state = {} })
	local result = render({ context = { density = 2, constraints = { max_width = 764 } },
		view_mode = "FULL_SCREEN", data = data })
	local weekdays = { Mon = true, Tue = true, Wed = true, Thu = true, Fri = true, Sat = true, Sun = true }
	local weekday_count, heading_count, tiles = 0, 0, 0
	for _, section in ipairs(sections(result)) do
		for _, row in ipairs(rows(section)) do
			local recording = row.draw({ bounds = { width = 700, height = 250 }, density = 2 })
			for _, item in ipairs(recording.instructions) do
				if item.type == "TEXT" then
					assert(tonumber(item.text) == nil)
					if weekdays[item.text] then weekday_count = weekday_count + 1
					else heading_count = heading_count + 1 end
				elseif item.type == "RECT" then
					assert(item.size.width == 28 and item.size.height == 28)
					assert(item.top_left.x >= 0 and item.top_left.x + 28 <= 700)
					assert(item.top_left.y >= 48 and item.top_left.y + 28 <= 250)
					if item.color == "THEME_DATA_VIS_BLANK" then tiles = tiles + 1 end
				end
			end
		end
	end
	assert(weekday_count == 7 and heading_count == 23)
	assert(tiles == 365 + 334) -- complete months through November, including future blank days
end)

test("whole months fit narrow screens and larger font scales", function()
	local data = prepare({ sources = {}, state = {} })
	for _, width in ipairs({ 200, 700 }) do
		for _, scale in ipairs({ 1, 1.5 }) do
			local geometry = month_layout(data.years[1].months, width, 2, scale, true)
			assert(#geometry.rows == 1 and geometry.rows[1].width <= width)
			assert(geometry.tile >= 1)
		end
	end
end)

test("all input history determines the number of weeks including zero-valued days", function()
	local old = core.time({ year = 2024, month = 1, day = 1, hour = 12 }).timestamp
	local data = prepare({ sources = { source_1 = source_points({
		{ timestamp = today, value = 1 }, { timestamp = old, value = 0 },
	}) }, state = {} })
	assert(data.total_weeks > 140)
	assert(data.calendar(data.total_weeks).days[1].key == "2024-01-01")
	assert(#data.years == 3 and #data.years[3].months == 12)
end)

test("sampling endpoint choices filter input and anchor the calendar consistently", function()
	local old = core.time({ year = 2026, month = 9, day = 1, hour = 12 }).timestamp
	local future = core.time({ year = 2026, month = 12, day = 1, hour = 12 }).timestamp
	local function sampled(state)
		return prepare({ sources = { source_1 = source_points({
			{ timestamp = future, value = 1 }, { timestamp = today, value = 1 }, { timestamp = old, value = 1 },
		}) }, state = state })
	end
	local latest = sampled({})
	assert(latest.sources[1].values["2026-12-01"] == 1)
	assert(latest.calendar(1).days[2].key == "2026-12-01")
	local now = sampled({ sample_ending_at = "NOW" })
	assert(now.sources[1].values["2026-12-01"] == nil)
	assert(now.calendar(1).days[6].key == "2026-11-14")
	local custom = sampled({ sample_ending_at = "CUSTOM", sample_end_date = { year = 2026, month = 9, day = 1 } })
	assert(custom.sources[1].values["2026-09-01"] == 1)
	assert(custom.sources[1].values["2026-11-14"] == nil)
	assert(custom.calendar(1).days[2].key == "2026-09-01")
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
