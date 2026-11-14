local core = require("tng.core")

local days_per_week = 7

local data_vis_color_count = 12
local data_vis_color_generator = 7

local function date_key(date)
	return string.format("%04d-%02d-%02d", date.year, date.month, date.day)
end

local function start_of_day(date)
	return {
		year = date.year,
		month = date.month,
		day = date.day,
		hour = 0,
		min = 0,
		sec = 0,
		zone = date.zone,
	}
end

local function ordered_sources(sources)
	local result = {}
	for _, source in pairs(sources) do
		table.insert(result, source)
	end
	table.sort(result, function(left, right)
		if left.index == right.index then
			return left.name < right.name
		end
		return left.index < right.index
	end)
	return result
end

local function calendar_days(today, weeks)
	local start = core.shift(
		start_of_day(today),
		core.PERIOD.DAY,
		-((weeks - 1) * days_per_week + today.wday - 1)
	)
	local today_index = (weeks - 1) * days_per_week + today.wday
	local days = {}
	local weekday_labels = {}
	local month_labels = {}

	for index = 1, weeks * days_per_week do
		local date = core.date(core.shift(start, core.PERIOD.DAY, index - 1))
		local week = math.floor((index - 1) / days_per_week) + 1
		local weekday = ((index - 1) % days_per_week) + 1
		local key = date_key(date)
		days[index] = {
			key = key,
			week = week,
			weekday = weekday,
			future = index > today_index,
			month = core.format(date, "MMM"),
		}

		if week == 1 then
			weekday_labels[weekday] = core.format(date, "EEE")
		end
		-- Month headings mark the week containing the 1st, even when its Monday
		-- belongs to the previous month. Also label the initial partial month.
		if index == 1 or date.day == 1 then
			month_labels[week] = core.format(date, "MMM")
		end
	end

	return { days = days, weekday_labels = weekday_labels, month_labels = month_labels }
end

local function read_source(source, configuration, color)
	local values = {}
	local nonzero = {}
	-- Consume incrementally and retain only daily aggregates, so a wider canvas
	-- can reveal older dates without imposing a fixed maximum number of weeks.
	while true do
		local data_point = source.dp()
		if data_point == nil then break end
		local key = date_key(core.date(data_point))
		local value = data_point.value
		values[key] = (values[key] or 0) + value
		if value ~= 0 then nonzero[key] = true end
	end

	local mode = configuration and configuration.mode or "BINARY"
	local minimum = tonumber(configuration and configuration.minimum) or 0
	local maximum = tonumber(configuration and configuration.maximum) or 100
	for index, value in pairs(values) do
		if mode == "RANGE" and maximum > minimum then
			values[index] = math.max(0, math.min(1, (value - minimum) / (maximum - minimum)))
		else
			values[index] = nonzero[index] and 1 or 0
		end
	end

	return {
		name = source.name,
		color = color,
		values = values,
	}
end

return function(params)
	local sources = params.sources
	local state = params.state
	local today = core.date()
	local source_data = {}

	for index, source in ipairs(ordered_sources(sources)) do
		local configured_source = state and state.sources and state.sources[index]
		local color = configured_source and configured_source.color
			or ((index - 1) * data_vis_color_generator) % data_vis_color_count
		source_data[index] = read_source(source, configured_source, color)
	end

	return {
		-- Invoked under the retained VM lock once the canvas knows its width.
		calendar = function(weeks)
			return calendar_days(today, weeks)
		end,
		sources = source_data,
	}
end
