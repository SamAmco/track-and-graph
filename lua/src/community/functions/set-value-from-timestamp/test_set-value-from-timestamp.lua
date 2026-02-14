local M = {}

local core = require("tng.core")
local test = require("test.core")

-- Shared test time used by many tests
local test_time = core.time({ year = 2025, month = 10, day = 15, hour = 14, min = 30, sec = 45 })

-- Day of week timestamps
local monday = core.time({ year = 2025, month = 10, day = 13 })
local tuesday = core.time({ year = 2025, month = 10, day = 14 })
local wednesday = core.time({ year = 2025, month = 10, day = 15 })
local saturday = core.time({ year = 2025, month = 10, day = 18 })
local sunday = core.time({ year = 2025, month = 10, day = 19 })

-- Time of day timestamps
local morning = core.time({ year = 2025, month = 10, day = 13, hour = 9, min = 30, sec = 0 })
local afternoon = core.time({ year = 2025, month = 10, day = 13, hour = 14, min = 45, sec = 30 })
local midnight_time = core.time({ year = 2025, month = 10, day = 13, hour = 0, min = 0, sec = 0 })
local late = core.time({ year = 2025, month = 10, day = 13, hour = 23, min = 59, sec = 59 })
local noon = core.time({ year = 2025, month = 10, day = 13, hour = 12, min = 0, sec = 0 })

-- == Day of Month tests ==

M.test_day_of_month = {
	config = { component = "_day_of_month" },
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
		test.assertEquals(15.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

-- == Day of Week tests ==

M.test_day_of_week_conversion = {
	config = { component = "_day_of_week" },
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

		test.assertEquals(3.0, result[1].value) -- Wednesday
		test.assertEquals(2.0, result[2].value) -- Tuesday
		test.assertEquals(1.0, result[3].value) -- Monday

		test.assertEquals("test1", result[1].label)
		test.assertEquals("test2", result[2].label)
		test.assertEquals("test3", result[3].label)
	end,
}

M.test_day_of_week_weekend = {
	config = { component = "_day_of_week" },
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
	config = { component = "_day_of_week" },
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

		test.assertEquals(1.0, result[1].value) -- Monday

		test.assertEquals(monday.timestamp, result[1].timestamp)
		test.assertEquals("test", result[1].label)
		test.assertEquals("test note", result[1].note)
	end,
}

-- == Hour of Day tests ==

M.test_hour_of_day = {
	config = { component = "_hour_of_day" },
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

-- == Minute of Hour tests ==

M.test_minute_of_hour = {
	config = { component = "_minute_of_hour" },
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
		test.assertEquals(30.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

-- == Second of Minute tests ==

M.test_second_of_minute = {
	config = { component = "_second_of_minute" },
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
		test.assertEquals(45.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

-- == Month of Year tests ==

M.test_month_of_year = {
	config = { component = "_month_of_year" },
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
		test.assertEquals(10.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

-- == Year tests ==

M.test_year = {
	config = { component = "_year_value" },
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
		test.assertEquals(2025.0, result[1].value)
		test.assertEquals("test", result[1].label)
	end,
}

-- == Time of Day tests ==

M.test_duration_since_midnight_basic = {
	config = { component = "_duration_since_midnight" },
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
					timestamp = midnight_time.timestamp,
					offset = midnight_time.offset,
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

M.test_duration_since_midnight_near_midnight = {
	config = { component = "_duration_since_midnight" },
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

M.test_duration_since_midnight_preserves_fields = {
	config = { component = "_duration_since_midnight" },
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

		test.assertEquals(noon.timestamp, result[1].timestamp)
		test.assertEquals("test", result[1].label)
		test.assertEquals("test note", result[1].note)
	end,
}

-- == Default config test ==

M.test_default_config = {
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
		-- Default is day_of_month, so expect 15
		test.assertEquals(15.0, result[1].value)
	end,
}

return M
