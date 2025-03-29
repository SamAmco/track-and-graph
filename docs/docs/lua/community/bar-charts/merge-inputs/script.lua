local core = require("tng.core")
local graph = require("tng.graph")

--- PREVIEW_START
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = nil
-- If from_now is false the end of the graph will be the last datapoint, otherwise it's the current date/time
local from_now = false
-- Totalling period used to calculate 'plot totals'
local totalling_period = core.PERIOD.DAY
-- Optional totalling period multiplier used to calculate 'plot totals' e.g. 2
local totalling_period_multiplier = nil
-- Optional boolean to count by label. If true, each datapoint counts as 1, and the value is ignored
local count_by_label = false
-- Optional colors list, e.g. { label="#FF00FF", label2="#0000FF", label3=core.COLOR.BLUE_SKY }
local label_colors = nil
-- Optional if the y axis represents time
local duration_based_range = false
-- Optional max for the y axis e.g. 100
local y_max = nil
--- PREVIEW_END

return function(sources)
	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
	}
	local all_data = graph.merge_sources(sources, cutoff_params, from_now)

	if not all_data or #all_data == 0 then
		return nil
	end

	local end_date = nil
	if from_now then
		end_date = core.get_end_of_period(totalling_period, core.time().timestamp)
	else
		end_date = core.get_end_of_period(totalling_period, all_data[1].timestamp)
	end

	local end_time = core.time(end_date).timestamp-1

	local bars = graph.collect_to_bars(
		all_data,
		totalling_period,
		totalling_period_multiplier,
		count_by_label,
		end_time,
		label_colors
	)

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
