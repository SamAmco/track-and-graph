local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_filter_after_cutoff_basic = {
    config = {
        cutoff = core.time().timestamp - (5 * DDAY),
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
                    label = "recent1",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 20.0,
                    label = "recent2",
                },
                {
                    timestamp = now - (DDAY * 7),
                    value = 30.0,
                    label = "old1",
                },
                {
                    timestamp = now - (DDAY * 10),
                    value = 40.0,
                    label = "old2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Only 2 data points should pass (within 5 days)
        test.assertEquals(2, #result)

        test.assertEquals(10.0, result[1].value)
        test.assertEquals("recent1", result[1].label)

        test.assertEquals(20.0, result[2].value)
        test.assertEquals("recent2", result[2].label)
    end,
}

M.test_filter_after_cutoff_exact_boundary = {
    config = {
        cutoff = 1000000000000,  -- Fixed timestamp
    },
    sources = function()
        return {
            {
                {
                    timestamp = 1000000000000,  -- Exactly at cutoff
                    value = 1.0,
                    label = "at_cutoff",
                },
                {
                    timestamp = 1000000000001,  -- After cutoff
                    value = 2.0,
                    label = "after",
                },
                {
                    timestamp = 999999999999,  -- Before cutoff
                    value = 3.0,
                    label = "before",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- 2 data points: at and after cutoff
        test.assertEquals(2, #result)

        test.assertEquals(1.0, result[1].value)
        test.assertEquals("at_cutoff", result[1].label)

        test.assertEquals(2.0, result[2].value)
        test.assertEquals("after", result[2].label)
    end,
}

M.test_filter_after_cutoff_all_pass = {
    config = {
        cutoff = core.time().timestamp - (365 * DDAY),  -- 1 year ago
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
                    label = "test1",
                },
                {
                    timestamp = now - (DDAY * 2),
                    value = 20.0,
                    label = "test2",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 30.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- All 3 data points should pass
        test.assertEquals(3, #result)

        test.assertEquals(10.0, result[1].value)
        test.assertEquals(20.0, result[2].value)
        test.assertEquals(30.0, result[3].value)
    end,
}

M.test_filter_after_cutoff_all_filtered = {
    config = {
        cutoff = core.time().timestamp + (DDAY * 1),  -- Tomorrow
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
                    label = "test1",
                },
                {
                    timestamp = now - (DDAY * 2),
                    value = 20.0,
                    label = "test2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- No data points should pass (all before tomorrow)
        test.assertEquals(0, #result)
    end,
}

M.test_filter_after_cutoff_preserves_fields = {
    config = {
        cutoff = core.time().timestamp - (DDAY * 5),
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
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
