local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

M.test_no_last_value_below_threshold = {
  config = {
    threshold = 1.5,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - 1000,
          value = 2.0,
        },
        {
          timestamp = now - 2000,
          value = 3.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assertEquals(nil, result)
  end,
}

M.test_last_value_below_threshold = {
  config = {
    threshold = 4,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - 1,
          value = 6.0,
        },
        {
          timestamp = now - 2,
          value = 5.0,
        },
        {
          timestamp = now - 3,
          value = 4.0,
        },
        {
          timestamp = now - 4,
          value = 3.0,
        },
        {
          timestamp = now - 5,
          value = 2.0,
        },
        {
          timestamp = now - 6,
          value = 1.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.DATA_POINT, result.type)
    test.assertEquals(3.0, result.datapoint.value)
  end,
}

M.test_last_value_below_threshold_two_sources = {
  config = {
    threshold = 2,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - 1,
          value = 3.0,
        },
        {
          timestamp = now - 3,
          value = 2.5,
        },
        {
          timestamp = now - 5,
          value = 1.0,
        },
      },
      source2 = {
        {
          timestamp = now - 2,
          value = 3.0,
        },
        {
          timestamp = now - 4,
          value = 1.5,
        },
        {
          timestamp = now - 6,
          value = 0.5,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.DATA_POINT, result.type)
    test.assertEquals(1.5, result.datapoint.value)
  end,
}

M.test_last_value_below_threshold_two_sources2 = {
  config = {
    threshold = 3,
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - 1,
          value = 4.0,
        },
        {
          timestamp = now - 3,
          value = 3.5,
        },
        {
          timestamp = now - 5,
          value = 2.0,
        },
      },
      source2 = {
        {
          timestamp = now - 2,
          value = 4.0,
        },
        {
          timestamp = now - 4,
          value = 2.5,
        },
        {
          timestamp = now - 6,
          value = 1.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.DATA_POINT, result.type)
    test.assertEquals(2.5, result.datapoint.value)
  end,
}

return M
