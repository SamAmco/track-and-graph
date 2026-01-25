local M = {}

local core = require("tng.core")
local test = require("test.core")

local now = core.time().timestamp

M.test_addition = {
  config = {
    threshold = core.DURATION.SECOND,
    operation = "_addition",
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        {
          timestamp = now + 1000,
          value = 20.0,
        },
        {
          timestamp = now,
          value = 10.0,
        },
      },
      {
        {
          timestamp = now + 1000,
          value = 15.0,
        },
        {
          timestamp = now,
          value = 5.0,
        },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(35.0, result[1].value)
    test.assertEquals(15.0, result[2].value)
  end,
}

M.test_subtraction = {
  config = {
    operation = "_subtraction",
    threshold = core.DURATION.SECOND,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
      },
      {
        { timestamp = now + 1000, value = 15.0 },
        { timestamp = now,        value = 5.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(5.0, result[1].value)
    test.assertEquals(5.0, result[2].value)
  end,
}

M.test_multiplication = {
  config = {
    operation = "_multiplication",
    threshold = core.DURATION.SECOND,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
      },
      {
        { timestamp = now + 1000, value = 15.0 },
        { timestamp = now,        value = 5.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(300.0, result[1].value)
    test.assertEquals(50.0, result[2].value)
  end,
}

M.test_division = {
  config = {
    operation = "_division",
    threshold = core.DURATION.SECOND,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
      },
      {
        { timestamp = now + 1000, value = 15.0 },
        { timestamp = now,        value = 5.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(1.3333333333333, result[1].value)
    test.assertEquals(2.0, result[2].value)
  end,
}

M["test division with 0 and skip stratergy"] = {
  config = {
    operation = "_division",
    threshold = core.DURATION.SECOND,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
        { timestamp = now - 1000, value = 9.0 },
      },
      {
        { timestamp = now + 1000, value = 4.0 },
        { timestamp = now,        value = 0.0 },
        { timestamp = now - 1000, value = 3.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(5, result[1].value)
    test.assertEquals(3, result[2].value)
  end,
}

M["test division with 0 and pass through stratergy"] = {
  config = {
    operation = "_division",
    threshold = core.DURATION.SECOND,
    on_missing = "_pass_through",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
        { timestamp = now - 1000, value = 9.0 },
      },
      {
        { timestamp = now + 1000, value = 4.0 },
        { timestamp = now,        value = 0.0 },
        { timestamp = now - 1000, value = 3.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(3, #result)
    test.assertEquals(5, result[1].value)
    test.assertEquals(10, result[2].value)
    test.assertEquals(3, result[3].value)
  end,
}

M["test multiplication with missing data point and pass through stratergy"] = {
  config = {
    operation = "_multiplication",
    threshold = core.DURATION.SECOND / 2,
    on_missing = "_pass_through",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
        { timestamp = now - 1000, value = 9.0 },
      },
      {
        { timestamp = now + 1000, value = 4.0 },
        { timestamp = now - 1000, value = 3.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(3, #result)
    test.assertEquals(80, result[1].value)
    test.assertEquals(10, result[2].value)
    test.assertEquals(27, result[3].value)
  end,
}

M["test multiplication with missing data point and skip stratergy"] = {
  config = {
    operation = "_multiplication",
    threshold = core.DURATION.SECOND / 2,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now + 1000, value = 20.0 },
        { timestamp = now,        value = 10.0 },
        { timestamp = now - 1000, value = 9.0 },
      },
      {
        { timestamp = now + 1000, value = 4.0 },
        { timestamp = now - 1000, value = 3.0 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(80, result[1].value)
    test.assertEquals(27, result[2].value)
  end,
}

M["test data points outside threshold and skip stratergy"] = {
  config = {
    operation = "_addition",
    threshold = core.DURATION.SECOND / 10,
    on_missing = "_skip",
  },
  sources = function()
    return {
      {
        { timestamp = now,        value = 1 },
        { timestamp = now - 1000, value = 1 },
        { timestamp = now - 2000, value = 1 },
        { timestamp = now - 3000, value = 1 },
      },
      {
        { timestamp = now - 200,        value = 1 },
        { timestamp = now - 1100, value = 2 },
        { timestamp = now - 1800, value = 3 },
        { timestamp = now - 3000, value = 4 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(2, #result)
    test.assertEquals(3, result[1].value)
    test.assertEquals(5, result[2].value)
  end,
}

M["test data points outside threshold and pass through stratergy"] = {
  config = {
    operation = "_addition",
    threshold = core.DURATION.SECOND / 10,
    on_missing = "_pass_through",
  },
  sources = function()
    return {
      {
        { timestamp = now,        value = 1 },
        { timestamp = now - 1000, value = 1 },
        { timestamp = now - 2000, value = 1 },
        { timestamp = now - 3000, value = 1 },
      },
      {
        { timestamp = now - 200,        value = 1 },
        { timestamp = now - 1100, value = 2 },
        { timestamp = now - 1800, value = 3 },
        { timestamp = now - 3000, value = 4 },
      },
    }
  end,
  assertions = function(result)
    test.assert("Result was nil", result)
    test.assertEquals(4, #result)
    test.assertEquals(1, result[1].value)
    test.assertEquals(3, result[2].value)
    test.assertEquals(1, result[3].value)
    test.assertEquals(5, result[4].value)
  end,
}

return M
