local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_distinct_note_basic = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "morning",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "morning",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "evening",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 40.0,
          note = "evening",
        },
        {
          timestamp = now - (DDAY * 5),
          value = 50.0,
          note = "morning",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep first morning, first evening, then morning again
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("morning", result[1].note)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("evening", result[2].note)
    test.assertEquals(50.0, result[3].value)
    test.assertEquals("morning", result[3].note)
  end,
}

M.test_distinct_note_all_same = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "same note",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "same note",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "same note",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep the first one
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("same note", result[1].note)
  end,
}

M.test_distinct_note_all_different = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "note A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "note B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "note C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should keep all of them
    test.assertEquals(3, #result)
    test.assertEquals("note A", result[1].note)
    test.assertEquals("note B", result[2].note)
    test.assertEquals("note C", result[3].note)
  end,
}

return M
