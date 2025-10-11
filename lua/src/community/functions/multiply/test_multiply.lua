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

    -- Check that other properties are preserved
    test.assertEquals("test1", result[1].label)
    test.assertEquals("test2", result[2].label)
    test.assertEquals("test3", result[3].label)
  end,
}

return M
