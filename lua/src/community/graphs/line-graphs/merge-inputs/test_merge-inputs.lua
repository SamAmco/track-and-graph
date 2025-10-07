local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

M.test_merge_line_merges_datapoints_from_multiple_sources_in_time_order = {
  config = {
    from_now = "false",
  },
  sources = function()
    return {
      source1 = {
        {
          timestamp = 5,
          value = 1.0,
        },
        {
          timestamp = 4,
          value = 4.0,
        },
        {
          timestamp = 1,
          value = 5.0,
        },
      },
      source2 = {
        {
          timestamp = 6,
          value = 2.0,
        },
        {
          timestamp = 3,
          value = 3.0,
        },
        {
          timestamp = 2,
          value = 6.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.LINE_GRAPH, result.type)

    local expected_values = { 2.0, 1.0, 4.0, 3.0, 6.0, 5.0 }
    local line_points = result.lines[1].line_points

    test.assertEquals(#expected_values, #line_points)
    for i, value in ipairs(expected_values) do
      test.assertEquals(value, line_points[i].value)
    end
  end,
}

return M
