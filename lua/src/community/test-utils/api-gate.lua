-- lua/test_utils/api-gate.lua
-- API level gate that tracks usage and validates at test end
-- Usage (in the module load interceptor):
--   local gated = require("test_utils.api-gate").gate(real_module, spec)
--   return gated
local M = {}

-- Initialize or get the global usage tracking table
local function get_usage_table()
  if not rawget(_G, "TNG_API_USAGE") then
    rawset(_G, "TNG_API_USAGE", {})
  end
  return rawget(_G, "TNG_API_USAGE")
end

-- Track that a specific API path was used
local function track_usage(spec, path, kind)
  local since = spec[path]
  if not since then
    error(("API '%s' (%s) is not declared in apispec"):format(path, kind or "symbol"), 3)
  end

  local usage = get_usage_table()
  if not usage[path] or usage[path] < since then
    usage[path] = since
  end
end

local function gate_table(real, spec, base_path)
  -- cache proxies so repeated field access returns stable tables
  local cache = {}
  return setmetatable({}, {
    __index = function(_, k)
      local success, result = pcall(function()
        local v = real[k]
        local key = tostring(k)
        local path = base_path and (base_path .. "." .. key) or key

        if v == nil then
          -- Symbol doesn't exist on the real module
          error(("API '%s' does not exist on module"):format(path), 2)
        end

        if type(v) == "table" then
          -- Track table access
          track_usage(spec, path, "table")
          -- Return a gated proxy for nested lookups
          local prox = cache[k]
          if not prox then
            prox = gate_table(v, spec, path)
            cache[k] = prox
          end
          return prox
        elseif type(v) == "function" then
          -- Track function access
          track_usage(spec, path, "function")
          return v
        else
          -- constant/enum leaf; track exact path in spec (e.g. "COLOR.RED")
          track_usage(spec, path, "value")
          return v
        end
      end)

      if not success then
        local path = base_path and (base_path .. "." .. tostring(k)) or tostring(k)
        print("Error in api_gate at path:", path)
        print("Original error:", result)
        error(result, 2)
      end

      return result
    end,

    __newindex = function()
      error("Attempt to modify gated API table", 2)
    end,

    -- Make tostring helpful for debugging
    __tostring = function()
      return ("<Gated:%s>"):format(base_path or "<root>")
    end,
  })
end

-- Public: gate(real_module_table, apispec_table)
function M.gate(real, spec)
  assert(type(real) == "table", "gate: real module must be a table")
  assert(type(spec) == "table", "gate: spec must be a table")
  return gate_table(real, spec, nil)
end

-- Public: Reset the usage tracking (call at beginning of each test)
function M.reset()
  rawset(_G, "TNG_API_USAGE", nil)
end

-- Public: Validate that all used APIs are at or below the given level
function M.validate(max_level)
  assert(type(max_level) == "number", "validate: max_level must be a number")

  local usage = rawget(_G, "TNG_API_USAGE")
  if not usage then
    error("validate: usage table not initialized; call reset() before tests", 2)
  end

  local violations = {}
  for path, required_level in pairs(usage) do
    if required_level > max_level then
      table.insert(violations, {
        path = path,
        required = required_level,
        declared = max_level
      })
    end
  end

  if #violations > 0 then
    table.sort(violations, function(a, b) return a.path < b.path end)
    local messages = {}
    for _, v in ipairs(violations) do
      table.insert(messages,
        string.format("  • '%s' requires API level %d (script declares major version %d)",
          v.path, v.required, v.declared))
    end
    error(string.format(
      "API level violations detected (%d):\n%s",
      #violations,
      table.concat(messages, "\n")
    ))
  end
end

return M
