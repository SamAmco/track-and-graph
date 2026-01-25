local M = {}

local core = require("tng.core")
local test = require("test.core")

local test_time = core.time({ year = 2025, month = 10, day = 15, hour = 14, min = 30, sec = 45 })

M.test_hour_of_day = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 999.0,
					label = "test",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals(14.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

return M
