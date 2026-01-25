local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_filter_basic_substring = {
  config = {
    filter_label = "test",
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
          label = "test1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "other",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "TEST2",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 40.0,
          label = "another",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should match "test1" and "TEST2" (case insensitive substring)
    test.assertEquals(2, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("test1", result[1].label)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("TEST2", result[2].label)
  end,
}

M.test_filter_exact_match = {
  config = {
    filter_label = "exact",
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
          label = "exact",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "exact_match",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "other",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should match only "exact" (exact match, case insensitive would match "EXACT" too but we don't have one)
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("exact", result[1].label)
  end,
}

M.test_filter_case_sensitive = {
  config = {
    filter_label = "Test",
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
          label = "Test1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "test2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "TEST3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only match "Test1" (case sensitive substring)
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("Test1", result[1].label)
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
          label = "label1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "label2",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Without filter_label, all data points should pass through
    test.assertEquals(2, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
  end,
}

M.test_filter_invert = {
  config = {
    filter_label = "keep",
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
          label = "keep_this",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "remove",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "also_remove",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- With invert=true, should only get items that DON'T match "keep"
    test.assertEquals(2, #result)
    test.assertEquals(20.0, result[1].value)
    test.assertEquals("remove", result[1].label)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("also_remove", result[2].label)
  end,
}

return M
