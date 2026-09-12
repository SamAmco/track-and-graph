local M = {}

local test = require("test.core")

M.test_moves_points_to_their_start = {
	config = {},
	sources = function()
		return {
			{
				{ timestamp = 20000000, offset = 3600, value = 3600, label = "one hour", note = "first" },
				{ timestamp = 10000000, offset = 0, value = 30.5, label = "seconds", note = "second" },
			},
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals(16400000, result[1].timestamp)
		test.assertEquals(9969500, result[2].timestamp)
		test.assertEquals(3600, result[1].value)
		test.assertEquals("one hour", result[1].label)
		test.assertEquals("first", result[1].note)
		test.assertEquals(3600, result[1].offset)
	end,
}

M.test_reorders_overlapping_durations_by_start_time = {
	config = {},
	sources = function()
		return {
			{
				{ timestamp = 100000, value = 90, label = "long newest" },
				{ timestamp = 90000, value = 10, label = "short middle" },
				{ timestamp = 70000, value = 20, label = "oldest" },
			},
		}
	end,
	assertions = function(result)
		test.assertEquals(3, #result)
		test.assertEquals("short middle", result[1].label)
		test.assertEquals(80000, result[1].timestamp)
		test.assertEquals("oldest", result[2].label)
		test.assertEquals(50000, result[2].timestamp)
		test.assertEquals("long newest", result[3].label)
		test.assertEquals(10000, result[3].timestamp)
	end,
}

M.test_negative_duration_moves_timestamp_forward = {
	config = {},
	sources = function()
		return {
			{
				{ timestamp = 100000, value = 10, label = "positive" },
				{ timestamp = 90000, value = -30, label = "negative" },
			},
		}
	end,
	assertions = function(result)
		test.assertEquals(2, #result)
		test.assertEquals("negative", result[1].label)
		test.assertEquals(120000, result[1].timestamp)
		test.assertEquals("positive", result[2].label)
		test.assertEquals(90000, result[2].timestamp)
	end,
}

M.test_zero_durations_keep_stable_input_order = {
	config = {},
	sources = function()
		return {
			{
				{ timestamp = 100000, value = 0, label = "first" },
				{ timestamp = 100000, value = 0, label = "second" },
				{ timestamp = 90000, value = 0, label = "third" },
			},
		}
	end,
	assertions = function(result)
		test.assertEquals(3, #result)
		test.assertEquals("first", result[1].label)
		test.assertEquals("second", result[2].label)
		test.assertEquals("third", result[3].label)
	end,
}

M.test_many_overlaps_match_a_full_sort = {
	config = {},
	sources = function()
		local source = {}
		for index = 1, 200 do
			local timestamp = 1000000 - (index * 1000)
			local duration = (index * 7919) % 97
			source[index] = {
				timestamp = timestamp,
				value = duration,
				label = tostring(index),
			}
		end
		return { source }
	end,
	assertions = function(result)
		local expected = {}
		for index = 1, 200 do
			local timestamp = 1000000 - (index * 1000)
			local duration = (index * 7919) % 97
			expected[index] = {
				timestamp = timestamp - (duration * 1000),
				input_order = index,
			}
		end
		table.sort(expected, function(left, right)
			if left.timestamp ~= right.timestamp then
				return left.timestamp > right.timestamp
			end
			return left.input_order < right.input_order
		end)

		test.assertEquals(#expected, #result)
		for index, expected_point in ipairs(expected) do
			test.assertEquals(expected_point.timestamp, result[index].timestamp)
			test.assertEquals(tostring(expected_point.input_order), result[index].label)
		end
	end,
}

M.test_empty_source = {
	config = {},
	sources = function()
		return { {} }
	end,
	assertions = function(result)
		test.assertEquals(0, #result)
	end,
}

return M
