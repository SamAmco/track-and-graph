local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_round_default = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 10.4,
					label = "round_down",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 10.5,
					label = "round_up",
				},
				{
					timestamp = now - (DDAY * 3),
					value = -5.6,
					label = "negative",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		test.assertEquals(10.0, result[1].value)
		test.assertEquals(11.0, result[2].value)
		test.assertEquals(-6.0, result[3].value)
	end,
}

M.test_round_to_multiple = {
	config = {
		nearest = 5.0,
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
					value = 17.0,
					label = "test2",
				},
				{
					timestamp = now - (DDAY * 3),
					value = 13.0,
					label = "test3",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		-- Round to nearest 5
		test.assertEquals(10.0, result[1].value)
		test.assertEquals(15.0, result[2].value)
		test.assertEquals(15.0, result[3].value)
	end,
}

M.test_round_to_decimal = {
	config = {
		nearest = 0.1,
	},
	sources = function()
		return {
			{
				{
					timestamp = now,
					value = 10.14,
					label = "test",
				},
				{
					timestamp = now - DDAY,
					value = 10.16,
					label = "test2",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)

		-- Round to nearest 0.1
		test.assertClose(10.1, result[1].value, 0.001)
		test.assertClose(10.2, result[2].value, 0.001)
	end,
}

return M
