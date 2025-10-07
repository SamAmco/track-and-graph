local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local DHOUR = core.DURATION.HOUR

M.test_default_duration_7_days = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DDAY,
          value = 10,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20,
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30,
        },
        {
          timestamp = now - (DDAY * 8), -- Outside 7-day window
          value = 100,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("20", result.text) -- Average of 10, 20, 30 = 20
  end,
}

M.test_custom_duration_3_days = {
  config = {
    duration = "core.DURATION.DAY * 3",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DHOUR,
          value = 5,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 15,
        },
        {
          timestamp = now - (DDAY * 4), -- Outside 3-day window
          value = 50,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("10", result.text) -- Average of 5, 15 = 10
  end,
}

M.test_multi_source_combined_average = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DDAY,
          value = 10,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20,
        },
      },
      source2 = {
        {
          timestamp = now - DDAY,
          value = 30,
        },
        {
          timestamp = now - (DDAY * 3),
          value = 40,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("25", result.text) -- Average of 10, 20, 30, 40 = 25
  end,
}

M.test_no_data_points = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 8), -- Outside 7-day window
          value = 100,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(nil, result)
  end,
}

M.test_single_data_point = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DDAY,
          value = 42.5,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("42.5", result.text)
  end,
}

M.test_floating_point_precision = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - DDAY,
          value = 1.111,
        },
        {
          timestamp = now - (DDAY * 2),
          value = 2.222,
        },
        {
          timestamp = now - (DDAY * 3),
          value = 3.333,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("2.22", result.text) -- Average of 1.111, 2.222, 3.333 = 2.222 rounded to 2.22
  end,
}

return M
