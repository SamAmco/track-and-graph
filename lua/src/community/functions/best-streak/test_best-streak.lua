local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_best_streak_basic_running_best = {
    config = {
        reset_threshold = 0.0,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                { timestamp = now - (DDAY * 1), value = 1.0, label = "newest" },
                { timestamp = now - (DDAY * 2), value = 0.0, label = "reset_after_best" },
                { timestamp = now - (DDAY * 3), value = 1.0, label = "third_success" },
                { timestamp = now - (DDAY * 4), value = 1.0, label = "second_success" },
                { timestamp = now - (DDAY * 5), value = 1.0, label = "first_success" },
                { timestamp = now - (DDAY * 6), value = 0.0, label = "reset" },
                { timestamp = now - (DDAY * 7), value = 1.0, label = "early_second" },
                { timestamp = now - (DDAY * 8), value = 1.0, label = "early_first" },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(8, #result)

        test.assertEquals(3, result[1].value)
        test.assertEquals(3, result[2].value)
        test.assertEquals(3, result[3].value)
        test.assertEquals(2, result[4].value)
        test.assertEquals(2, result[5].value)
        test.assertEquals(2, result[6].value)
        test.assertEquals(2, result[7].value)
        test.assertEquals(1, result[8].value)
    end,
}

M.test_best_streak_default_threshold_resets_on_zero_and_negative = {
    config = {},
    sources = function()
        local now = core.time().timestamp
        return {
            {
                { timestamp = now - (DDAY * 1), value = 2.0 },
                { timestamp = now - (DDAY * 2), value = -1.0 },
                { timestamp = now - (DDAY * 3), value = 1.0 },
                { timestamp = now - (DDAY * 4), value = 1.0 },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(4, #result)

        test.assertEquals(2, result[1].value)
        test.assertEquals(2, result[2].value)
        test.assertEquals(2, result[3].value)
        test.assertEquals(1, result[4].value)
    end,
}

M.test_best_streak_custom_threshold = {
    config = {
        reset_threshold = 5.0,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                { timestamp = now - (DDAY * 1), value = 9.0 },
                { timestamp = now - (DDAY * 2), value = 6.0 },
                { timestamp = now - (DDAY * 3), value = 5.0 },
                { timestamp = now - (DDAY * 4), value = 8.0 },
                { timestamp = now - (DDAY * 5), value = 7.0 },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(5, #result)

        test.assertEquals(2, result[1].value)
        test.assertEquals(2, result[2].value)
        test.assertEquals(2, result[3].value)
        test.assertEquals(2, result[4].value)
        test.assertEquals(1, result[5].value)
    end,
}

M.test_best_streak_preserves_fields = {
    config = {
        reset_threshold = 0.0,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now,
                    value = 10.0,
                    label = "test label",
                    note = "test note",
                    offset = 3600,
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)

        test.assertEquals(1, result[1].value)
        test.assertEquals("test label", result[1].label)
        test.assertEquals("test note", result[1].note)
        test.assertEquals(3600, result[1].offset)
    end,
}

M.test_best_streak_empty_source = {
    config = {},
    sources = function()
        return {
            {},
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(0, #result)
    end,
}

return M
