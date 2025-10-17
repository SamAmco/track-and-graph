local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

-- Test: All fields mode (default)
M.test_all_fields = {
	config = {
		compare_by = "_all_fields",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 10.0, label = "a", note = "note1" }, -- duplicate
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "a", note = "note1" }, -- value changed
				{ timestamp = now - (DDAY * 2), value = 20.0, label = "b", note = "note1" }, -- label changed
				{ timestamp = now - (DDAY * 1), value = 20.0, label = "b", note = "note2" }, -- note changed
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(4, #result) -- Should filter out 1 duplicate
		test.assertEquals(10.0, result[1].value)
		test.assertEquals("a", result[1].label)
		test.assertEquals("note1", result[1].note)
		test.assertEquals(20.0, result[2].value) -- value changed
		test.assertEquals("b", result[3].label) -- label changed
		test.assertEquals("note2", result[4].note) -- note changed
	end,
}

-- Test: Value only mode
M.test_value_only = {
	config = {
		compare_by = "_value_only",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 10.0, label = "b", note = "note2" }, -- same value, different label/note
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "b", note = "note2" }, -- value changed
				{ timestamp = now - (DDAY * 2), value = 20.0, label = "c", note = "note3" }, -- same value, different label/note
				{ timestamp = now - (DDAY * 1), value = 30.0, label = "c", note = "note3" }, -- value changed
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should only pass through when value changes
		test.assertEquals(10.0, result[1].value)
		test.assertEquals(20.0, result[2].value)
		test.assertEquals(30.0, result[3].value)
	end,
}

-- Test: Label only mode
M.test_label_only = {
	config = {
		compare_by = "_label_only",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 20.0, label = "a", note = "note2" }, -- same label, different value/note
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "b", note = "note2" }, -- label changed
				{ timestamp = now - (DDAY * 2), value = 30.0, label = "b", note = "note3" }, -- same label, different value/note
				{ timestamp = now - (DDAY * 1), value = 30.0, label = "c", note = "note3" }, -- label changed
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should only pass through when label changes
		test.assertEquals("a", result[1].label)
		test.assertEquals("b", result[2].label)
		test.assertEquals("c", result[3].label)
	end,
}

-- Test: Note only mode
M.test_note_only = {
	config = {
		compare_by = "_note_only",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 20.0, label = "b", note = "note1" }, -- same note, different value/label
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "b", note = "note2" }, -- note changed
				{ timestamp = now - (DDAY * 2), value = 30.0, label = "c", note = "note2" }, -- same note, different value/label
				{ timestamp = now - (DDAY * 1), value = 30.0, label = "c", note = "note3" }, -- note changed
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should only pass through when note changes
		test.assertEquals("note1", result[1].note)
		test.assertEquals("note2", result[2].note)
		test.assertEquals("note3", result[3].note)
	end,
}

-- Test: Value and label mode
M.test_value_and_label = {
	config = {
		compare_by = "_value_and_label",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 10.0, label = "a", note = "note2" }, -- same value/label, different note
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "a", note = "note2" }, -- value changed
				{ timestamp = now - (DDAY * 2), value = 20.0, label = "b", note = "note3" }, -- label changed
				{ timestamp = now - (DDAY * 1), value = 20.0, label = "b", note = "note4" }, -- same value/label, different note
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should filter out when both value and label are same
		test.assertEquals(10.0, result[1].value)
		test.assertEquals("a", result[1].label)
		test.assertEquals(20.0, result[2].value) -- value changed
		test.assertEquals("b", result[3].label) -- label changed
	end,
}

-- Test: Value and note mode
M.test_value_and_note = {
	config = {
		compare_by = "_value_and_note",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 10.0, label = "b", note = "note1" }, -- same value/note, different label
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "b", note = "note1" }, -- value changed
				{ timestamp = now - (DDAY * 2), value = 20.0, label = "c", note = "note2" }, -- note changed
				{ timestamp = now - (DDAY * 1), value = 20.0, label = "d", note = "note2" }, -- same value/note, different label
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should filter out when both value and note are same
		test.assertEquals(10.0, result[1].value)
		test.assertEquals("note1", result[1].note)
		test.assertEquals(20.0, result[2].value) -- value changed
		test.assertEquals("note2", result[3].note) -- note changed
	end,
}

-- Test: Label and note mode
M.test_label_and_note = {
	config = {
		compare_by = "_label_and_note",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 5), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 4), value = 20.0, label = "a", note = "note1" }, -- same label/note, different value
				{ timestamp = now - (DDAY * 3), value = 20.0, label = "b", note = "note1" }, -- label changed
				{ timestamp = now - (DDAY * 2), value = 30.0, label = "b", note = "note2" }, -- note changed
				{ timestamp = now - (DDAY * 1), value = 40.0, label = "b", note = "note2" }, -- same label/note, different value
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(3, #result) -- Should filter out when both label and note are same
		test.assertEquals("a", result[1].label)
		test.assertEquals("note1", result[1].note)
		test.assertEquals("b", result[2].label) -- label changed
		test.assertEquals("note2", result[3].note) -- note changed
	end,
}

-- Test: No config (should default to all fields)
M.test_no_config = {
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 3), value = 10.0, label = "a", note = "note1" },
				{ timestamp = now - (DDAY * 2), value = 10.0, label = "a", note = "note1" }, -- duplicate
				{ timestamp = now - (DDAY * 1), value = 20.0, label = "a", note = "note1" }, -- value changed
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result) -- Should default to all fields mode
		test.assertEquals(10.0, result[1].value)
		test.assertEquals(20.0, result[2].value)
	end,
}

-- -- Test: Empty source
M.test_empty_source = {
	config = {
		compare_by = "_all_fields",
	},
	sources = function()
		return { {} }
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(0, #result)
	end,
}

-- Test: Single data point
M.test_single_data_point = {
	config = {
		compare_by = "_value_only",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now, value = 10.0, label = "a", note = "note1" },
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(1, #result)
		test.assertEquals(10.0, result[1].value)
	end,
}

-- Test: Nil fields
M.test_nil_fields = {
	config = {
		compare_by = "_all_fields",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			{
				{ timestamp = now - (DDAY * 3), value = 10.0 }, -- no label or note
				{ timestamp = now - (DDAY * 2), value = 10.0 }, -- duplicate
				{ timestamp = now - (DDAY * 1), value = 10.0, label = "a" }, -- label added
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(2, #result)
		test.assertEquals(10.0, result[1].value)
		test.assert("first should have no label", result[1].label == "")
		test.assertEquals("a", result[2].label) -- label changed from nil
	end,
}

return M
