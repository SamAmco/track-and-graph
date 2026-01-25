local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_cumulative_value_basic = {
    config = {
        enable_reset = false,
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
                    value = 5.0,
                    label = "test2",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 3.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- Values should be cumulative (processed chronologically, returned reverse)
        test.assertEquals(18.0, result[1].value)  -- 3 + 5 + 10
        test.assertEquals(8.0, result[2].value)   -- 3 + 5
        test.assertEquals(3.0, result[3].value)   -- 3

        -- Labels should be preserved
        test.assertEquals("test1", result[1].label)
        test.assertEquals("test2", result[2].label)
        test.assertEquals("test3", result[3].label)
    end,
}

M.test_cumulative_value_with_reset = {
    config = {
        enable_reset = true,
        reset_label = "reset",
        exact_match = true,
        case_sensitive = true,
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
                    value = 5.0,
                    label = "reset",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 3.0,
                    label = "test2",
                },
                {
                    timestamp = now - (DDAY * 4),
                    value = 2.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(4, #result)

        -- After reset, accumulation restarts
        test.assertEquals(15.0, result[1].value)  -- reset at 5, then 5 + 10
        test.assertEquals(5.0, result[2].value)   -- reset point
        test.assertEquals(5.0, result[3].value)   -- 2 + 3
        test.assertEquals(2.0, result[4].value)   -- 2
    end,
}

M.test_cumulative_value_substring_match = {
    config = {
        enable_reset = true,
        reset_label = "reset",
        exact_match = false,
        case_sensitive = true,
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
                    value = 5.0,
                    label = "reset_counter",  -- Contains "reset"
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 3.0,
                    label = "test2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- Substring match should trigger reset
        test.assertEquals(15.0, result[1].value)  -- reset, then 5 + 10
        test.assertEquals(5.0, result[2].value)   -- reset point
        test.assertEquals(3.0, result[3].value)   -- 3
    end,
}

M.test_cumulative_value_case_insensitive = {
    config = {
        enable_reset = true,
        reset_label = "RESET",
        exact_match = true,
        case_sensitive = false,
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
                    value = 5.0,
                    label = "reset",  -- Lowercase
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 3.0,
                    label = "test2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- Case insensitive should match
        test.assertEquals(15.0, result[1].value)
        test.assertEquals(5.0, result[2].value)
        test.assertEquals(3.0, result[3].value)
    end,
}

M.test_cumulative_value_negative_values = {
    config = {
        enable_reset = false,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = -5.0,
                    label = "test1",
                },
                {
                    timestamp = now - (DDAY * 2),
                    value = 10.0,
                    label = "test2",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = -3.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- Should handle negative values correctly
        test.assertEquals(2.0, result[1].value)   -- -3 + 10 + -5
        test.assertEquals(7.0, result[2].value)   -- -3 + 10
        test.assertEquals(-3.0, result[3].value)  -- -3
    end,
}

M.test_cumulative_value_empty_reset_label = {
    config = {
        enable_reset = true,
        reset_label = "",  -- Empty label
        exact_match = true,
        case_sensitive = true,
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
                    value = 5.0,
                    label = "",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 3.0,
                    label = "test2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- Empty reset label should still trigger resets
        test.assertEquals(15.0, result[1].value)
        test.assertEquals(5.0, result[2].value)
        test.assertEquals(3.0, result[3].value)
    end,
}

M.test_cumulative_value_preserves_fields = {
    config = {
        enable_reset = false,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
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
        test.assertEquals(10.0, result[1].value)
        test.assertEquals("test_label", result[1].label)
        test.assertEquals("test_note", result[1].note)
    end,
}

return M
