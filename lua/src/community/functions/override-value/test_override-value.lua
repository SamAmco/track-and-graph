local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_override_value_basic = {
  config = {
    new_value = 100.0,
  },
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
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "label3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(3, #result)

    -- All values should be overridden
    test.assertEquals(100.0, result[1].value)
    test.assertEquals(100.0, result[2].value)
    test.assertEquals(100.0, result[3].value)

    -- Labels should be preserved
    test.assertEquals("label1", result[1].label)
    test.assertEquals("label2", result[2].label)
    test.assertEquals("label3", result[3].label)
  end,
}

M.test_override_value_no_config = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 5.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Without config, value should remain unchanged
    test.assertEquals(5.0, result[1].value)
    test.assertEquals("test", result[1].label)
  end,
}

M.test_override_value_zero = {
  config = {
    new_value = 0.0,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 15.0,
          label = "test",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Zero should be a valid override
    test.assertEquals(0.0, result[1].value)
    test.assertEquals("test", result[1].label)
  end,
}

M.test_override_value_negative = {
  config = {
    new_value = -42.5,
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
    -- Negative values should work
    test.assertEquals(-42.5, result[1].value)
    test.assertEquals("test", result[1].label)
  end,
}

return M
