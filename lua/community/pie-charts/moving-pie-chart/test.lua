local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_moving_pie_chart_basic = {
  config = {},
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 4.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 3.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

    local expected_segments = {
      { value = 5.0, label = "A" },
      { value = 4.0, label = "B" },
      { value = 3.0, label = "C" },
    }

    test.assertEquals(#expected_segments, #result.segments)
    for i, segment in ipairs(expected_segments) do
      test.assertEquals(segment.value, result.segments[i].value)
      test.assertEquals(segment.label, result.segments[i].label)
    end
  end,
}

M.test_moving_pie_chart_with_period = {
  config = {
    period = "core.PERIOD.WEEK",
    period_multiplier = "1",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 4.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 10), -- This should be filtered out due to period
          value = 3.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

    local expected_segments = {
      { value = 5.0, label = "A" },
      { value = 4.0, label = "B" },
    }

    test.assertEquals(#expected_segments, #result.segments)
    for i, segment in ipairs(expected_segments) do
      test.assertEquals(segment.value, result.segments[i].value)
      test.assertEquals(segment.label, result.segments[i].label)
    end
  end,
}

M.test_moving_pie_chart_with_label_colors = {
  config = {
    label_colors = '{A="#FF0000", B="#00FF00", C="#0000FF"}',
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 4.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 3.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

    local expected_segments = {
      { value = 5.0, label = "A", color = "#FF0000" },
      { value = 4.0, label = "B", color = "#00FF00" },
      { value = 3.0, label = "C", color = "#0000FF" },
    }

    test.assertEquals(#expected_segments, #result.segments)
    for i, segment in ipairs(expected_segments) do
      test.assertEquals(segment.value, result.segments[i].value)
      test.assertEquals(segment.label, result.segments[i].label)
      test.assertEquals(segment.color, result.segments[i].color)
    end
  end,
}

M.test_moving_pie_chart_count_by_label = {
  config = {
    count_by_label = "true",
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 5.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 4.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 3.0,
          label = "A",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.PIE_CHART, result.type)

    local expected_segments = {
      { value = 2, label = "A" },
      { value = 1, label = "B" },
    }

    test.assertEquals(#expected_segments, #result.segments)
    for i, segment in ipairs(expected_segments) do
      test.assertEquals(segment.value, result.segments[i].value)
      test.assertEquals(segment.label, result.segments[i].label)
    end
  end,
}

return M

