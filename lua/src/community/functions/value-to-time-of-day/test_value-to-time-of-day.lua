local M = {}

local core = require("tng.core")
local test = require("test.core")

-- Create timestamps for specific times of day
local morning = core.time({ year = 2025, month = 10, day = 13, hour = 9, min = 30, sec = 0 })
local afternoon = core.time({ year = 2025, month = 10, day = 13, hour = 14, min = 45, sec = 30 })
local midnight = core.time({ year = 2025, month = 10, day = 13, hour = 0, min = 0, sec = 0 })
local late = core.time({ year = 2025, month = 10, day = 13, hour = 23, min = 59, sec = 59 })
local noon = core.time({ year = 2025, month = 10, day = 13, hour = 12, min = 0, sec = 0 })

M.test_time_of_day_basic = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = afternoon.timestamp,
					offset = afternoon.offset,
					value = 999.0,
					label = "test1",
				},
				{
					timestamp = morning.timestamp,
					offset = morning.offset,
					value = 888.0,
					label = "test2",
				},
				{
					timestamp = midnight.timestamp,
					offset = midnight.offset,
					value = 777.0,
					label = "test3",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		-- 14:45:30 = (14*3600) + (45*60) + 30 = 53130 seconds
		test.assertEquals(53130.0, result[1].value)
		test.assertEquals("test1", result[1].label)

		-- 09:30:00 = (9*3600) + (30*60) = 34200 seconds
		test.assertEquals(34200.0, result[2].value)
		test.assertEquals("test2", result[2].label)

		-- 00:00:00 = 0 seconds
		test.assertEquals(0.0, result[3].value)
		test.assertEquals("test3", result[3].label)
	end,
}

M.test_time_of_day_near_midnight = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = late.timestamp,
					offset = late.offset,
					value = 100.0,
					label = "late",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)

		-- 23:59:59 = (23*3600) + (59*60) + 59 = 86399 seconds
		test.assertEquals(86399.0, result[1].value)
		test.assertEquals("late", result[1].label)
	end,
}

M.test_time_of_day_preserves_fields = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = noon.timestamp,
					offset = noon.offset,
					value = 42.0,
					label = "test",
					note = "test note",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)

		-- 12:00:00 = 12*3600 = 43200 seconds
		test.assertEquals(43200.0, result[1].value)

		-- Other fields preserved
		test.assertEquals(noon.timestamp, result[1].timestamp)
		test.assertEquals("test", result[1].label)
		test.assertEquals("test note", result[1].note)
	end,
}

return M
