local M = {}

local core = require("tng.core")
local test = require("test.core")

local DHOUR = core.DURATION.HOUR
local DDAY = core.DURATION.DAY
local DWEEK = core.DURATION.WEEK

M.test_default_last_7_days = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DHOUR, -- Recent, should be included
          value = 5,
        },
        {
          timestamp = now - DDAY,
          value = 1,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 2,
        },
        {
          timestamp = now - (DDAY * 3),
          value = 3,
        },
        {
          timestamp = now - (DDAY * 8), -- Outside 7-day window
          value = 4,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(11, result.text) -- 1 + 2 + 3 + 5 = 11 (excludes the 8-day-old value)
  end,
}

M.test_custom_duration_24_hours = {
  config = {
    duration = "core.DURATION.HOUR",
    multiplier = "24",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DHOUR * 2), -- 2 hours ago
          value = 40,
        },
        {
          timestamp = now - (DHOUR * 12), -- 12 hours ago
          value = 10,
        },
        {
          timestamp = now - (DHOUR * 23), -- 23 hours ago
          value = 20,
        },
        {
          timestamp = now - (DHOUR * 25), -- 25 hours ago, outside window
          value = 30,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(70, result.text) -- 10 + 20 + 40 = 70 (excludes 25-hour-old value)
  end,
}

M.test_multi_source_output = {
  config = {
    duration = "core.DURATION.DAY",
    multiplier = "3",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DDAY,
          value = 1,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 2,
        },
        {
          timestamp = now - (DDAY * 4), -- Outside 3-day window
          value = 10,
        },
      },
      source2 = {
        {
          timestamp = now - DDAY,
          value = 3,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 6,
        },
        {
          timestamp = now - (DDAY * 4), -- Outside 3-day window
          value = 20,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("12\nsource2: 9\nsource1: 3", result.text) -- source1: 1+2=3, source2: 3+6=9, total: 12
  end,
}

M.test_empty_result = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DWEEK - DDAY, -- Outside 7-day window
          value = 100,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("0", result.text)
  end,
}

return M
