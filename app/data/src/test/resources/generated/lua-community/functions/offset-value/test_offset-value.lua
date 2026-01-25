local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_offset_positive = {
  config = {
    offset = 10.0,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "test1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 0.0,
          label = "test2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = -5.0,
          label = "test3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(3, #result)

    -- Check that values were offset by +10.0
    test.assertEquals(15.0, result[1].value)
    test.assertEquals(10.0, result[2].value)
    test.assertEquals(5.0, result[3].value)

    -- Check that labels are preserved
    test.assertEquals("test1", result[1].label)
    test.assertEquals("test2", result[2].label)
    test.assertEquals("test3", result[3].label)
  end,
}

M.test_offset_negative = {
  config = {
    offset = -10.0,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 20.0,
          label = "test1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 10.0,
          label = "test2",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(2, #result)

    -- Check that values were offset by -10.0
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(0.0, result[2].value)
  end,
}

M.test_offset_default = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 7.5,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Default offset should be 0.0 (identity)
    test.assertEquals(7.5, result[1].value)
  end,
}

M.test_offset_zero = {
  config = {
    offset = 0.0,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 100.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Zero offset should not change value
    test.assertEquals(100.0, result[1].value)
  end,
}

return M
