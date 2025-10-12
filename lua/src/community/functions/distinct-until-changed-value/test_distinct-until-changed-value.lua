local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_distinct_value_basic = {
  config = {},
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
          value = 10.0,
          label = "test2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 20.0,
          label = "test3",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 20.0,
          label = "test4",
        },
        {
          timestamp = now - (DDAY * 5),
          value = 10.0,
          label = "test5",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep first 10.0, first 20.0, then 10.0 again
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("test1", result[1].label)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals("test3", result[2].label)
    test.assertEquals(10.0, result[3].value)
    test.assertEquals("test5", result[3].label)
  end,
}

M.test_distinct_value_all_same = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 42.0,
          label = "label1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 42.0,
          label = "label2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 42.0,
          label = "label3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep the first one
    test.assertEquals(1, #result)
    test.assertEquals(42.0, result[1].value)
    test.assertEquals("label1", result[1].label)
  end,
}

M.test_distinct_value_all_different = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "test",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "test",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should keep all of them
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals(30.0, result[3].value)
  end,
}

return M
