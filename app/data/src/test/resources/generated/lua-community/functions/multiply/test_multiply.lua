local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_multiply_basic = {
  config = {
    multiplier = "2.5",
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
          value = 4.0,
          label = "test2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = -2.0,
          label = "test3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)

    -- For functions, result should be an array of transformed data points
    test.assertEquals(3, #result)

    -- Check that values were multiplied by 2.5
    test.assertEquals(25.0, result[1].value)  -- 10.0 * 2.5
    test.assertEquals(10.0, result[2].value)  -- 4.0 * 2.5
    test.assertEquals(-5.0, result[3].value)  -- -2.0 * 2.5

    -- Check that other properties are preserved (including timestamp)
    test.assertEquals("test1", result[1].label)
    test.assertEquals("test2", result[2].label)
    test.assertEquals("test3", result[3].label)
  end,
}

M.test_multiply_by_zero = {
  config = {
    multiplier = "0",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 100.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    test.assertEquals(0.0, result[1].value)
  end,
}

M.test_multiply_negative_multiplier = {
  config = {
    multiplier = "-3",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 5.0,
          label = "pos",
        },
        {
          timestamp = now - DDAY,
          value = -4.0,
          label = "neg",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(-15.0, result[1].value)  -- 5.0 * -3
    test.assertEquals(12.0, result[2].value)   -- -4.0 * -3
  end,
}

M.test_multiply_with_default_config = {
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
    -- Default multiplier should be 1.0 (identity)
    test.assertEquals(7.5, result[1].value)
  end,
}

M.test_multiply_double_precision = {
  config = {
    multiplier = "1.23456789",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 9876543210.123456,
          label = "large",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Verify double precision is maintained
    local expected = 9876543210.123456 * 1.23456789
    test.assertEquals(expected, result[1].value)
  end,
}

return M
