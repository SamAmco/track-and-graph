local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_absolute_value_basic = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = -10.5,
					label = "negative",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 20.0,
					label = "positive",
				},
				{
					timestamp = now - (DDAY * 3),
					value = 0.0,
					label = "zero",
				},
				{
					timestamp = now - (DDAY * 4),
					value = -100.0,
					label = "large_negative",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(4, #result)

		-- Check absolute values
		test.assertEquals(10.5, result[1].value)
		test.assertEquals(20.0, result[2].value)
		test.assertEquals(0.0, result[3].value)
		test.assertEquals(100.0, result[4].value)

		-- Labels preserved
		test.assertEquals("negative", result[1].label)
		test.assertEquals("positive", result[2].label)
		test.assertEquals("zero", result[3].label)
		test.assertEquals("large_negative", result[4].label)
	end,
}

M.test_absolute_value_preserves_fields = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now,
					value = -42.0,
					label = "test",
					note = "test note",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)

		test.assertEquals(42.0, result[1].value)
		test.assertEquals(now, result[1].timestamp)
		test.assertEquals("test", result[1].label)
		test.assertEquals("test note", result[1].note)
	end,
}

return M
