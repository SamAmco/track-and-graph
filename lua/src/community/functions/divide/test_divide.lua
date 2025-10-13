local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_divide_basic = {
  config = {
    divisor = 2.0,
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
          value = -8.0,
          label = "test3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(3, #result)

    -- Check that values were divided by 2.0
    test.assertEquals(5.0, result[1].value)
    test.assertEquals(2.0, result[2].value)
    test.assertEquals(-4.0, result[3].value)

    -- Check that labels are preserved
    test.assertEquals("test1", result[1].label)
    test.assertEquals("test2", result[2].label)
    test.assertEquals("test3", result[3].label)
  end,
}

M.test_divide_fractional = {
  config = {
    divisor = 0.5,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Dividing by 0.5 is same as multiplying by 2
    test.assertEquals(20.0, result[1].value)
  end,
}

M.test_divide_default = {
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
    -- Default divisor should be 1.0 (identity)
    test.assertEquals(7.5, result[1].value)
  end,
}

M.test_divide_negative_divisor = {
  config = {
    divisor = -4.0,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 8.0,
          label = "pos",
        },
        {
          timestamp = now - (DDAY * 2),
          value = -12.0,
          label = "neg",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(2, #result)
    -- Dividing by negative should flip sign
    test.assertEquals(-2.0, result[1].value)
    test.assertEquals(3.0, result[2].value)
  end,
}

return M
