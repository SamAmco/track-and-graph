local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_filter_after_last_basic = {
  config = {},
  sources = function()
    return {
      { -- Source 1: data to filter
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "after",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 20.0,
          label = "before",
        },
        {
          timestamp = now - (DDAY * 5),
          value = 30.0,
          label = "way_before",
        },
      },
      { -- Source 2: reference point
        {
          timestamp = now - (DDAY * 2),
          value = 100.0,
          label = "reference",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should only include points after the reference (timestamp > now - 2 days)
    test.assertEquals(1, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("after", result[1].label)
    test.assertEquals(now - (DDAY * 1), result[1].timestamp)
  end,
}

M.test_filter_after_last_no_reference = {
  config = {},
  sources = function()
    return {
      { -- Source 1: data to filter
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "point1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "point2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "point3",
        },
      },
      { -- Source 2: empty
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- No reference point, so all data should pass through
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals(30.0, result[3].value)
  end,
}

M.test_filter_after_last_all_before = {
  config = {},
  sources = function()
    return {
      { -- Source 1: all before reference
        {
          timestamp = now - (DDAY * 5),
          value = 10.0,
          label = "old1",
        },
        {
          timestamp = now - (DDAY * 6),
          value = 20.0,
          label = "old2",
        },
      },
      { -- Source 2: reference point
        {
          timestamp = now - (DDAY * 1),
          value = 100.0,
          label = "reference",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- All points are before the reference, none should pass
    test.assertEquals(0, #result)
  end,
}

M.test_filter_after_last_all_after = {
  config = {},
  sources = function()
    return {
      { -- Source 1: all after reference
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "new1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "new2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 30.0,
          label = "new3",
        },
      },
      { -- Source 2: reference point
        {
          timestamp = now - (DDAY * 5),
          value = 100.0,
          label = "reference",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- All points are after the reference, all should pass
    test.assertEquals(3, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
    test.assertEquals(30.0, result[3].value)
  end,
}

return M
