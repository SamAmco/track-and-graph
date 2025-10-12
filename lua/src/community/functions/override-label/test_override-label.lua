local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_override_label_basic = {
  config = {
    new_label = "new_label",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "old_label_1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "old_label_2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "old_label_3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(3, #result)

    -- All labels should be overridden
    test.assertEquals("new_label", result[1].label)
    test.assertEquals("new_label", result[2].label)
    test.assertEquals("new_label", result[3].label)

    -- Values and timestamps should be preserved
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals(30.0, result[3].value)
  end,
}

M.test_override_label_no_config = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 5.0,
          label = "original",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Without config, label should remain unchanged
    test.assertEquals("original", result[1].label)
    test.assertEquals(5.0, result[1].value)
  end,
}

M.test_override_label_empty_string = {
  config = {
    new_label = "",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      {
        {
          timestamp = now,
          value = 15.0,
          label = "original",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(1, #result)
    -- Empty string should be a valid override
    test.assertEquals("", result[1].label)
    test.assertEquals(15.0, result[1].value)
  end,
}

return M
