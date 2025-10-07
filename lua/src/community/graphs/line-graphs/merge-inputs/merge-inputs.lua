local core = require("tng.core")
local graph = require("tng.graph")
local graphext = require("tng.graphext")

--- PREVIEW_START
-- Script: Line Graphs - Merge Inputs
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = 8
-- If from_now is false the end of the graph will be the last datapoint, otherwise it's the current date/time
local from_now = false
-- Optional color, e.g. "#FF00FF" or core.COLOR.BLUE_SKY
local line_color = nil
-- Optional point style e.g. graph.LINE_POINT_STYLE.CIRCLE
local line_point_style = nil
-- Optional string label for the line in the legend, e.g. "Data"
local line_label = nil
-- Optional integer value used to average data points over a certain duration e.g. core.DURATION.DAY * 30 for a 30 day moving average
local averaging_duration = nil
-- Optional totalling period used to calculate 'plot totals' e.g. core.PERIOD.WEEK
local totalling_period = nil
-- Optional totalling period multiplier used to calculate 'plot totals' e.g. 2
local totalling_period_multiplier = nil
--- PREVIEW_END

return function(sources)
	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
	}
	local all_data = graphext.merge_sources(sources, cutoff_params, from_now)

	local totalled_data = graphext.calculate_period_totals(all_data, totalling_period, totalling_period_multiplier)
	graphext.apply_moving_averaging(totalled_data, averaging_duration)

	local line = {
		line_points = totalled_data,
		line_color = line_color,
		point_style = line_point_style or graph.LINE_POINT_STYLE.NONE,
		label = line_label,
	}

	return graph.line_graph({
		lines = { line },
	})
end
