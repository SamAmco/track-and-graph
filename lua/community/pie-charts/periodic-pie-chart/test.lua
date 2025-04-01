local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

M.test_periodic_pie_chart_default_is_week = {
	config = {},
	sources = function()
		local end_date = core.get_end_of_period(core.PERIOD.WEEK, core.time().timestamp)
		local end_time = core.time(end_date).timestamp
		local start_time = core.shift(end_time, core.PERIOD.WEEK, -1).timestamp
		local diff = end_time - start_time
		local interval = diff / 3

		return {
			source1 = {
				{
					timestamp = end_time - interval,
					value = 5.0,
					label = "A",
				},
				{
					timestamp = end_time - (interval * 2),
					value = 4.0,
					label = "B",
				},
				{
					timestamp = end_time - (interval * 4),
					value = 3.0,
					label = "C",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

		local expected_segments = {
			{ value = 5.0, label = "A" },
			{ value = 4.0, label = "B" },
		}

		test.assertEquals(#expected_segments, #result.segments)
		for i, segment in ipairs(expected_segments) do
			test.assertEquals(segment.value, result.segments[i].value)
			test.assertEquals(segment.label, result.segments[i].label)
		end
	end,
}

M.test_periodic_pie_chart_specific_period = {
	config = {
		period = "core.PERIOD.DAY",
	},
	sources = function()
		local now = core.time().timestamp
		local end_date = core.get_end_of_period(core.PERIOD.DAY, now)
		local end_time = core.time(end_date).timestamp
		local start_time = core.shift({ timestamp = end_time }, core.PERIOD.DAY, -1).timestamp
		local diff = end_time - start_time
		local interval = diff / 4

		return {
			source1 = {
				{
					timestamp = start_time + interval,
					value = 5.0,
					label = "A",
				},
				{
					timestamp = start_time + (interval * 2),
					value = 4.0,
					label = "B",
				},
				{
					timestamp = start_time + (interval * 3),
					value = 3.0,
					label = "C",
				},
				{
					timestamp = start_time - interval, -- Outside current period
					value = 10.0,
					label = "D",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

		local expected_segments = {
			{ value = 5.0, label = "A" },
			{ value = 4.0, label = "B" },
			{ value = 3.0, label = "C" },
		}

		test.assertEquals(#expected_segments, #result.segments)
		for i, segment in ipairs(expected_segments) do
			test.assertEquals(segment.value, result.segments[i].value)
			test.assertEquals(segment.label, result.segments[i].label)
		end
	end,
}

M.test_periodic_pie_chart_multiple_sources = {
	config = {
		period = "core.PERIOD.WEEK",
	},
	sources = function()
		local now = core.time().timestamp
		local end_date = core.get_end_of_period(core.PERIOD.WEEK, now)
		local end_time = core.time(end_date).timestamp
		local start_time = core.shift({ timestamp = end_time }, core.PERIOD.WEEK, -1).timestamp
		local diff = end_time - start_time
		local interval = diff / 3

		return {
			source1 = {
				{
					timestamp = end_time - interval,
					value = 5.0,
					label = "A",
				},
			},
			source2 = {
				{
					timestamp = end_time - (interval * 2),
					value = 4.0,
					label = "B",
				},
				{
					timestamp = end_time - (interval * 4), -- Outside current period
					value = 3.0,
					label = "C",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

		local expected_segments = {
			{ value = 5.0, label = "A" },
			{ value = 4.0, label = "B" },
		}

		test.assertEquals(#expected_segments, #result.segments)
		for i, segment in ipairs(expected_segments) do
			test.assertEquals(segment.value, result.segments[i].value)
			test.assertEquals(segment.label, result.segments[i].label)
		end
	end,
}

M.test_periodic_pie_chart_with_colors = {
	config = {
		period = "core.PERIOD.DAY",
		label_colors = '{ A = "#FF0000", B = core.COLOR.BLUE, C = core.COLOR.GREEN_LIGHT }',
	},
	sources = function()
		local now = core.time().timestamp
		local end_date = core.get_end_of_period(core.PERIOD.DAY, now)
		local end_time = core.time(end_date).timestamp
		local start_time = core.shift({ timestamp = end_time }, core.PERIOD.DAY, -1).timestamp
		local diff = end_time - start_time
		local interval = diff / 4

		return {
			source1 = {
				{
					timestamp = start_time + interval,
					value = 5.0,
					label = "A",
				},
				{
					timestamp = start_time + (interval * 2),
					value = 4.0,
					label = "B",
				},
				{
					timestamp = start_time + (interval * 3),
					value = 3.0,
					label = "C",
				},
				{
					timestamp = start_time - interval, -- Outside current period
					value = 10.0,
					label = "D",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

		local expected_segments = {
			{ value = 5.0, label = "A", color = "#FF0000" },
			{ value = 4.0, label = "B", color = core.COLOR.BLUE },
			{ value = 3.0, label = "C", color = core.COLOR.GREEN_LIGHT },
		}

		test.assertEquals(#expected_segments, #result.segments)
		for i, segment in ipairs(expected_segments) do
			test.assertEquals(segment.value, result.segments[i].value)
			test.assertEquals(segment.label, result.segments[i].label)
			test.assertEquals(segment.color, result.segments[i].color)
		end
	end,
}

return M
