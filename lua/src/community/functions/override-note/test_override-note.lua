local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_override_note_basic = {
  config = {
    new_note = "new_note",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "old_note_1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "old_note_2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "old_note_3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(3, #result)

    -- All notes should be overridden
    test.assertEquals("new_note", result[1].note)
    test.assertEquals("new_note", result[2].note)
    test.assertEquals("new_note", result[3].note)

    -- Values should be preserved
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals(30.0, result[3].value)
  end,
}

M.test_override_note_no_config = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 5.0,
          note = "original",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Without config, note should remain unchanged
    test.assertEquals("original", result[1].note)
    test.assertEquals(5.0, result[1].value)
  end,
}

M.test_override_note_empty_string = {
  config = {
    new_note = "",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 15.0,
          note = "original",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Empty string should be a valid override
    test.assertEquals("", result[1].note)
    test.assertEquals(15.0, result[1].value)
  end,
}

return M
