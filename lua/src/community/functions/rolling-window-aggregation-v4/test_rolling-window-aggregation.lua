local M = {}

local core = require("tng.core")
local test = require("test.core")

local DHOUR = core.DURATION.HOUR
local PMON = core.PERIOD.MONTH
local PDAY = core.PERIOD.DAY
local now = core.time().timestamp
local shift = core.shift

M["test window is inclusive and preserves data"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 1,
          label = "first",
          note = "first note"
        },
        {
          timestamp = now - DHOUR,
          value = 1,
          label = "second",
          note = "second note"
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)

    -- Default placement is "end" (newest data point in window)
    test.assertEquals(now, result[1].timestamp)
    test.assertEquals(2, result[1].value)
    test.assertEquals("first", result[1].label)
    test.assertEquals("first note", result[1].note)

    test.assertEquals(now - DHOUR, result[2].timestamp)
    test.assertEquals(1, result[2].value)
    test.assertEquals("second", result[2].label)
    test.assertEquals("second note", result[2].note)
  end
}

M["test window rolling includes relevant data"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        { timestamp = now, },
        { timestamp = now - (DHOUR * 2), },
        { timestamp = now - (DHOUR * 3), },
        { timestamp = now - (DHOUR * 3.5), },
        { timestamp = now - (DHOUR * 4), },
        { timestamp = now - (DHOUR * 4.3), },
        { timestamp = now - (DHOUR * 4.6), },
        { timestamp = now - (DHOUR * 4.9), },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(8, #result)

    test.assertEquals(1, result[1].value)
    test.assertEquals(2, result[2].value)
    test.assertEquals(3, result[3].value)
    test.assertEquals(3, result[4].value)
    test.assertEquals(4, result[5].value)
    test.assertEquals(3, result[6].value)
    test.assertEquals(2, result[7].value)
    test.assertEquals(1, result[8].value)
  end
}

M["test window rolling includes relevant data with period and multiplier"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 2,
    window = "_months"
  },
  sources = function()
    return {
      {
        { timestamp = now, },
        { timestamp = shift(now, PDAY, -1).timestamp },
        { timestamp = shift(now, PDAY, -10).timestamp },
        { timestamp = shift(now, PMON, -2).timestamp },
        { timestamp = shift(now, PMON, -3).timestamp },
        { timestamp = shift(now, PMON, -5).timestamp },
        { timestamp = shift(now, PMON, -8).timestamp },
        { timestamp = shift(now, PMON, -9).timestamp },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(8, #result)

    test.assertEquals(4, result[1].value)
    test.assertEquals(3, result[2].value)
    test.assertEquals(2, result[3].value)
    test.assertEquals(2, result[4].value)
    test.assertEquals(2, result[5].value)
    test.assertEquals(1, result[6].value)
    test.assertEquals(2, result[7].value)
    test.assertEquals(1, result[8].value)
  end
}

M["test empty window returns empty"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return { { } }
  end,
  assertions = function(result)
    test.assertEquals(0, #result)
  end
}

M["test min works"] = {
  config = {
    aggregation_type = "_min",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertEquals(3, result[1].value)
    test.assertEquals(3, result[2].value)
    test.assertEquals(5, result[3].value)
  end
}

M["test max works"] = {
  config = {
    aggregation_type = "_max",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertEquals(8, result[1].value)
    test.assertEquals(5, result[2].value)
    test.assertEquals(5, result[3].value)
  end
}

M["test average works"] = {
  config = {
    aggregation_type = "_average",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertEquals(5.5, result[1].value)
    test.assertEquals(4, result[2].value)
    test.assertEquals(5, result[3].value)
  end
}

M["test sum works"] = {
  config = {
    aggregation_type = "_sum",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertEquals(11, result[1].value)
    test.assertEquals(8, result[2].value)
    test.assertEquals(5, result[3].value)
  end
}

M["test variance works"] = {
  config = {
    aggregation_type = "_variance",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertClose(2.5 * 2.5, result[1].value, 0.0001)
    test.assertEquals(1, result[2].value)
    test.assertEquals(0, result[3].value)
  end
}

M["test standard deviation works"] = {
  config = {
    aggregation_type = "_standard_deviation",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 8,
        },
        {
          timestamp = now - DHOUR,
          value = 3,
        },
        {
          timestamp = now - (2 * DHOUR),
          value = 5,
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(3, #result)

    test.assertClose(2.5, result[1].value, 0.0001)
    test.assertEquals(1, result[2].value)
    test.assertEquals(0, result[3].value)
  end
}

M["test midpoint placement preserves old behavior"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours",
    placement = "_window_midpoint"
  },
  sources = function()
    return {
      {
        {
          timestamp = now,
          value = 1,
          label = "first",
          note = "first note"
        },
        {
          timestamp = now - DHOUR,
          value = 1,
          label = "second",
          note = "second note"
        },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(2, #result)

    test.assertEquals(now - (DHOUR / 2), result[1].timestamp)
    test.assertEquals(2, result[1].value)

    test.assertEquals(now - DHOUR, result[2].timestamp)
    test.assertEquals(1, result[2].value)
  end
}

return M
