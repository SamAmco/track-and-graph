local core = require("tng.core")
local graph = require("tng.graph")
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = nil
-- If from_now is false the end of the graph will be the last datapoint, otherwise it's the current date/time
local from_now = false
-- Totalling period used to calculate 'plot totals'
local totalling_period = core.PERIOD.DAY
-- Optional boolean to count by label. If true, each datapoint counts as 1, and the value is ignored
local count_by_label = false
-- Optional totalling period multiplier used to calculate 'plot totals' e.g. 2
local totalling_period_multiplier = nil
-- Optional colors list, e.g. { label: "#FF00FF", label2: "#0000FF", label3: core.COLOR.BLUE_SKY }
local label_colors = nil
-- Optional if the y axis represents time
local duration_based_range = false
-- Optional max for the y axis e.g. 100
local y_max = nil

local get_accumulation = function(datapoints, cutoff)
	local bars = {}

	if not datapoints or #datapoints == 0 then
		return bars
	end

	local current_totals = {}
	-- Add all the labels to current totals so that they are sorted correctly from the beginning
	for _, dp in ipairs(datapoints) do
		current_totals[dp.label or ""] = 0
	end

	local index = #datapoints
	local current_bar_start = cutoff or datapoints[#datapoints].timestamp
	local current_bar_end = core.time(graph.get_end_of_period(totalling_period, current_bar_start)).timestamp
	current_bar_start = core.shift(current_bar_start, totalling_period, -1).timestamp

	while index > 0 do
		local dp = datapoints[index]

		while dp.timestamp < current_bar_end do
			local key = dp.label or ""
			local this_value = count_by_label and 1 or dp.value
			current_totals[key] = (current_totals[key] or 0) + this_value
			index = index - 1
			dp = datapoints[index]
			if not dp then
				break
			end
		end

		local segments = {}

		for label, value in pairs(current_totals) do
			table.insert(segments, {
				label = label,
				value = value,
				color = label_colors and label_colors[label] or nil,
			})
		end

		table.sort(segments, function(a, b)
			return a.label < b.label
		end)

		table.insert(bars, 1, segments)

		current_bar_start = current_bar_end
		current_bar_end = core.shift(current_bar_end, totalling_period, 1).timestamp
	end

	return bars
end

return function(sources)
	local bars = nil
	local end_time = nil

	local _, source = next(sources)

	if not source then
		return nil
	end

	local latest_data_point = source.dp()

	if not latest_data_point then
		return nil
	end

	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
		from_now = from_now,
		timestamp = latest_data_point.timestamp,
	}
	local cutoff = graph.get_cutoff(cutoff_params)

	local datapoints = source.dpafter(cutoff)
	table.insert(datapoints, 1, latest_data_point)

	local end_date = nil
	if from_now then
		end_date = graph.get_end_of_period(totalling_period, core.time().timestamp)
	else
		end_date = graph.get_end_of_period(totalling_period, datapoints[1].timestamp)
	end

	end_time = core.time(end_date)
	bars = get_accumulation(datapoints, cutoff)

	return {
		type = graph.GRAPH_TYPE.TIME_BARCHART,
		bars = bars,
		end_time = end_time,
		duration_based_range = duration_based_range,
		bar_period = totalling_period,
		bar_period_multiple = totalling_period_multiplier or 1,
		y_max = y_max,
	}
end
