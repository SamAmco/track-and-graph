local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_swap_label_note_basic = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now - (DDAY * 1),
					value = 10.0,
					label = "original_label",
					note = "original_note",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 20.0,
					label = "label2",
					note = "note2",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)

		-- Check that label and note were swapped
		test.assertEquals("original_note", result[1].label)
		test.assertEquals("original_label", result[1].note)
		test.assertEquals("note2", result[2].label)
		test.assertEquals("label2", result[2].note)

		-- Values and timestamps should be preserved
		test.assertEquals(10.0, result[1].value)
		test.assertEquals(20.0, result[2].value)
	end,
}

M.test_swap_label_note_empty_fields = {
	config = {},
	sources = function()
		return {
			{
				{
					timestamp = now,
					value = 5.0,
					label = "",
					note = "has_note",
				},
				{
					timestamp = now - DDAY,
					value = 10.0,
					label = "has_label",
					note = "",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)

		-- Empty strings should swap correctly
		test.assertEquals("has_note", result[1].label)
		test.assertEquals("", result[1].note)
		test.assertEquals("", result[2].label)
		test.assertEquals("has_label", result[2].note)
	end,
}

return M
