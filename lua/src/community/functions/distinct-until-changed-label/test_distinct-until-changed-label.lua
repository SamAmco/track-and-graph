local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_distinct_label_basic = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 40.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 5),
          value = 50.0,
          label = "A",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep first A, first B, then A again (when it changes)
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("A", result[1].label)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("B", result[2].label)
    test.assertEquals(50.0, result[3].value)
    test.assertEquals("A", result[3].label)
  end,
}

M.test_distinct_label_all_same = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "same",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "same",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "same",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep the first one
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("same", result[1].label)
  end,
}

M.test_distinct_label_all_different = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should keep all of them
    test.assertEquals(3, #result)
    test.assertEquals("A", result[1].label)
    test.assertEquals("B", result[2].label)
    test.assertEquals("C", result[3].label)
  end,
}

return M
