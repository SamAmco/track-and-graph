local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_merge_bars_from_multiple_sources = {
  config = {
    totalling_period = "core.PERIOD.DAY",
    totalling_period_multiplier = "2",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 2),
          value = 5.0,
          label = "d",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 4.0,
          label = "a",
        },
        {
          timestamp = now - (DDAY * 6),
          value = 3.0,
          label = "c",
        },
        {
          timestamp = now - (DDAY * 9),
          value = 2.0,
          label = "b",
        },
        {
          timestamp = now - (DDAY * 10),
          value = 1.0,
          label = "a",
        },
      },
      source2 = {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "d",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 4.0,
          label = "a",
        },
        {
          timestamp = now - (DDAY * 5),
          value = 3.0,
          label = "c",
        },
        {
          timestamp = now - (DDAY * 7),
          value = 2.0,
          label = "b",
        },
        {
          timestamp = now - (DDAY * 8),
          value = 1.0,
          label = "f",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TIME_BARCHART, result.type)

    local expected_bars = {
      {
        { value = 10.0, label = "d" },
      },
      {
        { value = 8.0, label = "a" },
      },
      {
        { value = 6.0, label = "c" },
      },
      {
        { value = 2.0, label = "b" },
        { value = 1.0, label = "f" },
      },
      {
        { value = 1.0, label = "a" },
        { value = 2.0, label = "b" },
      },
    }

    -- Verify the structure of bars and segments
    test.assertEquals(#expected_bars, #result.bars)

    for i, bar in ipairs(expected_bars) do
      test.assertEquals(#bar, #result.bars[i])

      for j, segment in ipairs(bar) do
        test.assertEquals(segment.value, result.bars[i][j].value)
        test.assertEquals(segment.label, result.bars[i][j].label)
      end
    end
  end,
}

return M
