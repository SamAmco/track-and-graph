local M = {}

local core = require("tng.core")
local test = require("test.core")

local test_time = core.time({ year = 2025, month = 10, day = 15, hour = 14, min = 30, sec = 45 })

M.test_overwrite = {
	config = { mode = "_overwrite" },
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 42.5,
					label = "old label",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("42.5", result[1].label)
		test.assertEquals(42.5, result[1].value)
	end,
}

M.test_prepend = {
	config = { mode = "_prepend" },
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 100.5,
					label = " units",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("100.5 units", result[1].label)
	end,
}

M.test_append = {
	config = { mode = "_append" },
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 25.5,
					label = "score: ",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("score: 25.5", result[1].label)
	end,
}

M.test_default_is_overwrite = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 99.5,
					label = "existing",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("99.5", result[1].label)
	end,
}

M.test_empty_label_prepend = {
	config = { mode = "_prepend" },
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 7.5,
					label = "",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("7.5", result[1].label)
	end,
}

M.test_preserves_other_fields = {
	config = { mode = "_overwrite" },
	sources = function()
		return {
			{
				{
					timestamp = test_time.timestamp,
					offset = test_time.offset,
					value = 123.5,
					label = "old",
					note = "my note",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals("123.5", result[1].label)
		test.assertEquals(123.5, result[1].value)
		test.assertEquals("my note", result[1].note)
		test.assertEquals(test_time.timestamp, result[1].timestamp)
	end,
}

return M
