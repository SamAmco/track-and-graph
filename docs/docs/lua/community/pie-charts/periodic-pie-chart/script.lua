local core = require("tng.core")
local graph = require("tng.graph")
local graphext = require("tng.graphext")

--- PREVIEW_START
-- Script: Pie Charts - Periodic Pie Chart
-- Period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = core.PERIOD.WEEK
-- Optional colors list, e.g. { label="#FF00FF", label2="#0000FF", label3=core.COLOR.BLUE_SKY }
local label_colors = nil
-- Boolean to count by label. If true, each datapoint counts as 1, and the value is ignored
local count_by_label = false
--- PREVIEW_END

return function(sources)
	local end_time = core.get_end_of_period(period, core.time().timestamp)
	local cutoff = core.shift(end_time, period, -1).timestamp

	local datapoints = graphext.merge_sources(sources, cutoff)

	local segments = graphext.collect_to_segments(datapoints, count_by_label, label_colors)

	return graph.pie_chart({
		segments = segments,
	})
end
