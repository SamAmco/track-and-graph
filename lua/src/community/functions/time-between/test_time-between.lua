local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY
local DHOUR = core.DURATION.HOUR
local now = core.time().timestamp

M.test_time_between_basic = {
  config = {
    include_first = false,
  },
  sources = function()
    return {
      {
        {
          timestamp = now - (DDAY * 1),
          value = 100.0,
          label = "point1",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 200.0,
          label = "point2",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 300.0,
          label = "point3",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should have 2 results (first is skipped without include_first)
    test.assertEquals(2, #result)

    -- Point1 with duration to point2 = 1 day = 86400 seconds
    test.assertEquals(now - (DDAY * 1), result[1].timestamp)
    test.assertEquals(86400.0, result[1].value)
    test.assertEquals("point1", result[1].label)

    -- Point2 with duration to point3 = 1 day = 86400 seconds
    test.assertEquals(now - (DDAY * 2), result[2].timestamp)
    test.assertEquals(86400.0, result[2].value)
    test.assertEquals("point2", result[2].label)
  end,
}

M.test_time_between_include_first = {
  config = {
    include_first = true,
  },
  sources = function()
    return {
      {
        {
          timestamp = now - (DHOUR * 2),
          value = 100.0,
          label = "point1",
        },
        {
          timestamp = now - (DHOUR * 5),
          value = 200.0,
          label = "point2",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should have 2 results (includes first)
    test.assertEquals(2, #result)

    -- First output: synthetic point with time from now to point1 = ~2 hours = ~7200 seconds
    test.assertEquals(now - (DHOUR * 2), result[1].timestamp)
    test.assertClose(7200.0, result[1].value, 1.0)  -- Allow 1 second tolerance for timing
    test.assertEquals("", result[1].label)  -- Synthetic point has empty label
    test.assertEquals("", result[1].note)   -- Synthetic point has empty note

    -- Second output: point1 with duration to point2 = 3 hours = 10800 seconds
    test.assertEquals(now - (DHOUR * 2), result[2].timestamp)
    test.assertEquals(10800.0, result[2].value)
    test.assertEquals("point1", result[2].label)
  end,
}

M.test_time_between_single_point_no_include = {
  config = {
    include_first = false,
  },
  sources = function()
    return {
      {
        {
          timestamp = now - DDAY,
          value = 100.0,
          label = "only",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should have 0 results (single point, first is skipped)
    test.assertEquals(0, #result)
  end,
}

M.test_time_between_single_point_with_include = {
  config = {
    include_first = true,
  },
  sources = function()
    return {
      {
        {
          timestamp = now - (DHOUR * 6),
          value = 100.0,
          label = "only",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Should have 1 result (synthetic point with time from now)
    test.assertEquals(1, #result)
    test.assertEquals(now - (DHOUR * 6), result[1].timestamp)
    test.assertClose(21600.0, result[1].value, 1.0)  -- ~6 hours = ~21600 seconds, allow timing variance
    test.assertEquals("", result[1].label)  -- Synthetic point has empty label
    test.assertEquals("", result[1].note)   -- Synthetic point has empty note
  end,
}

return M
