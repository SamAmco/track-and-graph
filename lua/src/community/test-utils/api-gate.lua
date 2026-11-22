-- Test-only API lookup tracking. Each session owns its usage; no VM-global version/state.
local M = {}

function M.new()
    local usage = {}
    local tracking = true
    local session = {}

    function session.set_tracking(enabled)
        local previous = tracking
        tracking = enabled
        return previous
    end

    function session.reset()
        usage = {}
    end

    function session.gate(real, spec, namespace)
        assert(type(real) == "table", "gate: real module must be a table")
        assert(type(spec) == "table", "gate: spec must be a table")
        local function wrap(value, prefix)
            local cache = {}
            return setmetatable({}, {
                __index = function(_, key)
                    local path = prefix and (prefix .. "." .. tostring(key)) or tostring(key)
                    local member = value[key]
                    assert(member ~= nil, "API '" .. path .. "' does not exist on module")
                    local qualified = namespace and (namespace .. "." .. path) or path
                    if tracking then
                        local since = spec[path]
                        assert(since, "API '" .. qualified .. "' is not declared in apispec")
                        usage[qualified] = math.max(usage[qualified] or 0, since)
                    end
                    if type(member) == "table" then
                        if not cache[key] then cache[key] = wrap(member, path) end
                        return cache[key]
                    end
                    -- Track lookups, not calls: return the original function unchanged.
                    return member
                end,
                __newindex = function() error("Attempt to modify gated API table", 2) end,
            })
        end
        return wrap(real)
    end

    function session.validate(max_level)
        assert(type(max_level) == "number", "validate: max_level must be a number")
        local violations = {}
        for path, required in pairs(usage) do
            if required > max_level then
                violations[#violations + 1] = string.format(
                    "  • '%s' requires API level %d (script declares major version %d)",
                    path, required, max_level)
            end
        end
        table.sort(violations)
        if #violations > 0 then
            error("API level violations detected (" .. #violations .. "):\n" ..
                table.concat(violations, "\n"), 2)
        end
    end

    return session
end

-- Existing plain-Lua API tests use one default session. JVM runners create their own.
local default = M.new()
M.gate = default.gate
M.reset = default.reset
M.validate = default.validate
return M
