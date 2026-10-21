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
	local index_by_date = {}
	local weekday_labels = {}
	local month_labels = {}
	local previous_month = nil

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
		}
		index_by_date[key] = index

		if week == 1 then
			weekday_labels[weekday] = core.format(date, "EEEEE")
		end
		if weekday == 1 then
			if date.month ~= previous_month then
				month_labels[week] = core.format(date, "MMM")
			else
				month_labels[week] = ""
			end
			previous_month = date.month
		end
	end

	return days, index_by_date, weekday_labels, month_labels, start, today_index
end

local function read_source(source, source_index, index_by_date, start)
	local values = {}
	local point_counts = {}
	local maximum = 0
	local cutoff = core.shift(start, core.PERIOD.DAY, -1)

	for _, data_point in ipairs(source.dpafter(cutoff)) do
		local index = index_by_date[date_key(core.date(data_point))]
		if index ~= nil then
			local value = math.abs(data_point.value)
			values[index] = (values[index] or 0) + value
			point_counts[index] = (point_counts[index] or 0) + 1
		end
	end

	for index, count in pairs(point_counts) do
		if values[index] == 0 and count > 0 then
			values[index] = 1
		end
		maximum = math.max(maximum, values[index])
	end

	if maximum > 0 then
		for index, value in pairs(values) do
			values[index] = value / maximum
		end
	end

	return {
		name = source.name,
		color = ((source_index - 1) * data_vis_color_generator) % data_vis_color_count,
		values = values,
	}
end

return function(sources, state)
	local weeks = state and state.weeks or 53
	local today = core.date()
	local days, index_by_date, weekday_labels, month_labels, start, today_index =
		calendar_days(today, weeks)
	local source_data = {}

	for index, source in ipairs(ordered_sources(sources)) do
		source_data[index] = read_source(source, index, index_by_date, start)
	end

	return {
		weeks = weeks,
		days = days,
		weekday_labels = weekday_labels,
		month_labels = month_labels,
		today_index = today_index,
		sources = source_data,
	}
end
