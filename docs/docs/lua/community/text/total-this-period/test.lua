local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local DWEEK = core.DURATION.WEEK
local PWEEK = core.PERIOD.WEEK
local PMONTH = core.PERIOD.MONTH

M.test_default_period_week = {
  config = {},
  sources = function()
    local end_of_week_date = core.get_end_of_period(PWEEK, core.time().timestamp)
    local end_of_week = core.time(end_of_week_date).timestamp
    return {
      source1 = {
        {
          timestamp = end_of_week - DDAY,
          value = 1,
        },
        {
          timestamp = end_of_week - (DDAY * 2),
          value = 2,
        },
        {
          timestamp = end_of_week - (DDAY * 3),
          value = 3,
        },
        {
          timestamp = end_of_week - DWEEK - DDAY,
          value = 4,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(6, result.text)
    test.assertEquals(3, result.size) -- Large text for single source
  end,
}

M.test_period_month = {
  config = {
    period = "core.PERIOD.MONTH",
  },
  sources = function()
    local end_of_month_date = core.get_end_of_period(PMONTH, core.time().timestamp)
    local end_of_month = core.time(end_of_month_date).timestamp
    return {
      source1 = {
        {
          timestamp = core.shift(end_of_month, PWEEK, -1).timestamp,
          value = 1,
        },
        {
          timestamp = core.shift(end_of_month, PWEEK, -2).timestamp,
          value = 2,
        },
        {
          timestamp = core.shift(end_of_month, PWEEK, -3).timestamp,
          value = 3,
        },
        {
          timestamp = core.shift(end_of_month, PWEEK, -6).timestamp,
          value = 4,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(6, result.text)
    test.assertEquals(3, result.size) -- Large text for single source
  end,
}

M.test_multi_source_output = {
  config = {},
  sources = function()
    local end_of_week_date = core.get_end_of_period(PWEEK, core.time().timestamp)
    local end_of_week = core.time(end_of_week_date).timestamp
    return {
      source1 = {
        {
          timestamp = end_of_week - DDAY,
          value = 1,
        },
        {
          timestamp = end_of_week - (DDAY * 2),
          value = 2,
        },
        {
          timestamp = end_of_week - (DDAY * 3),
          value = 3,
        },
        {
          timestamp = end_of_week - DWEEK - DDAY,
          value = 4,
        },
      },
      source2 = {
        {
          timestamp = end_of_week - DDAY,
          value = 2,
        },
        {
          timestamp = end_of_week - (DDAY * 2),
          value = 4,
        },
        {
          timestamp = end_of_week - (DDAY * 3),
          value = 6,
        },
        {
          timestamp = end_of_week - DWEEK - DDAY,
          value = 8,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals("18\nsource2: 12\nsource1: 6", result.text)
    test.assertEquals(2, result.size) -- Medium text for multiple sources
  end,
}

M.test_text_size_override = {
  config = {
    text_size = "1", -- Override to small text for single source (normally would be large)
  },
  sources = function()
    local end_of_week_date = core.get_end_of_period(PWEEK, core.time().timestamp)
    local end_of_week = core.time(end_of_week_date).timestamp
    return {
      source1 = {
        {
          timestamp = end_of_week - DDAY,
          value = 5,
        },
        {
          timestamp = end_of_week - (DDAY * 2),
          value = 10,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(15, result.text)
    test.assertEquals(1, result.size) -- Small text override instead of default large
  end,
}

return M
