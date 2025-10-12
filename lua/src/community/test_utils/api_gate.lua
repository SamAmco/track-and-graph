-- lua/test_utils/api_gate.lua
-- Strict API level gate for flat-key apispecs (e.g., "COLOR.RED" => 1).
-- Usage (in the module load interceptor):
--   local gated = require("test_utils.api_gate").gate(real_module, spec)
--   return gated
local M = {}

local function current_level(path)
  local lvl = rawget(_G, "CURRENT_TEST_API_LEVEL")
  assert(
    type(lvl) == "number",
    "Tried to access " .. path .. " CURRENT_TEST_API_LEVEL must be set to a number before loading APIs"
  )
  return lvl
end

-- Throw if path is unknown or too new.
local function assert_allowed(spec, path, kind)
  local since = spec[path]
  if not since then
    error(("API '%s' (%s) is not declared in apispec"):format(path, kind or "symbol"), 3)
  end
  local cur = current_level(path)
  if cur < since then
    error(("API '%s' requires API level %d (script declares major version %d)"):format(path, since, cur), 3)
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
          -- Require the table itself to be present/allowed
          assert_allowed(spec, path, "table")
          -- Return a gated proxy for nested lookups
          local prox = cache[k]
          if not prox then
            prox = gate_table(v, spec, path)
            cache[k] = prox
          end
          return prox
        elseif type(v) == "function" then
          -- Require the function symbol itself
          assert_allowed(spec, path, "function")
          return v
        else
          -- constant/enum leaf; require exact path in spec (e.g. "COLOR.RED")
          assert_allowed(spec, path, "value")
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

return M
