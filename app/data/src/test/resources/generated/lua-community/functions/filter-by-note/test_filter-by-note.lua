local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_filter_basic_substring = {
  config = {
    filter_note = "important",
    case_sensitive = false,
    match_exactly = false,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "important note",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "other",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "IMPORTANT data",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 40.0,
          note = "unrelated",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should match notes containing "important" (case insensitive)
    test.assertEquals(2, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("important note", result[1].note)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("IMPORTANT data", result[2].note)
  end,
}

M.test_filter_exact_match = {
  config = {
    filter_note = "exact",
    case_sensitive = false,
    match_exactly = true,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "exact",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "exact_match",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "other",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should match only "exact" (exact match, case insensitive)
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("exact", result[1].note)
  end,
}

M.test_filter_case_sensitive = {
  config = {
    filter_note = "Note",
    case_sensitive = true,
    match_exactly = false,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "Note1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "note2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "NOTE3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only match "Note1" (case sensitive substring)
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("Note1", result[1].note)
  end,
}

M.test_filter_no_config = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "note1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "note2",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Without filter_note, all data points should pass through
    test.assertEquals(2, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
  end,
}

M.test_filter_invert = {
  config = {
    filter_note = "skip",
    case_sensitive = false,
    match_exactly = false,
    invert = true,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          note = "skip this",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          note = "keep",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          note = "also keep",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- With invert=true, should only get items that DON'T match "skip"
    test.assertEquals(2, #result)
    test.assertEquals(20.0, result[1].value)
    test.assertEquals("keep", result[1].note)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("also keep", result[2].note)
  end,
}

return M
