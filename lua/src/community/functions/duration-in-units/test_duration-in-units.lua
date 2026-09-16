local M = {}
local test = require("test.core")

local function source_with_value(value)
	return {{
		{
			timestamp = 123456789,
			offset = 3600,
			value = value,
			label = "preserved label",
			note = "preserved note",
		},
	}}
end

local conversion_cases = {
	_seconds = { input = 42.5, expected = 42.5 },
	_minutes = { input = 90, expected = 1.5 },
	_hours = { input = 5400, expected = 1.5 },
	_days = { input = 129600, expected = 1.5 },
	_weeks = { input = 907200, expected = 1.5 },
	_months = { input = 30.44 * 24 * 60 * 60 * 1.5, expected = 1.5 },
	_years = { input = 365.25 * 24 * 60 * 60 * 1.5, expected = 1.5 },
}

for unit, conversion in pairs(conversion_cases) do
	M["test_converts_to" .. unit] = {
		config = { unit = unit },
		sources = function()
			return source_with_value(conversion.input)
		end,
		assertions = function(result)
			test.assertEquals(1, #result)
			test.assertClose(conversion.expected, result[1].value)
		end,
	}
end

M.test_defaults_to_hours = {
	config = {},
	sources = function()
		return source_with_value(7200)
	end,
	assertions = function(result)
		test.assertEquals(2, result[1].value)
	end,
}

M.test_preserves_other_data_point_fields = {
	config = { unit = "_minutes" },
	sources = function()
		return source_with_value(60)
	end,
	assertions = function(result)
		local data_point = result[1]
		test.assertEquals(123456789, data_point.timestamp)
		test.assertEquals(3600, data_point.offset)
		test.assertEquals("preserved label", data_point.label)
		test.assertEquals("preserved note", data_point.note)
	end,
}

M.test_zero_and_negative_durations = {
	config = { unit = "_minutes" },
	sources = function()
		return {{
			{ timestamp = 2, value = 0 },
			{ timestamp = 1, value = -90 },
		}}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals(0, result[1].value)
		test.assertEquals(-1.5, result[2].value)
	end,
}

M.test_empty_source = {
	config = { unit = "_days" },
	sources = function()
		return { { } }
	end,
	assertions = function(result)
		test.assertEquals(0, #result)
	end,
}

return M
