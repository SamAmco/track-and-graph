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
    test.assertEquals(1, #result)

    test.assertEquals(now - (DHOUR / 2), result[1].timestamp)
    test.assertEquals(2, result[1].value)
    test.assertEquals("first", result[1].label)
    test.assertEquals("first note", result[1].note)
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
        { timestamp = now - (DHOUR * 4), },
        { timestamp = now - (DHOUR * 5), },
        { timestamp = now - (DHOUR * 7), },
        { timestamp = now - (DHOUR * 8), },
        { timestamp = now - (DHOUR * 10), },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(4, #result)

    test.assertEquals(1, result[1].value)
    test.assertEquals(4, result[2].value)
    test.assertEquals(2, result[3].value)
    test.assertEquals(1, result[4].value)
  end
}

M["test window rolling includes relevant data no clusters"] = {
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
        { timestamp = now - (DHOUR * 4), },
        { timestamp = now - (DHOUR * 6), },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(4, #result)

    test.assertEquals(1, result[1].value)
    test.assertEquals(1, result[2].value)
    test.assertEquals(1, result[3].value)
    test.assertEquals(1, result[4].value)
  end
}

M["test window rolling includes relevant data all clusters"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours"
  },
  sources = function()
    return {
      {
        { timestamp = now, },
        { timestamp = now - (DHOUR * 1), },
        { timestamp = now - (DHOUR * 2), },
        { timestamp = now - (DHOUR * 4), },
        { timestamp = now - (DHOUR * 5), },
        { timestamp = now - (DHOUR * 7), },
        { timestamp = now - (DHOUR * 8), },
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

M["test window rolling includes relevant data period and multiplier"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 2,
    window = "_months"
  },
  sources = function()
    return {
      {
        { timestamp = now, },
        { timestamp = shift(now, PMON, -2).timestamp },
        { timestamp = shift(now, PMON, -4).timestamp },
        { timestamp = shift(now, PMON, -8).timestamp },
        { timestamp = shift(now, PMON, -10).timestamp },
        { timestamp = shift(now, PMON, -24).timestamp },
        { timestamp = shift(now, PMON, -28).timestamp },
      }
    }
  end,
  assertions = function(result)
    test.assertEquals(4, #result)

    test.assertEquals(3, result[1].value)
    test.assertEquals(2, result[2].value)
    test.assertEquals(1, result[3].value)
    test.assertEquals(1, result[4].value)
  end
}

M["test empty window returns empty"] = {
  config = {
    aggregation_type = "_count",
    multiplier = 1,
    window = "_hours"
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
    test.assertEquals(1, #result)
    test.assertEquals(3, result[1].value)
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
    test.assertEquals(1, #result)
    test.assertEquals(8, result[1].value)
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
    test.assertEquals(1, #result)
    test.assertClose(5.33333, result[1].value, 0.00001)
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
    test.assertEquals(1, #result)
    test.assertEquals(16, result[1].value)
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
