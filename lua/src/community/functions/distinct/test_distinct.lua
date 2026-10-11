local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

local function points(rows)
	local now = core.time().timestamp
	local result = {}
	for index, row in ipairs(rows) do
		result[index] = {
			timestamp = now - (DDAY * (#rows - index)),
			value = row[1],
			label = row[2],
			note = row[3],
		}
	end
	return { result }
end

M.test_all_fields_filters_non_consecutive_duplicates = {
	config = { compare_by = "_all_fields" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "b", "two" },
			{ 10, "a", "one" },
			{ 10, "a", "different note" },
		}
	end,
	assertions = function(result)
		test.assertEquals(3, #result)
		test.assertEquals(10, result[1].value)
		test.assertEquals(20, result[2].value)
		test.assertEquals("different note", result[3].note)
	end,
}

M.test_value_only = {
	config = { compare_by = "_value_only" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "b", "two" },
			{ 10, "c", "three" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals(10, result[1].value)
		test.assertEquals(20, result[2].value)
	end,
}

M.test_label_only = {
	config = { compare_by = "_label_only" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "b", "two" },
			{ 30, "a", "three" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("a", result[1].label)
		test.assertEquals("b", result[2].label)
	end,
}

M.test_note_only = {
	config = { compare_by = "_note_only" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "b", "two" },
			{ 30, "c", "one" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("one", result[1].note)
		test.assertEquals("two", result[2].note)
	end,
}

M.test_value_and_label = {
	config = { compare_by = "_value_and_label" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 10, "b", "two" },
			{ 10, "a", "three" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("a", result[1].label)
		test.assertEquals("b", result[2].label)
	end,
}

M.test_value_and_note = {
	config = { compare_by = "_value_and_note" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 10, "b", "two" },
			{ 10, "c", "one" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("one", result[1].note)
		test.assertEquals("two", result[2].note)
	end,
}

M.test_label_and_note = {
	config = { compare_by = "_label_and_note" },
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "a", "two" },
			{ 30, "a", "one" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("one", result[1].note)
		test.assertEquals("two", result[2].note)
	end,
}

M.test_no_config_defaults_to_all_fields = {
	sources = function()
		return points {
			{ 10, "a", "one" },
			{ 20, "b", "two" },
			{ 10, "a", "one" },
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
	end,
}

M.test_empty_source = {
	sources = function()
		return { {} }
	end,
	assertions = function(result)
		test.assertEquals(0, #result)
	end,
}

M.test_missing_optional_fields = {
	config = { compare_by = "_all_fields" },
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - DDAY, value = 10 },
				{ timestamp = now, value = 10 },
			},
		}
	end,
	assertions = function(result)
		test.assertEquals(1, #result)
	end,
}

return M
