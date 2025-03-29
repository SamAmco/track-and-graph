local core = require("tng.core")
local graph = require("tng.graph")

--- PREVIEW_START
-- Script: Pie Charts - Moving Pie Chart
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = nil
-- Optional colors list, e.g. { label="#FF00FF", label2="#0000FF", label3=core.COLOR.BLUE_SKY }
local label_colors = nil
-- Boolean to count by label. If true, each datapoint counts as 1, and the value is ignored
local count_by_label = false
--- PREVIEW_END

return function(sources)

	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier
	}

	local datapoints = graph.merge_sources(sources, cutoff_params, true)

	local segments = graph.collect_to_segments(datapoints, count_by_label, label_colors)

	return {
		type = graph.GRAPH_TYPE.PIE_CHART,
		segments = segments
	}
end
