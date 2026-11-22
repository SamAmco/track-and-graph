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

local function calendar_days(today, weeks, year)
	local start = core.shift(
		start_of_day(today),
		core.PERIOD.DAY,
		-((weeks - 1) * days_per_week + today.wday - 1)
	)
	local today_index = (weeks - 1) * days_per_week + today.wday
	local days = {}
	local weekday_labels = {}
	local month_labels = {}
	local year_labels = {}

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
			year = date.year,
			month_number = date.month,
			outside_year = year ~= nil and date.year ~= year,
		}

		if week == 1 then
			weekday_labels[weekday] = core.format(date, "EEE")
		end
		-- Month headings mark the week containing the 1st, even when its Monday
		-- belongs to the previous month. Also label the initial partial month.
		if (year == nil or date.year == year) and (index == 1 or date.day == 1) then
			month_labels[week] = core.format(date, "MMM")
			if date.month == 1 then year_labels[week] = tostring(date.year) end
		end
	end

	return { days = days, weekday_labels = weekday_labels, month_labels = month_labels, year_labels = year_labels }
end

local function read_source(source, configuration, color, ending_at)
	local values = {}
	local nonzero = {}
	local earliest, latest
	-- Consume incrementally and retain only daily aggregates, so a wider canvas
	-- can reveal older dates without imposing a fixed maximum number of weeks.
	while true do
		local data_point = source.dp()
		if data_point == nil then break end
		if ending_at == nil or data_point.timestamp <= ending_at then
			local date = core.date(data_point)
			local key = date_key(date)
			local value = data_point.value
			values[key] = (values[key] or 0) + value
			if value ~= 0 then nonzero[key] = true end
			if earliest == nil or key < date_key(earliest) then earliest = date end
			if latest == nil or data_point.timestamp > latest.timestamp then latest = data_point end
		end
	end

	local mode = configuration and configuration.mode or "BINARY"
	local minimum = tonumber(configuration and configuration.minimum and configuration.minimum.text) or 0
	local maximum = tonumber(configuration and configuration.maximum and configuration.maximum.text) or 100
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
	}, earliest, latest
end

-- Civil-day arithmetic avoids treating DST weeks as a fixed number of milliseconds.
local function civil_day(date)
	local year = date.year - 1
	local day = 365 * year + math.floor(year / 4) - math.floor(year / 100) + math.floor(year / 400) + date.day
	local months = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 }
	for month = 1, date.month - 1 do day = day + months[month] end
	if date.month > 2 and date.year % 4 == 0 and (date.year % 100 ~= 0 or date.year % 400 == 0) then
		day = day + 1
	end
	return day
end

return function(params)
	local sources = params.sources
	local state = params.state
	local ending_at
	if state and state.sample_ending_at == "CUSTOM" and state.sample_end_date then
		ending_at = core.shift(start_of_day(state.sample_end_date), core.PERIOD.DAY, 1).timestamp - 1
	elseif state and state.sample_ending_at == "NOW" then
		ending_at = core.time().timestamp
	end
	local source_data = {}
	local earliest, latest

	for index, source in ipairs(ordered_sources(sources)) do
		local configured_source = state and state.sources and state.sources[index]
		local color = configured_source and configured_source.color
			or ((index - 1) * data_vis_color_generator) % data_vis_color_count
		local first, last
		source_data[index], first, last = read_source(source, configured_source, color, ending_at)
		if first and (earliest == nil or date_key(first) < date_key(earliest)) then earliest = first end
		if last and (latest == nil or last.timestamp > latest.timestamp) then latest = last end
	end
	local ending_date = core.date(ending_at or latest)
	local total_weeks = earliest and math.max(1, math.floor(
		(civil_day(ending_date) - civil_day(earliest) + earliest.wday - 1) / 7
	) + 1) or 1
	local years = {}
	for year = ending_date.year, (earliest or ending_date).year, -1 do
		local last = year == ending_date.year and ending_date
			or core.date(core.time({ year = year, month = 12, day = 31, hour = 0, min = 0, sec = 0 }))
		local months = {}
		local first_month = earliest and year == earliest.year and earliest.month or 1
		if earliest == nil then first_month = last.month end
		for month = first_month, last.month do
			local first_day = core.date(core.time({ year = year, month = month, day = 1 }))
			local next_month = core.date(core.time({ year = year + (month == 12 and 1 or 0),
				month = month == 12 and 1 or month + 1, day = 1 }))
			local days = {}
			for day = 1, civil_day(next_month) - civil_day(first_day) do
				local date = core.date(core.shift(first_day, core.PERIOD.DAY, day - 1))
				days[day] = { key = date_key(date), number = day, weekday = date.wday,
					week = math.floor((first_day.wday + day - 2) / 7) + 1,
					future = date_key(date) > date_key(ending_date) }
			end
			local weekdays = {}
			for day = 1, 7 do weekdays[days[day].weekday] = core.format(
				core.date(core.shift(first_day, core.PERIOD.DAY, day - 1)), "EEE") end
			months[#months + 1] = { month = month, title = core.format(first_day, "MMM"),
				days = days, weekday_labels = weekdays }
		end
		years[#years + 1] = { year = year, months = months }
	end

	return {
		total_weeks = total_weeks,
		years = years,
		-- Invoked under the retained VM lock once the canvas knows its width.
		calendar = function(weeks)
			return calendar_days(ending_date, weeks)
		end,
		sources = source_data,
	}
end
