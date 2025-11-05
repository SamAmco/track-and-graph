local M = {}
local core = require("tng.core")
local test = require("test.core")
local DDAY = core.DURATION.DAY
local now = core.time().timestamp

M.test_filter_before_last_basic = {
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
    -- Should only include points before the reference (timestamp < now - 2 days)
    test.assertEquals(2, #result)
    test.assertEquals(20.0, result[1].value)
    test.assertEquals("before", result[1].label)
    test.assertEquals(now - (DDAY * 3), result[1].timestamp)
    
    test.assertEquals(30.0, result[2].value)
    test.assertEquals("way_before", result[2].label)
    test.assertEquals(now - (DDAY * 5), result[2].timestamp)
  end,
}

M.test_filter_before_last_no_reference = {
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

M.test_filter_before_last_all_before = {
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
    -- All points are before the reference, all should pass
    test.assertEquals(2, #result)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals(20.0, result[2].value)
  end,
}

M.test_filter_before_last_all_after = {
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
    -- All points are after the reference, none should pass
    test.assertEquals(0, #result)
  end,
}

M.test_filter_before_last_exact_boundary = {
  config = {},
  sources = function()
    return {
      { -- Source 1: data with one exactly at reference time
        {
          timestamp = 1000000000001,  -- After reference
          value = 30.0,
          label = "after_reference",
        },
        {
          timestamp = 1000000000000,  -- Exactly at reference
          value = 10.0,
          label = "at_reference",
        },
        {
          timestamp = 999999999999,  -- Before reference
          value = 20.0,
          label = "before_reference",
        },
      },
      { -- Source 2: reference point
        {
          timestamp = 1000000000000,  -- Fixed timestamp
          value = 100.0,
          label = "reference",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    -- Only points strictly before reference should pass (not including exact match)
    test.assertEquals(1, #result)
    test.assertEquals(20.0, result[1].value)
    test.assertEquals("before_reference", result[1].label)
  end,
}

M.test_filter_before_last_preserves_fields = {
  config = {},
  sources = function()
    return {
      { -- Source 1: data with all fields
        {
          timestamp = now - (DDAY * 5),
          value = 42.5,
          label = "test_label",
          note = "test_note",
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
    test.assertEquals(1, #result)
    -- All fields should be preserved
    test.assertEquals(42.5, result[1].value)
    test.assertEquals("test_label", result[1].label)
    test.assertEquals("test_note", result[1].note)
  end,
}

return M
