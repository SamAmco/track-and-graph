local M = {}

local core = require("tng.core")
local test = require("test.core")

-- Test snapping to 9:00 AM with different directions
M.test_snap_to_9am_nearest = {
    config = {
        target_time = 9 * core.DURATION.HOUR, -- 9:00 AM
        direction = "_nearest",
    },
    sources = function()
        local base_time = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 10, -- 10:00 AM
            min = 30,
            sec = 0,
        })

        return {
            {
                {
                    timestamp = base_time.timestamp,
                    offset = base_time.offset,
                    value = 100.0,
                    label = "test",
                    note = "note",
                },
                {
                    timestamp = base_time.timestamp - (2 * core.DURATION.HOUR),
                    offset = base_time.offset,
                    value = 200.0,
                    label = "test2",
                    note = "note2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assert("result should have 2 points", #result == 2)

        -- 10:30 AM should snap to 9:00 AM same day (closer than next day 9:00 AM)
        local expected_9am = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 9,
            min = 0,
            sec = 0,
        }).timestamp

        test.assertEquals(expected_9am, result[1].timestamp)
        test.assertEquals(100.0, result[1].value)

        -- 8:30 AM should snap to 9:00 AM same day
        test.assertEquals(expected_9am, result[2].timestamp)
        test.assertEquals(200.0, result[2].value)
    end,
}

M.test_snap_to_9am_next = {
    config = {
        target_time = 9 * core.DURATION.HOUR, -- 9:00 AM
        direction = "_next",
    },
    sources = function()
        local base_time = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 10, -- 10:00 AM
            min = 30,
            sec = 0,
        })

        return {
            {
                {
                    timestamp = base_time.timestamp,
                    offset = base_time.offset,
                    value = 100.0,
                    label = "test",
                    note = "note",
                },
                {
                    timestamp = base_time.timestamp - (2 * core.DURATION.HOUR),
                    offset = base_time.offset,
                    value = 200.0,
                    label = "test2",
                    note = "note2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assert("result should have 1 point", #result == 2)

        -- 10:30 AM should snap to next day 9:00 AM
        local expected_next_9am = core.time({
            year = 2023,
            month = 6,
            day = 16, -- Next day
            hour = 9,
            min = 0,
            sec = 0
        }).timestamp

        test.assertEquals(expected_next_9am, result[1].timestamp)
        test.assertEquals(100.0, result[1].value)

        -- 10:30 AM should snap to next day 9:00 AM
        local expected_this_9am = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 9,
            min = 0,
            sec = 0
        }).timestamp

        test.assertEquals(expected_this_9am, result[2].timestamp)
        test.assertEquals(200.0, result[2].value)
    end,
}

M.test_snap_to_9am_previous = {
    config = {
        target_time = 9 * core.DURATION.HOUR, -- 9:00 AM
        direction = "_previous",
    },
    sources = function()
        local base_time = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 10, -- 10:00 AM
            min = 30,
            sec = 0,
        })

        return {
            {
                {
                    timestamp = base_time.timestamp,
                    offset = base_time.offset,
                    value = 100.0,
                    label = "test",
                    note = "note",
                },
                {
                    timestamp = base_time.timestamp - (2 * core.DURATION.HOUR),
                    offset = base_time.offset,
                    value = 200.0,
                    label = "test2",
                    note = "note2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assert("result should have 1 point", #result == 2)

        local expected_this_9am = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 9,
            min = 0,
            sec = 0
        }).timestamp

        test.assertEquals(expected_this_9am, result[1].timestamp)
        test.assertEquals(100.0, result[1].value)

        local expected_previous_9am = core.time({
            year = 2023,
            month = 6,
            day = 14, -- Previous day
            hour = 9,
            min = 0,
            sec = 0
        }).timestamp

        test.assertEquals(expected_previous_9am, result[2].timestamp)
        test.assertEquals(200.0, result[2].value)
    end,
}


local base_time = core.time({
    year = 2023,
    month = 6,
    day = 15,
    hour = 11,
    min = 45,
    sec = 0
})

M.test_data_preservation = {
    config = {
        target_time = 12 * core.DURATION.HOUR, -- 12:00 PM
        direction = "_nearest",
    },
    sources = function()
        return {
            {
                {
                    timestamp = base_time.timestamp,
                    offset = base_time.offset,
                    value = 42.5,
                    label = "preserved_label",
                    note = "preserved_note",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assert("result should have 1 point", #result == 1)

        -- Verify all fields are preserved except timestamp
        test.assertEquals(42.5, result[1].value)
        test.assertEquals("preserved_label", result[1].label)
        test.assertEquals("preserved_note", result[1].note)
        test.assertEquals(base_time.offset, result[1].offset)

        -- Timestamp should be snapped to noon
        local expected_noon = core.time({
            year = 2023,
            month = 6,
            day = 15,
            hour = 12,
            min = 0,
            sec = 0
        }).timestamp

        test.assertEquals(expected_noon, result[1].timestamp)
    end,
}

-- This test is commented because it fails. It fails because we don't have zone information in 
-- data points. We only have timestamp and offset. Without zone information, we cannot accurately
-- handle daylight savings time transitions. The implication is that the snap-time-to function 
-- will snap to the correct time for the zone that the machine it is running on is using. 
-- Since we don't yet have a way to specify the current zone in the test framework, we cannot
-- reliably test this behavior.
-- local dst_ref_time = core.time({
--     year = 2025,
--     month = 10,
--     day = 26,
--     hour = 8,
--     min = 30,
--     sec = 0,
--     zone = "Europe/London",
-- })
--
-- M["test daylight savings picks correct local time"] = {
--     config = {
--         target_time = 9 * core.DURATION.HOUR, -- 9:00 AM
--         direction = "_previous",
--     },
--     sources = function()
--         return {
--             {
--                 {
--                     timestamp = dst_ref_time.timestamp,
--                     offset = dst_ref_time.offset,
--                     value = 42.5,
--                 },
--             },
--         }
--     end,
--     assertions = function(result)
--         test.assert("result was nil", result)
--         test.assert("result should have 1 point", #result == 1)
--
--         -- Timestamp should be snapped to noon
--         local dst_ref_time_prev = core.time({
--             year = 2025,
--             month = 10,
--             day = 25,
--             hour = 9,
--             min = 0,
--             sec = 0,
--             zone = "Europe/London",
--         })
--
--         test.assertEquals(dst_ref_time_prev.offset, result[1].offset)
--         test.assertEquals(dst_ref_time_prev.timestamp, result[1].timestamp)
--     end,
-- }

return M
