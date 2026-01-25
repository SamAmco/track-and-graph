local M = {}

local core = require("tng.core")
local test = require("test.core")

-- Create timestamps for specific days of the week
-- Using known dates: 2025-10-13 is a Monday (wday=1), 2025-10-14 is Tuesday (wday=2), etc.
local monday = core.time({ year = 2025, month = 10, day = 13 })
local tuesday = core.time({ year = 2025, month = 10, day = 14 })
local wednesday = core.time({ year = 2025, month = 10, day = 15 })
-- 2025-10-18 is Saturday (wday=6), 2025-10-19 is Sunday (wday=7)
local saturday = core.time({ year = 2025, month = 10, day = 18 })
local sunday = core.time({ year = 2025, month = 10, day = 19 })

M.test_day_of_week_conversion = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = wednesday.timestamp,
					offset = wednesday.offset,
					value = 999.0,
					label = "test1",
				},
				{
					timestamp = tuesday.timestamp,
					offset = tuesday.offset,
					value = 888.0,
					label = "test2",
				},
				{
					timestamp = monday.timestamp,
					offset = monday.offset,
					value = 777.0,
					label = "test3",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result)

		-- Values should be replaced with day of week
		test.assertEquals(3.0, result[1].value) -- Wednesday
		test.assertEquals(2.0, result[2].value) -- Tuesday
		test.assertEquals(1.0, result[3].value) -- Monday

		-- Labels should be preserved
		test.assertEquals("test1", result[1].label)
		test.assertEquals("test2", result[2].label)
		test.assertEquals("test3", result[3].label)
	end,
}

M.test_day_of_week_weekend = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = sunday.timestamp,
					offset = sunday.offset,
					value = 100.0,
					label = "weekend",
				},
				{
					timestamp = saturday.timestamp,
					offset = saturday.offset,
					value = 200.0,
					label = "weekend",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)

		test.assertEquals(7.0, result[1].value) -- Sunday
		test.assertEquals(6.0, result[2].value) -- Saturday
	end,
}

M.test_day_of_week_preserves_timestamp = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = monday.timestamp,
					offset = monday.offset,
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

		-- Value should be day of week
		test.assertEquals(1.0, result[1].value) -- Monday

		-- Other fields preserved
		test.assertEquals(monday.timestamp, result[1].timestamp)
		test.assertEquals("test", result[1].label)
		test.assertEquals("test note", result[1].note)
	end,
}

return M
