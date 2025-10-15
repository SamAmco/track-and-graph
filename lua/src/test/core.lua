local M = {}

M.assertEquals = function(a, b)
  if not (tostring(a) == tostring(b)) then
    error("Assertion failed, expected: " .. tostring(a) .. " got: " .. tostring(b))
  end
end

M.assert = function(message, cond)
  if not cond then
    error("Assertion failed: " .. message)
  end
end

M.assertClose = function(expected, actual, tolerance)
  tolerance = tolerance or 0.01
  local diff = math.abs(expected - actual)
  if diff > tolerance then
    error("Assertion failed, expected: " .. tostring(expected) .. " got: " .. tostring(actual) .. " (diff: " .. tostring(diff) .. ", tolerance: " .. tostring(tolerance) .. ")")
  end
end

return M
