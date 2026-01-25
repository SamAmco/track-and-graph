local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

local PDAY = core.PERIOD.DAY
local DDAY = core.DURATION.DAY

M.test_difference_line_calculates_difference = {
	config = {
		from_now = "false",
	},
	sources = function()
		return {
			source1 = {
				{
					timestamp = 5,
					value = 5.0,
				},
				{
					timestamp = 4,
					value = 4.0,
				},
				{
					timestamp = 3,
					value = 3.0,
				},
				{
					timestamp = 2,
					value = 2.0,
				},
				{
					timestamp = 1,
					value = 1.0,
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.LINE_GRAPH, result.type)

		local expected_values = { 1.0, 1.0, 1.0, 1.0 }
		local line_points = result.lines[1].line_points

		test.assertEquals(#expected_values, #line_points)
		for i, value in ipairs(expected_values) do
			test.assertEquals(value, line_points[i].value)
		end
	end,
}

M.test_difference_line_with_totalling_period = {
	config = {
		from_now = "false",
		totalling_period = "core.PERIOD.DAY",
	},
	sources = function()
		local now = core.time()
		local data = {
			{ value = 1, days_ago = 0 },
			{ value = 2, days_ago = 0 },
			{ value = 3, days_ago = 1 },
			{ value = 4, days_ago = 2 },
			{ value = 5, days_ago = 2 },
			{ value = 1, days_ago = 3 },
			{ value = 2, days_ago = 3 },
			{ value = 3, days_ago = 3 },
		}

		local source1 = {}
		for _, item in ipairs(data) do
			table.insert(source1, {
				timestamp = core.shift(now, PDAY, -item.days_ago).timestamp,
				value = item.value,
			})
		end

		return {
			source1 = source1,
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.LINE_GRAPH, result.type)

		local expected_values = { 0.0, -6.0, 3.0 }
		local line_points = result.lines[1].line_points

		test.assertEquals(#expected_values, #line_points)
		for i, value in ipairs(expected_values) do
			test.assertEquals(value, line_points[i].value)
		end
	end,
}

M.test_difference_with_multiple_datasources = {
	config = {
		from_now = "false",
	},
	sources = function()
		return {
			source1 = {
				{
					timestamp = 5,
					value = 5.0,
				},
				{
					timestamp = 4,
					value = 4.0,
				},
				{
					timestamp = 3,
					value = 1.0,
				},
				{
					timestamp = 2,
					value = 2.0,
				},
				{
					timestamp = 1,
					value = 5.0,
				},
			},
			source2 = {
				{
					timestamp = 5,
					value = 1.0,
				},
				{
					timestamp = 4,
					value = -9.0,
				},
				{
					timestamp = 3,
					value = 20.0,
				},
				{
					timestamp = 2,
					value = 7.0,
				},
				{
					timestamp = 1,
					value = 8.0,
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.LINE_GRAPH, result.type)

		local expected_values_line1 = { 1.0, 3.0, -1.0, -3.0 }
		local expected_values_line2 = { 10.0, -29.0, 13.0, -1.0 }

		test.assertEquals(2, #result.lines)

		local line_points1 = result.lines[1].line_points
		local line_points2 = result.lines[2].line_points

		test.assertEquals(#expected_values_line1, #line_points1)
		test.assertEquals(#expected_values_line2, #line_points2)

		for i, value in ipairs(expected_values_line1) do
			test.assertEquals(value, line_points1[i].value)
		end

		for i, value in ipairs(expected_values_line2) do
			test.assertEquals(value, line_points2[i].value)
		end
	end,
}

M.test_using_from_now = {
	config = {
		from_now = "true",
		period = "core.PERIOD.DAY",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			source1 = {
				{
					timestamp = now - (DDAY * 0.3),
					value = 4.0,
				},
				{
					timestamp = now - (DDAY * 0.4),
					value = 3.0,
				},
				{
					timestamp = now - (DDAY * 0.4),
					value = 2.0,
				},
				{
					timestamp = now - (DDAY * 1.2),
					value = 1.0,
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.LINE_GRAPH, result.type)

		local expected_values = { 1.0, 1.0 }
		local line_points = result.lines[1].line_points

		test.assertEquals(#expected_values, #line_points)
		for i, value in ipairs(expected_values) do
			test.assertEquals(value, line_points[i].value)
		end
	end,
}

return M
