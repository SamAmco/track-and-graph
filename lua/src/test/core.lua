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

return M
