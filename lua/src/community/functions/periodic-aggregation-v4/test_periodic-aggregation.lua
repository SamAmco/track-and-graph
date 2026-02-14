local M = {}

local core = require("tng.core")
local test = require("test.core")

local PDAY = core.PERIOD.DAY
local PWEEK = core.PERIOD.WEEK
local PMON = core.PERIOD.MONTH
local PYEAR = core.PERIOD.YEAR

-- Use a fixed date for deterministic tests: 2024-06-15 12:00:00 UTC
local fixed_date = core.time { year = 2024, month = 6, day = 15, hour = 12, min = 0, sec = 0, zone = "UTC" }
local shift = core.shift

-- Helper to format dates for debugging
local format_date = function(dt)
  return core.format(dt, "yyyy-MM-dd HH:mm:ss z")
end

local format = function(dt)
  return core.format(core.date(dt), "yyyy-MM-dd HH:mm:ss")
end

M["test daily period groups by calendar day and preserves data"] = {
  config = {
    aggregation_type = "_count",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        {
          timestamp = day_start.timestamp + 10000,
          offset = 0,
          value = 1,
          label = "first",
          note = "first note"
        },
        {
          timestamp = prev_day_start.timestamp + 20000,
          offset = 0,
          value = 2,
          label = "second",
          note = "second note"
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(1, result[1].value)
    test.assertEquals("first", result[1].label)
    test.assertEquals("first note", result[1].note)
    test.assertEquals(1, result[2].value)
    test.assertEquals("second", result[2].label)
    test.assertEquals("second note", result[2].note)
  end
}

M["test daily period aggregates multiple points in same day"] = {
  config = {
    aggregation_type = "_count",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000,     value = 3 },
        { timestamp = day_start.timestamp + 5000,      value = 2 },
        { timestamp = day_start.timestamp + 1000,      value = 1 },
        { timestamp = prev_day_start.timestamp + 8000, value = 5 },
        { timestamp = prev_day_start.timestamp + 2000, value = 4 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(2, result[2].value)
  end
}

M["test weekly period groups by calendar week"] = {
  config = {
    aggregation_type = "_count",
    period = "_weeks"
  },
  sources = function()
    local week_end = core.get_end_of_period(PWEEK, fixed_date)
    local week_start = shift(week_end, PWEEK, -1)
    local prev_week_start = shift(week_start, PWEEK, -1)
    local prev2_week_start = shift(prev_week_start, PWEEK, -1)
    return {
      {
        { timestamp = week_start.timestamp + 30000,       value = 1 },
        { timestamp = week_start.timestamp + 20000,       value = 2 },
        { timestamp = week_start.timestamp + 10000,       value = 3 },
        { timestamp = prev_week_start.timestamp + 20000,  value = 4 },
        { timestamp = prev_week_start.timestamp + 10000,  value = 5 },
        { timestamp = prev2_week_start.timestamp + 10000, value = 6 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(2, result[2].value)
    test.assertEquals(1, result[3].value)
  end
}

M["test monthly period groups by calendar month"] = {
  config = {
    aggregation_type = "_count",
    period = "_months"
  },
  sources = function()
    local month_end = core.get_end_of_period(PMON, fixed_date)
    local month_start = shift(month_end, PMON, -1)
    local prev_month_start = shift(month_start, PMON, -1)
    local prev2_month_start = shift(prev_month_start, PMON, -1)
    return {
      {
        { timestamp = month_start.timestamp + 300000,       value = 1 },
        { timestamp = month_start.timestamp + 200000,       value = 2 },
        { timestamp = month_start.timestamp + 100000,       value = 3 },
        { timestamp = prev_month_start.timestamp + 200000,  value = 4 },
        { timestamp = prev_month_start.timestamp + 100000,  value = 5 },
        { timestamp = prev2_month_start.timestamp + 200000, value = 6 },
        { timestamp = prev2_month_start.timestamp + 100000, value = 7 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(2, result[2].value)
    test.assertEquals(2, result[3].value)
  end
}

M["test yearly period groups by calendar year"] = {
  config = {
    aggregation_type = "_count",
    period = "_years"
  },
  sources = function()
    local year_end = core.get_end_of_period(PYEAR, fixed_date)
    local year_start = shift(year_end, PYEAR, -1)
    local prev_year_start = shift(year_start, PYEAR, -1)
    local prev2_year_start = shift(prev_year_start, PYEAR, -1)
    return {
      {
        { timestamp = year_start.timestamp + 3000000,       value = 1 },
        { timestamp = year_start.timestamp + 2000000,       value = 2 },
        { timestamp = year_start.timestamp + 1000000,       value = 3 },
        { timestamp = prev_year_start.timestamp + 2000000,  value = 4 },
        { timestamp = prev_year_start.timestamp + 1000000,  value = 5 },
        { timestamp = prev2_year_start.timestamp + 1000000, value = 6 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(2, result[2].value)
    test.assertEquals(1, result[3].value)
  end
}

M["test empty input returns empty"] = {
  config = {
    aggregation_type = "_count",
    period = "_days"
  },
  sources = function()
    return { {} }
  end,
  assertions = function(result)
    test.assertEquals(0, #result)
  end
}

M["test min works"] = {
  config = {
    aggregation_type = "_min",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000,     value = 5 },
        { timestamp = day_start.timestamp + 5000,      value = 3 },
        { timestamp = day_start.timestamp + 1000,      value = 8 },
        { timestamp = prev_day_start.timestamp + 8000, value = 7 },
        { timestamp = prev_day_start.timestamp + 2000, value = 12 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(7, result[2].value)
  end
}

M["test max works"] = {
  config = {
    aggregation_type = "_max",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000,     value = 5 },
        { timestamp = day_start.timestamp + 5000,      value = 3 },
        { timestamp = day_start.timestamp + 1000,      value = 8 },
        { timestamp = prev_day_start.timestamp + 8000, value = 7 },
        { timestamp = prev_day_start.timestamp + 2000, value = 12 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(8, result[1].value)
    test.assertEquals(12, result[2].value)
  end
}

M["test average works"] = {
  config = {
    aggregation_type = "_average",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000,     value = 6 },
        { timestamp = day_start.timestamp + 5000,      value = 4 },
        { timestamp = day_start.timestamp + 1000,      value = 8 },
        { timestamp = prev_day_start.timestamp + 8000, value = 20 },
        { timestamp = prev_day_start.timestamp + 2000, value = 10 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(6, result[1].value)
    test.assertEquals(15, result[2].value)
  end
}

M["test sum works"] = {
  config = {
    aggregation_type = "_sum",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    local prev_day_start = shift(day_start, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000,     value = 5 },
        { timestamp = day_start.timestamp + 5000,      value = 3 },
        { timestamp = day_start.timestamp + 1000,      value = 8 },
        { timestamp = prev_day_start.timestamp + 8000, value = 7 },
        { timestamp = prev_day_start.timestamp + 2000, value = 12 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)
    test.assertEquals(16, result[1].value)
    test.assertEquals(19, result[2].value)
  end
}

M["test variance works"] = {
  config = {
    aggregation_type = "_variance",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000, value = 5 },
        { timestamp = day_start.timestamp + 5000,  value = 3 },
        { timestamp = day_start.timestamp + 1000,  value = 8 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(1, #result)

    local mean = (8 + 3 + 5) / 3
    local sqd1 = (8 - mean) * (8 - mean)
    local sqd2 = (3 - mean) * (3 - mean)
    local sqd3 = (5 - mean) * (5 - mean)
    local variance = (sqd1 + sqd2 + sqd3) / 3

    test.assertClose(variance, result[1].value, 0.0001)
  end
}

M["test standard deviation works"] = {
  config = {
    aggregation_type = "_standard_deviation",
    period = "_days"
  },
  sources = function()
    local day_end = core.get_end_of_period(PDAY, fixed_date)
    local day_start = shift(day_end, PDAY, -1)
    return {
      {
        { timestamp = day_start.timestamp + 10000, value = 5 },
        { timestamp = day_start.timestamp + 5000,  value = 3 },
        { timestamp = day_start.timestamp + 1000,  value = 8 },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(1, #result)

    local mean = (8 + 3 + 5) / 3
    local sqd1 = (8 - mean) * (8 - mean)
    local sqd2 = (3 - mean) * (3 - mean)
    local sqd3 = (5 - mean) * (5 - mean)
    local variance = (sqd1 + sqd2 + sqd3) / 3

    test.assertClose(math.sqrt(variance), result[1].value, 0.0001)
  end
}

return M
