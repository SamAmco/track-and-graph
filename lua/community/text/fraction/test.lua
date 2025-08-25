local M = {}

local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_basic_fraction = {
	config = {
		numerator_labels = "{'A', 'B'}",
	},
	sources = function()
		local now = core.time().timestamp
		return {
			source1 = {
				{
					timestamp = now - (DDAY * 1),
					value = 10.0,
					label = "A",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 5.0,
					label = "B",
				},
				{
					timestamp = now - (DDAY * 3),
					value = 15.0,
					label = "C",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
		test.assertEquals("15/30", result.text)
	end,
}

M.test_fraction_with_period = {
	config = {
		numerator_labels = "{'A'}",
		period = "core.PERIOD.WEEK",
	},
	sources = function()
		local now = core.time().timestamp
		local cutoff = core.shift(now, core.PERIOD.WEEK, -1).timestamp

		return {
			source1 = {
				{
					timestamp = now - (DDAY * 1),
					value = 10.0,
					label = "A",
				},
				{
					timestamp = now - (DDAY * 2),
					value = 5.0,
					label = "B",
				},
				{
					timestamp = cutoff - (DDAY * 1), -- Outside the period
					value = 15.0,
					label = "A",
				},
			},
		}
	end,
	assertions = function(result)
		test.assert("result was nil", result)
		test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
		test.assertEquals("10/15", result.text)
	end,
}

M.test_fraction_with_ignored_labels = {
  config = {
    numerator_labels = "{'A'}",
    ignored_labels = "{'C'}"
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 5.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 15.0,
          label = "C", -- Should be ignored
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
    test.assertEquals("10/15", result.text)
  end,
}

M.test_fraction_with_count_by_label = {
  config = {
    numerator_labels = "{'A'}",
    count_by_label = "true"
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 15.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 4),
          value = 5.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
    test.assertEquals("2/4", result.text)
  end,
}

M.test_fraction_with_fixed_denominator = {
  config = {
    numerator_labels = "{'A', 'B'}",
    fixed_denominator = "100"
  },
  sources = function()
    local now = core.time().timestamp
    return {
      source1 = {
        {
          timestamp = now - (DDAY * 1),
          value = 10.0,
          label = "A",
        },
        {
          timestamp = now - (DDAY * 2),
          value = 20.0,
          label = "B",
        },
        {
          timestamp = now - (DDAY * 3),
          value = 20.0,
          label = "C",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was nil", result)
    test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
    test.assertEquals("60/100", result.text)
  end,
}

M.test_fraction_with_zero_denominator_returns_nil = {
  config = {
    numerator_labels = "{'A'}",
  },
  sources = function()
    return {
      source1 = {
        {
          timestamp = core.time().timestamp,
          value = 0.0,
          label = "A",
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("result was not nil", not result)
  end,
}

return M
