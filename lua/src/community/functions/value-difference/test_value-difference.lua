local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_value_difference_basic = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 10.0,
					label = "point1",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 7.0,
					label = "point2",
				},
				{
					timestamp = now - (DDAY * 3),
					value = 5.0,
					label = "point3",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		-- Should have 2 results (n-1 differences)
		test.assertEquals(2, #result)

		-- Point1 with difference to point2: 10.0 - 7.0 = 3.0
		test.assertEquals(now - (DDAY * 1), result[1].timestamp)
		test.assertEquals(3.0, result[1].value)
		test.assertEquals("point1", result[1].label)

		-- Point2 with difference to point3: 7.0 - 5.0 = 2.0
		test.assertEquals(now - (DDAY * 2), result[2].timestamp)
		test.assertEquals(2.0, result[2].value)
		test.assertEquals("point2", result[2].label)
	end,
}

M.test_value_difference_negative = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 5.0,
					label = "decreasing",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 10.0,
					label = "increasing",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)

		-- 5.0 - 10.0 = -5.0 (negative difference when value increases)
		test.assertEquals(-5.0, result[1].value)
		test.assertEquals("decreasing", result[1].label)
	end,
}

M.test_value_difference_single_point = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now,
					value = 100.0,
					label = "only",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		-- Single point produces no output
		test.assertEquals(0, #result)
	end,
}

M.test_value_difference_preserves_fields = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 50.0,
					label = "first",
					note = "first note",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 20.0,
					label = "second",
					note = "second note",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)

		-- Difference: 50.0 - 20.0 = 30.0
		test.assertEquals(30.0, result[1].value)

		-- First point's fields preserved
		test.assertEquals(now - (DDAY * 1), result[1].timestamp)
		test.assertEquals("first", result[1].label)
		test.assertEquals("first note", result[1].note)
	end,
}

return M
