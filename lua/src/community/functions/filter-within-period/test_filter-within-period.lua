local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_filter_after_period_days = {
    config = {
        period = "_day",
        period_multiplier = 7,  -- 7 days ago
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 3),  -- 3 days ago - should pass
                    value = 10.0,
                    label = "recent1",
                },
                {
                    timestamp = now - (DDAY * 5),  -- 5 days ago - should pass
                    value = 20.0,
                    label = "recent2",
                },
                {
                    timestamp = now - (DDAY * 10), -- 10 days ago - should not pass
                    value = 30.0,
                    label = "old1",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        -- Only 2 data points should pass (within 7 days)
        test.assertEquals(2, #result)

        test.assertEquals(10.0, result[1].value)
        test.assertEquals("recent1", result[1].label)

        test.assertEquals(20.0, result[2].value)
        test.assertEquals("recent2", result[2].label)
    end,
}

M.test_filter_after_period_preserves_fields = {
    config = {
        period = "_day",
        period_multiplier = 7,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 3),
                    value = 42.5,
                    label = "test_label",
                    note = "test_note",
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
