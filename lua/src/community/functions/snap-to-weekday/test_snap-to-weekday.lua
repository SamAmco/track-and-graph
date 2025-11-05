local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

-- Test basic functionality - snapping to Monday preserving time
M.test_snap_to_monday_preserving_time_nearest = {
  config = {
    target_weekday = "_monday",
    direction = "_nearest",
  },
  sources = function()
    -- Create a Wednesday at 2:00 PM as test input
    local wednesday = core.time({
      year = 2023,
      month = 6,
      day = 14, -- Wednesday
      hour = 14,
      min = 30,
      sec = 45,
    })

    return {
      {
        {
          timestamp = wednesday.timestamp,
          offset = wednesday.offset,
          value = 10.0,
          label = "test",
          note = "note",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assert("result should have 1 data point", #result == 1)

    local snapped_date = core.date(result[1])
    test.assertEquals(1, snapped_date.wday)  -- Monday
    test.assertEquals(14, snapped_date.hour) -- 2 PM (preserved)
    test.assertEquals(30, snapped_date.min)  -- 30 min (preserved)
    test.assertEquals(45, snapped_date.sec)  -- 45 sec (preserved)
    test.assertEquals(10.0, result[1].value)
    test.assertEquals("test", result[1].label)
    test.assertEquals("note", result[1].note)
  end,
}

-- Test next direction
M.test_snap_to_friday_next = {
  config = {
    target_weekday = "_friday",
    direction = "_next",
  },
  sources = function()
    -- Create a Saturday at 2:15 PM (day after Friday)
    local saturday = core.time({
      year = 2023,
      month = 6,
      day = 17, -- Saturday
      hour = 14,
      min = 15,
      sec = 30,
    })

    return {
      {
        {
          timestamp = saturday.timestamp,
          offset = saturday.offset,
          value = 25.5,
          label = "friday_test",
          note = "after_friday",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assert("result should have 1 data point", #result == 1)

    local snapped_date = core.date(result[1])
    test.assertEquals(5, snapped_date.wday)  -- Friday
    test.assertEquals(14, snapped_date.hour) -- 2 PM (preserved)
    test.assertEquals(15, snapped_date.min)  -- 15 minutes (preserved)
    test.assertEquals(30, snapped_date.sec)  -- 30 seconds (preserved)
    -- Should be next Friday (June 23, 2023)
    test.assertEquals(23, snapped_date.day)
    test.assertEquals(25.5, result[1].value)
  end,
}

-- Test last direction
M.test_snap_to_sunday_last = {
  config = {
    target_weekday = "_sunday",
    direction = "_last",
  },
  sources = function()
    -- Create a Monday at 10:15:20 AM (after last Sunday)
    local monday = core.time({
      year = 2023,
      month = 6,
      day = 19, -- Monday
      hour = 10,
      min = 15,
      sec = 20,
    })

    return {
      {
        {
          timestamp = monday.timestamp,
          offset = monday.offset,
          value = 33.3,
          label = "monday_test",
          note = "after_sunday",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assert("result should have 1 data point", #result == 1)

    local snapped_date = core.date(result[1])
    test.assertEquals(7, snapped_date.wday)  -- Sunday
    test.assertEquals(10, snapped_date.hour) -- 10 AM (preserved)
    test.assertEquals(15, snapped_date.min)  -- 15 min (preserved)
    test.assertEquals(20, snapped_date.sec)  -- 20 sec (preserved)
    -- Should snap to previous Sunday (June 18, 2023)
    test.assertEquals(18, snapped_date.day)
  end,
}

-- Test exact match case (already on target weekday)
M.test_exact_match = {
  config = {
    target_weekday = "_wednesday",
    direction = "_nearest",
  },
  sources = function()
    -- Create exactly Wednesday at 12:00:00 PM
    local wednesday = core.time({
      year = 2023,
      month = 6,
      day = 14, -- Wednesday
      hour = 12,
      min = 0,
      sec = 0,
    })

    return {
      {
        {
          timestamp = wednesday.timestamp,
          offset = wednesday.offset,
          value = 42.0,
          label = "exact",
          note = "match",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assert("result should have 1 data point", #result == 1)

    local snapped_date = core.date(result[1])
    test.assertEquals(3, snapped_date.wday)    -- Wednesday
    test.assertEquals(12, snapped_date.hour)   -- 12 PM (preserved)
    test.assertEquals(0, snapped_date.min)     -- 0 min (preserved)
    test.assertEquals(0, snapped_date.sec)     -- 0 sec (preserved)
    test.assertEquals(14, snapped_date.day)    -- Same day
    test.assertEquals(2023, snapped_date.year) -- Same year
    test.assertEquals(42.0, result[1].value)
  end,
}

-- Test multiple data points
M.test_multiple_data_points = {
  config = {
    target_weekday = "_tuesday",
    direction = "_nearest",
  },
  sources = function()
    local base_time = core.time({
      year = 2023,
      month = 6,
      day = 12, -- Monday
      hour = 11,
      min = 30,
      sec = 15,
    })

    return {
      {
        {
          timestamp = base_time.timestamp,
          offset = base_time.offset,
          value = 1.0,
          label = "first",
          note = "point1",
        },
        {
          timestamp = base_time.timestamp - DDAY,
          offset = base_time.offset,
          value = 2.0,
          label = "second",
          note = "point2",
        },
        {
          timestamp = base_time.timestamp - (2 * DDAY),
          offset = base_time.offset,
          value = 3.0,
          label = "third",
          note = "point3",
        },
        {
          timestamp = base_time.timestamp - (3 * DDAY),
          offset = base_time.offset,
          value = 4.0,
          label = "fourth",
          note = "point4",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assert("result should have 4 data points", #result == 4)

    -- All should be snapped to Tuesday at their original times
    for i = 1, 3 do
      local dp = result[i]
      local snapped_date = core.date(dp)
      test.assertEquals(2, snapped_date.wday)  -- Tuesday
      test.assertEquals(13, snapped_date.day)  -- Tuesday
      test.assertEquals(11, snapped_date.hour) -- 4 PM (preserved)
      test.assertEquals(30, snapped_date.min)  -- 30 minutes (preserved)
      test.assertEquals(15, snapped_date.sec)  -- 15 seconds (preserved)
      test.assertEquals(i, dp.value)           -- Values should be preserved
    end

    local last_dp = result[4]
    local last_snapped_date = core.date(last_dp)
    test.assertEquals(2, last_snapped_date.wday)  -- Tuesday
    test.assertEquals(6, last_snapped_date.day)       -- Tuesday
    test.assertEquals(11, last_snapped_date.hour) -- 4 PM (pres
    test.assertEquals(30, last_snapped_date.min)  -- 30 minutes (preserved)
    test.assertEquals(15, last_snapped_date.sec)  -- 15 seconds (preserved)
    test.assertEquals(4, last_dp.value)           -- Value should be preserved
  end,
}

return M
