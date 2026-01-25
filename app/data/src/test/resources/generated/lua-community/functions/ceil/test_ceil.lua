local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_ceil_default = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 10.1,
					label = "small",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 10.9,
					label = "large",
				},
				{
					timestamp = now - (DDAY * 3),
					value = -5.1,
					label = "negative",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		-- Ceiling to nearest 1
		test.assertEquals(11.0, result[1].value)
		test.assertEquals(11.0, result[2].value)
		test.assertEquals(-5.0, result[3].value)
	end,
}

M.test_ceil_to_multiple = {
	config = {
		nearest = 10.0,
	},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 12.0,
					label = "test1",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 20.0,
					label = "exact",
				},
				{
					timestamp = now - (DDAY * 3),
					value = 1.0,
					label = "small",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		-- Ceiling to nearest 10
		test.assertEquals(20.0, result[1].value)
		test.assertEquals(20.0, result[2].value)
		test.assertEquals(10.0, result[3].value)
	end,
}

M.test_ceil_to_decimal = {
	config = {
		nearest = 0.5,
	},
	sources = function()
		return {
			{
				{
					timestamp = now,
					value = 10.1,
					label = "test",
				},
				{
					timestamp = now - DDAY,
					value = 10.6,
					label = "test2",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)

		-- Ceiling to nearest 0.5
		test.assertEquals(10.5, result[1].value)
		test.assertEquals(11.0, result[2].value)
	end,
}

return M
