local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_cumulative_bar_chart = {
  config = {
    totalling_period = "core.PERIOD.DAY",
    from_now = "false",
  },
  sources = function()
    local now_data = core.time()
    local now = now_data.timestamp
    local offset = now_data.offset
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          offset = offset,
          value = 5.0,
        },
        {
          timestamp = now - (DDAY * 2),
          offset = offset,
          value = 4.0,
        },
        {
          timestamp = now - (DDAY * 3),
          offset = offset,
          value = 3.0,
        },
        {
          timestamp = now - (DDAY * 4),
          offset = offset,
          value = 2.0,
        },
        {
          timestamp = now - (DDAY * 5),
          offset = offset,
          value = 1.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TIME_BARCHART, result.type)

    local expected_values = { 15.0, 10.0, 6.0, 3.0, 1.0 }
    for i, value in ipairs(expected_values) do
      test.assertEquals(value, result.bars[i][1].value)
    end
  end,
}

M.test_cumulative_bar_chart_with_different_labels = {
  config = {
    totalling_period = "core.PERIOD.DAY",
    from_now = "false",
  },
  sources = function()
    local now_data = core.time()
    local now = now_data.timestamp
    local offset = now_data.offset
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 0),
          offset = offset,
          value = 11.0,
          label = "l2",
        },
        {
          timestamp = now - (DDAY * 1),
          offset = offset,
          value = 10.0,
          label = "l1",
        },
        {
          timestamp = now - (DDAY * 1),
          offset = offset,
          value = 8.0,
          label = "l2",
        },
        {
          timestamp = now - (DDAY * 2),
          offset = offset,
          value = 5.0,
          label = "l1",
        },
        {
          timestamp = now - (DDAY * 2),
          offset = offset,
          value = 3.0,
          label = "l1",
        },
        {
          timestamp = now - (DDAY * 2),
          offset = offset,
          value = 1.0,
          label = "l2",
        },
        {
          timestamp = now - (DDAY * 3),
          offset = offset,
          value = 2.0,
          label = "l2",
        },
        {
          timestamp = now - (DDAY * 3),
          offset = offset,
          value = 1.0,
          label = "l2",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TIME_BARCHART, result.type)

    local expected_bars = {
      {
        segments = {
          { value = 23.0, label = "l2" },
          { value = 18.0, label = "l1" },
        },
      },
      {
        segments = {
          { value = 12.0, label = "l2" },
          { value = 18.0, label = "l1" },
        },
      },
      {
        segments = {
          { value = 4.0, label = "l2" },
          { value = 8.0, label = "l1" },
        },
      },
      {
        segments = {
          { value = 3.0, label = "l2" },
          { value = 0.0, label = "l1" },
        },
      },
    }

    -- Verify the structure of bars and segments
    test.assertEquals(#expected_bars, #result.bars)

    for i, bar in ipairs(expected_bars) do
      test.assertEquals(#bar.segments, #result.bars[i])

      for j, segment in ipairs(bar.segments) do
        test.assertEquals(segment.value, result.bars[i][j].value)
        test.assertEquals(segment.label, result.bars[i][j].label)
      end
    end
  end,
}

M.test_using_from_now = {
  config = {
    period = "core.PERIOD.DAY",
    totalling_period = "core.PERIOD.DAY",
    from_now = "true",
  },
  sources = function()
    local now_data = core.time()
    local now = now_data.timestamp
    local offset = now_data.offset
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 0.5),
          offset = offset,
          value = 5.0,
        },
        {
          timestamp = now - (DDAY * 1.3),
          offset = offset,
          value = 4.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TIME_BARCHART, result.type)

    local expected_values = { 5.0 }
    for i, value in ipairs(expected_values) do
      test.assertEquals(value, result.bars[i][1].value)
    end
  end,
}

return M
