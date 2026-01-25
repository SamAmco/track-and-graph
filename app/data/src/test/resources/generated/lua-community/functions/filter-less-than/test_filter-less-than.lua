local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_filter_less_than_basic = {
  config = {
    threshold = 50.0,
    include_equal = false,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 100.0,
          label = "high",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 50.0,
          label = "equal",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "low",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 25.0,
          label = "lower",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only keep values < 50.0 (not equal)
    test.assertEquals(2, #result)
    test.assertEquals(30.0, result[1].value)
    test.assertEquals("low", result[1].label)
    test.assertEquals(25.0, result[2].value)
    test.assertEquals("lower", result[2].label)
  end,
}

M.test_filter_less_than_include_equal = {
  config = {
    threshold = 50.0,
    include_equal = true,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 100.0,
          label = "high",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 50.0,
          label = "equal",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "low",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should keep values <= 50.0 (including equal)
    test.assertEquals(2, #result)
    test.assertEquals(50.0, result[1].value)
    test.assertEquals("equal", result[1].label)
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("low", result[2].label)
  end,
}

M.test_filter_less_than_default_threshold = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "positive",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 0.0,
          label = "zero",
        },
        {
          timestamp = now - (DDAY * 3),
          value = -10.0,
          label = "negative",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Default threshold is 0.0, should only keep negative values
    test.assertEquals(1, #result)
    test.assertEquals(-10.0, result[1].value)
    test.assertEquals("negative", result[1].label)
  end,
}

M.test_filter_less_than_negative_threshold = {
  config = {
    threshold = -25.0,
    include_equal = false,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "positive",
        },
        {
          timestamp = now - (DDAY * 2),
          value = -10.0,
          label = "neg_small",
        },
        {
          timestamp = now - (DDAY * 3),
          value = -50.0,
          label = "neg_large",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should keep values < -25.0
    test.assertEquals(1, #result)
    test.assertEquals(-50.0, result[1].value)
    test.assertEquals("neg_large", result[1].label)
  end,
}

return M
